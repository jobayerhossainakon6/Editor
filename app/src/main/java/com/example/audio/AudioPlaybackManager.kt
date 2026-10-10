package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.engine.AudioMixerEngine
import com.example.model.AudioTrackItem
import java.io.File

/**
 * Studio Multi-Track Audio Engine for Real-Time Preview & Timeline Editing.
 * Provides instant, zero-latency playback, smooth playhead scrubbing without audio stutter,
 * real-time volume slider responsiveness, dynamic speech ducking, and seamless play/pause state transitions.
 */
class AudioPlaybackManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val activePlayers = mutableMapOf<String, MediaPlayer>()
    private val completedTracks = mutableSetOf<String>()
    private val mixerEngine = AudioMixerEngine(context)

    private var wasPlaying: Boolean = false
    private var hasRequestedFocus: Boolean = false

    private fun requestAudioFocus() {
        if (hasRequestedFocus) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        .build()
                }
                audioFocusRequest?.let {
                    audioManager?.requestAudioFocus(it)
                    hasRequestedFocus = true
                }
            } else {
                @Suppress("DEPRECATION")
                val res = audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    hasRequestedFocus = true
                }
            }
        } catch (_: Exception) {}
    }

    private fun abandonAudioFocus() {
        if (!hasRequestedFocus) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
            hasRequestedFocus = false
        } catch (_: Exception) {}
    }

    /**
     * Synchronizes multi-track audio playback with the global timeline playhead.
     */
    fun syncPlayback(
        audioTracks: List<AudioTrackItem>,
        isPlaying: Boolean,
        currentTimeSeconds: Double
    ) {
        if (!isPlaying || audioTracks.isEmpty()) {
            if (wasPlaying) {
                pauseAll()
            }
            return
        }

        requestAudioFocus()

        val isStartingPlayback = !wasPlaying
        wasPlaying = true

        // Clean up removed audio tracks
        val currentTrackIds = audioTracks.map { it.id }.toSet()
        val removedIds = activePlayers.keys.filter { it !in currentTrackIds }
        for (id in removedIds) {
            activePlayers.remove(id)?.let { player ->
                try { player.stop(); player.release() } catch (_: Exception) {}
            }
            completedTracks.remove(id)
        }

        // Determine if any voiceover track is actively sounding for automated ducking
        val isVoiceoverActive = audioTracks.any {
            (it.trackType == "voiceover" || it.trackType == "voice" || it.trackIndex == 1) &&
                    !it.isMuted && it.volume > 0f &&
                    currentTimeSeconds >= it.startSeconds && currentTimeSeconds < (it.startSeconds + it.durationSeconds)
        }

        for (track in audioTracks) {
            if (track.isMuted || track.volume <= 0f) {
                activePlayers[track.id]?.let { player ->
                    try {
                        if (player.isPlaying) player.pause()
                    } catch (_: Exception) {}
                }
                continue
            }

            val trackStart = track.startSeconds
            val trackEnd = track.startSeconds + track.durationSeconds
            val isInActiveSpan = (currentTimeSeconds >= trackStart && currentTimeSeconds < trackEnd)

            if (isInActiveSpan) {
                val offsetInTrackSec = (currentTimeSeconds - trackStart) + track.audioOffsetSeconds
                val targetMs = (offsetInTrackSec * 1000).toInt().coerceAtLeast(0)
                val gain = track.getEffectiveVolumeAt(currentTimeSeconds, isVoiceoverActive).coerceIn(0f, 1f)

                var player = activePlayers[track.id]

                if (player == null) {
                    player = createPlayerForTrack(track)
                    if (player != null) {
                        activePlayers[track.id] = player
                        try {
                            val duration = player.duration
                            val safeSeekMs = if (duration > 0 && !track.isLooping) {
                                targetMs.coerceAtMost(duration - 50)
                            } else if (duration > 0 && track.isLooping) {
                                targetMs % duration
                            } else {
                                targetMs
                            }
                            player.seekTo(safeSeekMs)
                            player.setVolume(gain, gain)
                            player.start()
                            completedTracks.remove(track.id)
                        } catch (e: Exception) {
                            Log.w("AudioPlaybackManager", "Error starting player for ${track.title}: ${e.message}")
                        }
                    }
                } else {
                    // Update live automated volume on every tick (fade in, fade out, ducking)
                    try {
                        player.setVolume(gain, gain)
                    } catch (_: Exception) {}

                    if (isStartingPlayback || (!player.isPlaying && !completedTracks.contains(track.id))) {
                        // Start or resume playback
                        try {
                            val duration = player.duration
                            val safeSeekMs = if (duration > 0 && !track.isLooping) {
                                targetMs.coerceAtMost(duration - 50)
                            } else if (duration > 0 && track.isLooping) {
                                targetMs % duration
                            } else {
                                targetMs
                            }
                            player.seekTo(safeSeekMs)
                            player.start()
                            completedTracks.remove(track.id)
                        } catch (e: Exception) {
                            Log.w("AudioPlaybackManager", "Error resuming player for ${track.title}: ${e.message}")
                        }
                    }
                    // NOTE: While player is playing continuously, DO NOT call seekTo repeatedly!
                    // This allows native Android AudioTrack buffers to stream with 0 stutter.
                }
            } else {
                // Outside this track's timeline span
                if (currentTimeSeconds < trackStart) {
                    completedTracks.remove(track.id)
                }
                activePlayers[track.id]?.let { player ->
                    try {
                        if (player.isPlaying) {
                            player.pause()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Overload supporting (audioTracks, currentTimeSeconds, isPlaying) ordering.
     */
    fun syncPlayback(
        audioTracks: List<AudioTrackItem>,
        currentTimeSeconds: Double,
        isPlaying: Boolean
    ) {
        syncPlayback(audioTracks, isPlaying, currentTimeSeconds)
    }

    /**
     * Responds immediately to manual playhead scrubbing / seeking while editing or previewing.
     */
    fun seekTo(audioTracks: List<AudioTrackItem>, currentTimeSeconds: Double) {
        val isVoiceoverActive = audioTracks.any {
            (it.trackType == "voiceover" || it.trackType == "voice" || it.trackIndex == 1) &&
                    !it.isMuted && it.volume > 0f &&
                    currentTimeSeconds >= it.startSeconds && currentTimeSeconds < (it.startSeconds + it.durationSeconds)
        }

        for (track in audioTracks) {
            val trackStart = track.startSeconds
            val trackEnd = track.startSeconds + track.durationSeconds
            val isInActiveSpan = (currentTimeSeconds >= trackStart && currentTimeSeconds < trackEnd)

            if (isInActiveSpan && !track.isMuted && track.volume > 0f) {
                val offsetInTrackSec = (currentTimeSeconds - trackStart) + track.audioOffsetSeconds
                val targetMs = (offsetInTrackSec * 1000).toInt().coerceAtLeast(0)
                val gain = track.getEffectiveVolumeAt(currentTimeSeconds, isVoiceoverActive).coerceIn(0f, 1f)

                var player = activePlayers[track.id]
                if (player == null) {
                    player = createPlayerForTrack(track)
                    if (player != null) {
                        activePlayers[track.id] = player
                    }
                }

                player?.let { p ->
                    try {
                        p.setVolume(gain, gain)
                        val duration = p.duration
                        val safeSeekMs = if (duration > 0 && !track.isLooping) {
                            targetMs.coerceAtMost(duration - 50)
                        } else if (duration > 0 && track.isLooping) {
                            targetMs % duration
                        } else {
                            targetMs
                        }
                        p.seekTo(safeSeekMs)
                        completedTracks.remove(track.id)
                    } catch (_: Exception) {}
                }
            } else {
                activePlayers[track.id]?.let { p ->
                    try {
                        if (p.isPlaying) p.pause()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Immediate live volume adjustment (e.g., when moving timeline volume slider).
     */
    fun updateVolume(trackId: String, volume: Float) {
        activePlayers[trackId]?.let { player ->
            try {
                val gain = volume.coerceIn(0f, 1f)
                player.setVolume(gain, gain)
            } catch (_: Exception) {}
        }
    }

    /**
     * Immediate live mute toggle.
     */
    fun updateMute(trackId: String, isMuted: Boolean) {
        activePlayers[trackId]?.let { player ->
            try {
                if (isMuted) {
                    player.setVolume(0f, 0f)
                }
            } catch (_: Exception) {}
        }
    }

    private fun createPlayerForTrack(track: AudioTrackItem): MediaPlayer? {
        return try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                isLooping = track.isLooping
                setOnCompletionListener {
                    if (!track.isLooping) {
                        completedTracks.add(track.id)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("AudioPlaybackManager", "MediaPlayer error on '${track.title}': what=$what extra=$extra")
                    activePlayers.remove(track.id)?.let {
                        try { it.reset(); it.release() } catch (_: Exception) {}
                    }
                    true
                }
            }

            var resolvedPath = track.audioPath
            val needsFallback = resolvedPath.isEmpty() ||
                    (!resolvedPath.startsWith("content://") && !File(resolvedPath).exists())

            if (needsFallback) {
                resolvedPath = mixerEngine.getSampleAudioPath(track.title, track.trackType)
            }

            when {
                resolvedPath.startsWith("content://") || resolvedPath.startsWith("file://") -> {
                    mp.setDataSource(context, Uri.parse(resolvedPath))
                }
                else -> {
                    var file = File(resolvedPath)
                    if (!file.exists() || file.length() < 100L) {
                        // Guarantee file exists
                        resolvedPath = mixerEngine.getSampleAudioPath(track.title, track.trackType)
                        file = File(resolvedPath)
                    }
                    if (file.exists() && file.length() > 0L) {
                        mp.setDataSource(file.absolutePath)
                    } else {
                        return null
                    }
                }
            }

            mp.prepare()
            val gain = track.volume.coerceIn(0f, 1f)
            mp.setVolume(gain, gain)
            mp
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Failed to create player for track '${track.title}': ${e.message}")
            null
        }
    }

    /**
     * Pauses all media players while keeping them initialized for instant resumption.
     */
    fun pauseAll() {
        wasPlaying = false
        for ((_, player) in activePlayers) {
            try {
                if (player.isPlaying) {
                    player.pause()
                }
            } catch (_: Exception) {}
        }
        abandonAudioFocus()
    }

    /**
     * Stops and releases all media players completely.
     */
    fun stopAll() {
        wasPlaying = false
        for ((_, player) in activePlayers) {
            try {
                player.stop()
                player.release()
            } catch (_: Exception) {}
        }
        activePlayers.clear()
        completedTracks.clear()
        abandonAudioFocus()
    }

    fun release() = stopAll()
}
