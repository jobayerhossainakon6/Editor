package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectRepository
import com.example.model.AudioSegment
import com.example.model.AudioVolumeKeyframe
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioEngineTest {

    @Test
    fun testAudioAutomationFadeInAndFadeOut() {
        val audio = AudioSegment(
            id = "aud_test_1",
            name = "Test Audio",
            localPath = "/mock/path.wav",
            startSeconds = 2.0,
            durationSeconds = 10.0,
            volume = 1.0f,
            fadeInSeconds = 2.0,
            fadeOutSeconds = 2.0
        )

        // At start (relative time 0.0s), volume is 0
        assertEquals(0.0f, audio.getEffectiveVolumeAt(2.0), 0.01f)

        // At halfway through fade in (relative time 1.0s, timeline 3.0s), volume is 0.5
        assertEquals(0.5f, audio.getEffectiveVolumeAt(3.0), 0.05f)

        // At steady state (timeline 6.0s), volume is full 1.0
        assertEquals(1.0f, audio.getEffectiveVolumeAt(6.0), 0.01f)

        // At halfway through fade out (remaining 1.0s, timeline 11.0s), volume is 0.5
        assertEquals(0.5f, audio.getEffectiveVolumeAt(11.0), 0.05f)

        // Outside boundary (timeline 13.0s), volume is 0.0
        assertEquals(0.0f, audio.getEffectiveVolumeAt(13.0), 0.01f)
    }

    @Test
    fun testAutoDuckingWhenVoiceoverIsActive() {
        val musicAudio = AudioSegment(
            id = "music_track",
            name = "Background Music",
            localPath = "/mock/music.wav",
            startSeconds = 0.0,
            durationSeconds = 20.0,
            volume = 0.8f,
            trackIndex = 0,
            layerType = "music",
            fadeInSeconds = 0.0,
            fadeOutSeconds = 0.0,
            isDuckingEnabled = true,
            duckVolume = 0.25f
        )

        // When voiceover is NOT active: volume is 0.8
        assertEquals(0.8f, musicAudio.getEffectiveVolumeAt(5.0, isVoiceoverActive = false), 0.01f)

        // When voiceover is active: volume is ducked by 0.25 -> 0.8 * 0.25 = 0.20
        assertEquals(0.20f, musicAudio.getEffectiveVolumeAt(5.0, isVoiceoverActive = true), 0.01f)
    }

    @Test
    fun testCustomVolumeKeyframeInterpolation() {
        val keyframes = listOf(
            AudioVolumeKeyframe(timeOffsetSec = 0.0, volume = 0.2f),
            AudioVolumeKeyframe(timeOffsetSec = 4.0, volume = 1.0f)
        )

        val audio = AudioSegment(
            id = "aud_keyframe",
            name = "Keyframe Track",
            localPath = "/mock/keyframe.wav",
            startSeconds = 0.0,
            durationSeconds = 10.0,
            volume = 1.0f,
            fadeInSeconds = 0.0,
            fadeOutSeconds = 0.0,
            volumeKeyframes = keyframes
        )

        // At offset 0.0s -> 0.2
        assertEquals(0.2f, audio.getEffectiveVolumeAt(0.0), 0.01f)

        // At offset 2.0s (midpoint of 0.2 to 1.0) -> 0.6
        assertEquals(0.6f, audio.getEffectiveVolumeAt(2.0), 0.05f)

        // At offset 4.0s -> 1.0
        assertEquals(1.0f, audio.getEffectiveVolumeAt(4.0), 0.01f)
    }

    @Test
    fun testAudioSyncJsonRoundtripSerialization() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProjectRepository(context)

        val testAudios = listOf(
            AudioSegment(
                id = "aud_01",
                name = "Lo-Fi Study Beat",
                localPath = "/path/sample_lofi.wav",
                startSeconds = 0.0,
                durationSeconds = 15.0,
                volume = 0.85f,
                trackIndex = 0,
                layerType = "music",
                fadeInSeconds = 1.0,
                fadeOutSeconds = 1.5,
                isLooping = true,
                isDuckingEnabled = true,
                duckVolume = 0.25f
            ),
            AudioSegment(
                id = "aud_02",
                name = "Voiceover Track",
                localPath = "/path/voice.wav",
                startSeconds = 1.0,
                durationSeconds = 6.0,
                volume = 0.95f,
                trackIndex = 1,
                layerType = "voiceover",
                fadeInSeconds = 0.2,
                fadeOutSeconds = 0.2,
                isLooping = false
            )
        )

        val json = repo.generateAudioSyncJson(testAudios)
        assertTrue(json.contains("Lo-Fi Study Beat"))
        assertTrue(json.contains("fade_in_seconds"))
        assertTrue(json.contains("ducking"))

        val parsed = repo.parseAudioSyncJson(json)
        assertEquals(2, parsed.size)
        assertEquals("Lo-Fi Study Beat", parsed[0].name)
        assertEquals(0, parsed[0].trackIndex)
        assertEquals("music", parsed[0].layerType)
        assertEquals(1.0, parsed[0].fadeInSeconds, 0.01)
        assertEquals(true, parsed[0].isDuckingEnabled)
        assertEquals(0.25f, parsed[0].duckVolume, 0.01f)

        assertEquals("Voiceover Track", parsed[1].name)
        assertEquals(1, parsed[1].trackIndex)
        assertEquals("voiceover", parsed[1].layerType)
    }

    @Test
    fun testAudioExportCommandBuilderFilterComplex() {
        val tracks = listOf(
            AudioSegment(
                id = "aud_track_1",
                title = "Background Music",
                audioPath = "/data/music.wav",
                startSeconds = 0.0,
                durationSeconds = 15.0,
                volume = 0.8f
            ),
            AudioSegment(
                id = "aud_track_2",
                title = "Narration Voice",
                audioPath = "/data/voice.wav",
                startSeconds = 2.5,
                durationSeconds = 8.0,
                volume = 1.0f
            )
        )

        val inputs = mutableListOf<String>()
        val (finalLabel, filterParts) = com.example.video.AudioExportCommandBuilder.buildFfmpegAudioFilters(
            audioTracks = tracks,
            inputsList = inputs,
            startInputIndex = 1
        )

        assertEquals(4, inputs.size) // -i music.wav -i voice.wav
        assertEquals("-i", inputs[0])
        assertEquals("/data/music.wav", inputs[1])
        assertEquals("-i", inputs[2])
        assertEquals("/data/voice.wav", inputs[3])

        assertEquals("a_final", finalLabel)
        assertEquals(3, filterParts.size)
        // Track 1 starts at 0.0s -> no adelay
        assertTrue(filterParts[0].contains("[1:a]volume=0.80[a_mix_0]"))
        // Track 2 starts at 2.5s -> adelay=2500|2500
        assertTrue(filterParts[1].contains("adelay=2500|2500"))
        // Combined with amix
        assertTrue(filterParts[2].contains("amix=inputs=2"))
    }

    @Test
    fun testAudioPlaybackManagerSyncAndStop() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.audio.AudioPlaybackManager(context)

        val testTracks = listOf(
            AudioSegment(
                id = "bgm_1",
                title = "Lo-Fi Music",
                audioPath = "",
                startSeconds = 0.0,
                durationSeconds = 10.0,
                volume = 0.8f
            )
        )

        // Scrubbing to 3.0s while paused
        manager.seekTo(testTracks, 3.0)

        // Syncing playback
        manager.syncPlayback(testTracks, isPlaying = true, currentTimeSeconds = 1.0)

        // Updating live volume
        manager.updateVolume("bgm_1", 0.5f)

        // Stop all on pause
        manager.stopAll()
    }
}
