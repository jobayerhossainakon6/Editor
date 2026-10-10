package com.example.engine

import android.content.Context
import com.example.audio.AudioPlaybackManager
import com.example.model.AudioSegment

/**
 * AudioPlayerEngine adapter delegating to high-precision AudioPlaybackManager.
 */
class AudioPlayerEngine(private val context: Context) {

    private val playbackManager = AudioPlaybackManager(context)

    fun syncPlayback(
        audios: List<AudioSegment>,
        currentTimeSec: Double,
        isPlaying: Boolean
    ) {
        playbackManager.syncPlayback(audios, isPlaying, currentTimeSec)
    }

    fun seekTo(audios: List<AudioSegment>, currentTimeSec: Double) {
        playbackManager.seekTo(audios, currentTimeSec)
    }

    fun pauseAll() {
        playbackManager.pauseAll()
    }

    fun updateVolume(trackId: String, volume: Float) {
        playbackManager.updateVolume(trackId, volume)
    }

    fun updateMute(trackId: String, isMuted: Boolean) {
        playbackManager.updateMute(trackId, isMuted)
    }

    fun stopAll() {
        playbackManager.stopAll()
    }

    fun release() {
        playbackManager.release()
    }
}
