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
 * Studio Multi-Track Audio Engine for Real-Time Preview Playback and Timeline Scrubbing.
 * Handles instant, glitch-free preview playback, smooth playhead scrubbing without latency or audio stutter,
 * dynamic audio ducking, live volume adjustments, and clean multi-player lifecycle management.
 */
class AudioPlaybackManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val activePlayers = mutableMapOf<String, MediaPlayer>()
    private val playingTracks = mutableSetOf<String>()
    private val mixerEngine = AudioMixerEngine(context)

    private var hasRequestedFocus: Boolean = false

    init {
        // Pre-warm sample audio files on a background worker thread so preview is instant
        Thread {
            try {
                mixerEngine.ensureSampleAudioFilesExist()
            } catch (_: Exception) {}
        }.start()
    }

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
     * Starts players when entering clip boundaries, stops immediately when exiting,
     * and maintains uninterrupted audio streaming without re-seeking stutter.
     */
    fun syncPlayback(
        audioTracks: List<AudioTrackItem>,
        isPlaying: Boolean,
        currentTimeSeconds: Double
    ) {
        if (!isPlaying || audioTracks.isEmpty()) {
            stopAll()
            return
        }

        requestAudioFocus()

        // Clean up removed audio tracks
        val currentTrackIds = audioTracks.map { it.id }.toSet()
        val removedIds = activePlayers.keys.filter { it !in currentTrackIds }
        for (id in removedIds) {
            activePlayers.remove(id)?.let { player ->
                try { player.stop(); player.release() } catch (_: Exception) {}
            }
            playingTracks.remove(id)
        }

        // Determine if any voiceover track is actively sounding for speech ducking
        val isVoiceoverActive = audioTracks.any {
            (it.trackType == "voiceover" || it.trackType == "voice" || it.trackIndex == 1) &&
                    !it.isMuted && it.volume > 0f &&
                    currentTimeSeconds >= it.startSeconds && currentTimeSeconds < (it.startSeconds + it.durationSeconds)
        }

        for (track in audioTracks) {
            if (track.isMuted || track.volume <= 0f) {
                activePlayers.remove(track.id)?.let { player ->
                    try { player.stop(); player.release() } catch (_: Exception) {}
                }
                playingTracks.remove(track.id)
                continue
            }

            val trackStart = track.startSeconds
            val trackEnd = track.startSeconds + track.durationSeconds
            val isInActiveSpan = (currentTimeSeconds >= trackStart && currentTimeSeconds < trackEnd)

            if (isInActiveSpan) {
                val offsetInTrack = (currentTimeSeconds - trackStart) + track.audioOffsetSeconds
                val targetMs = (offsetInTrack * 1000).toInt().coerceAtLeast(0)
                val gain = track.getEffectiveVolumeAt(currentTimeSeconds, isVoiceoverActive).coerceIn(0f, 1f)

                var player = activePlayers[track.id]
                if (player == null) {
                    player = createPlayerForTrack(track)
                    if (player != null) {
                        try {
                            val duration = player.duration
                            val safeSeekMs = if (duration > 0 && !track.isLooping) {
                                targetMs.coerceAtMost((duration - 50).coerceAtLeast(0))
                            } else if (duration > 0 && track.isLooping) {
                                targetMs % duration
                            } else {
                                targetMs
                            }
                            player.seekTo(safeSeekMs)
                            player.setVolume(gain, gain)
                            player.start()
                            activePlayers[track.id] = player
                            playingTracks.add(track.id)
                        } catch (e: Exception) {
                            Log.w("AudioPlaybackManager", "Error starting player for ${track.title}: ${e.message}")
                            try { player.release() } catch (_: Exception) {}
                        }
                    }
                } else {
                    // Update live volume on each tick (fade, ducking, slider)
                    try {
                        player.setVolume(gain, gain)
                    } catch (_: Exception) {}

                    if (track.id !in playingTracks) {
                        try {
                            val duration = player.duration
                            val safeSeekMs = if (duration > 0 && !track.isLooping) {
                                targetMs.coerceAtMost((duration - 50).coerceAtLeast(0))
                            } else if (duration > 0 && track.isLooping) {
                                targetMs % duration
                            } else {
                                targetMs
                            }
                            player.seekTo(safeSeekMs)
                            player.start()
                            playingTracks.add(track.id)
                        } catch (e: Exception) {
                            Log.w("AudioPlaybackManager", "Error resuming player for ${track.title}: ${e.message}")
                        }
                    } else {
                        // Player is currently active; only correct drift if more than 300ms out of sync
                        val currentMs = try { player.currentPosition } catch (_: Exception) { -1 }
                        if (currentMs >= 0 && Math.abs(currentMs - targetMs) > 300) {
                            try {
                                player.seekTo(targetMs)
                            } catch (_: Exception) {}
                        }
                    }
                }
            } else {
                // Exited clip boundary: stop and release immediately
                activePlayers.remove(track.id)?.let { player ->
                    try {
                        player.stop()
                        player.release()
                    } catch (_: Exception) {}
                }
                playingTracks.remove(track.id)
            }
        }
    }

    /**
     * Overload supporting (audioTracks, currentTimeSeconds, isPlaying) signature.
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
            if (track.isMuted || track.volume <= 0f) {
                activePlayers.remove(track.id)?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
                playingTracks.remove(track.id)
                continue
            }

            val trackStart = track.startSeconds
            val trackEnd = track.startSeconds + track.durationSeconds
            val isInActiveSpan = (currentTimeSeconds >= trackStart && currentTimeSeconds < trackEnd)

            if (isInActiveSpan) {
                val offsetInTrack = (currentTimeSeconds - trackStart) + track.audioOffsetSeconds
                val targetMs = (offsetInTrack * 1000).toInt().coerceAtLeast(0)
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
                            targetMs.coerceAtMost((duration - 50).coerceAtLeast(0))
                        } else if (duration > 0 && track.isLooping) {
                            targetMs % duration
                        } else {
                            targetMs
                        }
                        val currentMs = p.currentPosition
                        if (Math.abs(currentMs - safeSeekMs) > 60) {
                            p.seekTo(safeSeekMs)
                        }
                    } catch (_: Exception) {}
                }
            } else {
                activePlayers.remove(track.id)?.let { p ->
                    try {
                        p.stop()
                        p.release()
                    } catch (_: Exception) {}
                }
                playingTracks.remove(track.id)
            }
        }
    }

    /**
     * Immediate live volume adjustment (e.g. from timeline volume slider).
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
                        playingTracks.remove(track.id)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("AudioPlaybackManager", "MediaPlayer error on '${track.title}': what=$what extra=$extra")
                    activePlayers.remove(track.id)?.let {
                        try { it.reset(); it.release() } catch (_: Exception) {}
                    }
                    playingTracks.remove(track.id)
                    true
                }
            }

            var resolvedPath = track.audioPath
            var dataSourceSet = false

            when {
                resolvedPath.startsWith("content://") || resolvedPath.startsWith("file://") -> {
                    try {
                        mp.setDataSource(context, Uri.parse(resolvedPath))
                        dataSourceSet = true
                    } catch (_: Exception) {}
                }
                resolvedPath.startsWith("/") -> {
                    val file = File(resolvedPath)
                    if (file.exists() && file.length() > 0L) {
                        try {
                            mp.setDataSource(file.absolutePath)
                            dataSourceSet = true
                        } catch (_: Exception) {}
                    }
                }
                resolvedPath.isNotBlank() -> {
                    val file = File(context.filesDir, resolvedPath)
                    if (file.exists() && file.length() > 0L) {
                        try {
                            mp.setDataSource(file.absolutePath)
                            dataSourceSet = true
                        } catch (_: Exception) {}
                    }
                }
            }

            if (!dataSourceSet) {
                // Ensure guaranteed fallback sample exists and is loaded
                val fallbackPath = mixerEngine.getSampleAudioPath(track.title, track.trackType)
                val fallbackFile = File(fallbackPath)
                if (fallbackFile.exists() && fallbackFile.length() > 0L) {
                    mp.setDataSource(fallbackFile.absolutePath)
                    dataSourceSet = true
                }
            }

            if (!dataSourceSet) {
                try { mp.release() } catch (_: Exception) {}
                return null
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
     * Pauses all media players.
     */
    fun pauseAll() {
        stopAll()
    }

    /**
     * Releases all active media players and resets audio state.
     */
    fun stopAll() {
        abandonAudioFocus()
        playingTracks.clear()
        for ((_, player) in activePlayers) {
            try {
                player.stop()
                player.release()
            } catch (_: Exception) {}
        }
        activePlayers.clear()
    }

    /**
     * Safe cleanup on ViewModel / Activity disposal.
     */
    fun release() {
        stopAll()
    }
}
