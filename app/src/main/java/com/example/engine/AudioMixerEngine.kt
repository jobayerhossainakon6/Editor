package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import com.example.model.AudioSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Studio Multi-Layer Audio Mixer Engine with Automation & Dynamic Synthesis.
 * Handles synthesis of high-fidelity royalty-free tracks, multi-track mixing with
 * automated volume curves, fade in/out, ducking, and hardware AAC encoding.
 */
class AudioMixerEngine(private val context: Context) {

    private val audioDir: File get() = File(context.filesDir, "audio_tracks").apply { if (!exists()) mkdirs() }

    /**
     * Guarantees default royalty-free sample audio files exist on device storage.
     */
    fun ensureSampleAudioFilesExist() {
        val sampleConfigs = listOf(
            SampleDefinition("Lo-Fi Study Chill", "sample_lofi.wav", 120.0, 6.0, "lofi"),
            SampleDefinition("Cyberpunk Synth Beat", "sample_synth.wav", 135.0, 6.0, "synth"),
            SampleDefinition("Epic Cinematic Theme", "sample_cinematic.wav", 100.0, 6.0, "cinematic"),
            SampleDefinition("Upbeat Vlog Funk", "sample_vlog.wav", 128.0, 6.0, "vlog"),
            SampleDefinition("AI Voiceover Narration", "sample_voiceover.wav", 110.0, 6.0, "voiceover"),
            SampleDefinition("Cinematic Impact Punch", "sample_sfx_punch.wav", 120.0, 1.5, "sfx_punch"),
            SampleDefinition("Dynamic Transition Whoosh", "sample_sfx_whoosh.wav", 120.0, 1.5, "sfx_whoosh")
        )

        for (cfg in sampleConfigs) {
            val file = File(audioDir, cfg.fileName)
            if (!file.exists() || file.length() < 1000L) {
                try {
                    generateWavFile(file, cfg.durationSeconds, cfg.tempoBpm, cfg.style)
                } catch (e: Exception) {
                    Log.w("AudioMixerEngine", "Could not generate sample ${cfg.fileName}: ${e.message}")
                }
            }
        }
    }

    private data class SampleDefinition(
        val name: String,
        val fileName: String,
        val tempoBpm: Double,
        val durationSeconds: Double,
        val style: String
    )

    fun getSampleAudioPath(sampleName: String, layerType: String = "music"): String {
        ensureSampleAudioFilesExist()
        val fileName = when {
            sampleName.contains("punch", ignoreCase = true) || sampleName.contains("impact", ignoreCase = true) || layerType == "sfx" -> "sample_sfx_punch.wav"
            sampleName.contains("whoosh", ignoreCase = true) || sampleName.contains("swoosh", ignoreCase = true) -> "sample_sfx_whoosh.wav"
            sampleName.contains("voice", ignoreCase = true) || sampleName.contains("speech", ignoreCase = true) || layerType == "voiceover" -> "sample_voiceover.wav"
            sampleName.contains("cyber", ignoreCase = true) || sampleName.contains("synth", ignoreCase = true) -> "sample_synth.wav"
            sampleName.contains("cinematic", ignoreCase = true) || sampleName.contains("epic", ignoreCase = true) -> "sample_cinematic.wav"
            sampleName.contains("vlog", ignoreCase = true) || sampleName.contains("funk", ignoreCase = true) -> "sample_vlog.wav"
            else -> "sample_lofi.wav"
        }
        val file = File(audioDir, fileName)
        if (!file.exists() || file.length() < 1000L) {
            generateWavFile(file, 6.0, 120.0, if (layerType == "voiceover") "voiceover" else "lofi")
        }
        return file.absolutePath
    }

    /**
     * Synthesizes pleasant musical / speech / SFX WAV files (44.1kHz 16-bit Stereo PCM).
     */
    private fun generateWavFile(
        outputFile: File,
        durationSeconds: Double,
        tempoBpm: Double,
        style: String
    ) {
        val sampleRate = 44100
        val numChannels = 2
        val totalSamples = (durationSeconds * sampleRate).toInt().coerceAtLeast(sampleRate)
        val bytesPerSample = 2 // 16-bit
        val pcmDataSize = totalSamples * numChannels * bytesPerSample

        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                writeWavHeader(fos, pcmDataSize, sampleRate, numChannels)

                val chunkSize = 4096
                val buffer = ByteArray(chunkSize * numChannels * bytesPerSample)
                val secondsPerBeat = 60.0 / tempoBpm
                var sampleOffset = 0

                while (sampleOffset < totalSamples) {
                    val samplesToProcess = minOf(chunkSize, totalSamples - sampleOffset)
                    var bufIdx = 0

                    for (i in 0 until samplesToProcess) {
                        val sIdx = sampleOffset + i
                        val t = sIdx.toDouble() / sampleRate

                    val (valL, valR) = when (style) {
                        "sfx_punch" -> {
                            val decay = exp(-t * 4.5).coerceIn(0.0, 1.0)
                            val subFreq = 80.0 * exp(-t * 6.0) + 38.0
                            val punch = sin(2.0 * PI * subFreq * t) * decay * 0.95
                            Pair(punch, punch)
                        }
                        "sfx_whoosh" -> {
                            val center = durationSeconds / 2.0
                            val dist = Math.abs(t - center)
                            val env = exp(-dist * dist * 8.0)
                            val sweepFreq = 300.0 + (t / durationSeconds) * 2400.0
                            val noise = (sin(2.0 * PI * sweepFreq * t) + sin(2.0 * PI * (sweepFreq * 1.5) * t) * 0.5) * env * 0.75
                            val pan = (t / durationSeconds).coerceIn(0.0, 1.0)
                            Pair(noise * (1.0 - pan * 0.5), noise * (0.5 + pan * 0.5))
                        }
                        "voiceover" -> {
                            // Gentle speech-like warm harmonics + melodic narrator chime
                            val syllablePeriod = 0.28
                            val sylFraction = (t % syllablePeriod) / syllablePeriod
                            val sylEnv = sin(PI * sylFraction).coerceAtLeast(0.0)
                            val noteIdx = ((t / (secondsPerBeat * 2.0)).toInt() % 4)
                            val basePitch = when (noteIdx) {
                                0 -> 261.63 // C4
                                1 -> 329.63 // E4
                                2 -> 392.00 // G4
                                else -> 523.25 // C5
                            }
                            val voiceTone = (sin(2.0 * PI * basePitch * t) * 0.40 +
                                    sin(2.0 * PI * (basePitch * 2.0) * t) * 0.25 +
                                    sin(2.0 * PI * (basePitch * 3.0) * t) * 0.12) * sylEnv * 0.70
                            Pair(voiceTone, voiceTone)
                        }
                        "synth" -> {
                            // Driving synth wave
                            val beatFraction = (t % secondsPerBeat) / secondsPerBeat
                            val kick = sin(2.0 * PI * (75.0 + (1.0 - beatFraction) * 80.0) * t) * (1.0 - beatFraction).coerceAtLeast(0.0) * 0.55
                            val arpStep = ((t / (secondsPerBeat / 4.0)).toInt() % 8)
                            val arpFreq = when (arpStep) {
                                0 -> 164.8 // E3
                                1 -> 220.0 // A3
                                2 -> 261.6 // C4
                                3 -> 329.6 // E4
                                4 -> 392.0 // G4
                                5 -> 329.6 // E4
                                6 -> 261.6 // C4
                                else -> 220.0 // A3
                            }
                            val synthSaw = (sin(2.0 * PI * arpFreq * t) + sin(2.0 * PI * (arpFreq * 2.0) * t) * 0.4) * 0.22
                            val hat = sin(2.0 * PI * 6500.0 * t) * (1.0 - (t % (secondsPerBeat / 2.0)) / (secondsPerBeat / 2.0)).coerceAtLeast(0.0) * 0.08
                            val mixed = (kick + synthSaw + hat) * 0.85
                            Pair(mixed, mixed)
                        }
                        "cinematic" -> {
                            // Deep cinematic strings + sub swell
                            val chordStep = ((t / (secondsPerBeat * 2.0)).toInt() % 4)
                            val root = when (chordStep) {
                                0 -> 110.0 // A2
                                1 -> 130.8 // C3
                                2 -> 146.8 // D3
                                else -> 98.0  // G2
                            }
                            val pad = sin(2.0 * PI * root * t) * 0.35 +
                                    sin(2.0 * PI * (root * 1.5) * t) * 0.20 +
                                    sin(2.0 * PI * (root * 2.0) * t) * 0.15
                            val subPulse = sin(2.0 * PI * 48.0 * t) * (1.0 - (t % (secondsPerBeat * 2.0)) / (secondsPerBeat * 2.0)).coerceAtLeast(0.0) * 0.35
                            val mixed = (pad + subPulse) * 0.85
                            Pair(mixed * 0.95, mixed * 1.05)
                        }
                        else -> {
                            // Lo-Fi Study Chill (Default warm piano + soft beat)
                            val beatFraction = (t % secondsPerBeat) / secondsPerBeat
                            val kick = sin(2.0 * PI * (55.0 + (1.0 - beatFraction) * 60.0) * t) * (1.0 - beatFraction * 3.5).coerceAtLeast(0.0) * 0.45
                            val step = ((t / secondsPerBeat).toInt() % 4)
                            val baseFreq = when (step) {
                                0 -> 220.0 // A3
                                1 -> 174.6 // F3
                                2 -> 261.6 // C4
                                else -> 196.0 // G3
                            }
                            val melody = sin(2.0 * PI * baseFreq * t) * 0.20 +
                                    sin(2.0 * PI * (baseFreq * 1.5) * t) * 0.10 +
                                    sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.06
                            val snareFraction = (t % (secondsPerBeat * 2.0)) / (secondsPerBeat * 2.0)
                            val snare = if (snareFraction > 0.5) {
                                val sFrac = (snareFraction - 0.5) * 2.0
                                (sin(2.0 * PI * 180.0 * t) + sin(2.0 * PI * 3200.0 * t) * 0.5) * (1.0 - sFrac * 6.0).coerceAtLeast(0.0) * 0.25
                            } else 0.0
                            val mixed = (kick + melody + snare) * 0.85
                            Pair(mixed, mixed)
                        }
                    }

                    val sampleShortL = (valL.coerceIn(-0.95, 0.95) * 32767.0).toInt().toShort()
                    val sampleShortR = (valR.coerceIn(-0.95, 0.95) * 32767.0).toInt().toShort()

                    // Little-endian Stereo
                    buffer[bufIdx++] = (sampleShortL.toInt() and 0xFF).toByte()
                    buffer[bufIdx++] = ((sampleShortL.toInt() shr 8) and 0xFF).toByte()
                    buffer[bufIdx++] = (sampleShortR.toInt() and 0xFF).toByte()
                    buffer[bufIdx++] = ((sampleShortR.toInt() shr 8) and 0xFF).toByte()
                }

                fos.write(buffer, 0, samplesToProcess * numChannels * bytesPerSample)
                sampleOffset += samplesToProcess
            }
        }
        if (tempFile.exists()) {
            if (outputFile.exists()) outputFile.delete()
            tempFile.renameTo(outputFile)
        }
    } catch (e: Exception) {
        tempFile.delete()
        Log.w("AudioMixerEngine", "Error generating WAV ${outputFile.name}: ${e.message}")
    }
}

    private fun writeWavHeader(fos: FileOutputStream, pcmDataSize: Int, sampleRate: Int, numChannels: Int) {
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * numChannels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0 // PCM format
        header[22] = numChannels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (numChannels * 2).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmDataSize and 0xff).toByte()
        header[41] = ((pcmDataSize shr 8) and 0xff).toByte()
        header[42] = ((pcmDataSize shr 16) and 0xff).toByte()
        header[43] = ((pcmDataSize shr 24) and 0xff).toByte()

        fos.write(header)
    }

    /**
     * Mixes multiple audio tracks with full automation (fade-in, fade-out, ducking, keyframes)
     * and encodes to an AAC audio file (.m4a) for multiplexing into the exported MP4 video.
     */
    suspend fun mixAndEncodeAudioTracks(
        audios: List<AudioSegment>,
        totalDurationSeconds: Double,
        outputM4aFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val targetDuration = totalDurationSeconds.coerceAtLeast(0.5)
        val activeAudios = audios.filter { !it.isMuted && it.volume > 0f }

        // If no active audio provided, create a soft ambient background music bed
        val effectiveTracks = if (activeAudios.isNotEmpty()) {
            activeAudios
        } else {
            val defaultPath = getSampleAudioPath("Lo-Fi Study Chill", "music")
            listOf(
                AudioSegment(
                    id = "auto_bgm_export",
                    name = "Background Music",
                    localPath = defaultPath,
                    startSeconds = 0.0,
                    durationSeconds = targetDuration,
                    volume = 0.80f,
                    trackIndex = 0,
                    layerType = "music",
                    fadeInSeconds = 1.0,
                    fadeOutSeconds = 1.5,
                    isLooping = true
                )
            )
        }

        val sampleRate = 44100
        val channelCount = 2
        val totalSamples = (targetDuration * sampleRate).toInt().coerceAtLeast(sampleRate)

        val mixBufferL = FloatArray(totalSamples)
        val mixBufferR = FloatArray(totalSamples)

        // Precompute voiceover active intervals for automatic music ducking
        val voiceoverIntervals = effectiveTracks
            .filter { it.trackIndex == 1 || it.layerType == "voiceover" }
            .map { Pair(it.startSeconds, it.startSeconds + it.durationSeconds) }

        // Mix every track into the master stereo buffer with automation
        for (audio in effectiveTracks) {
            val audioStartSample = (audio.startSeconds * sampleRate).toInt().coerceAtLeast(0)
            val audioDurationSamples = (audio.durationSeconds * sampleRate).toInt()

            var audioFile = File(audio.localPath)
            if (!audioFile.exists() || audioFile.length() < 100L) {
                val fallbackPath = getSampleAudioPath(audio.name, audio.layerType)
                audioFile = File(fallbackPath)
            }

            mixTrackIntoMasterBuffer(
                file = audioFile,
                audio = audio,
                bufferL = mixBufferL,
                bufferR = mixBufferR,
                startSample = audioStartSample,
                durationSamples = audioDurationSamples,
                sampleRate = sampleRate,
                voiceoverIntervals = voiceoverIntervals
            )
        }

        // Apply studio soft limiter to prevent clipping & ensure loudness
        applySoftLimiter(mixBufferL, mixBufferR)

        // Encode to standard AAC (.m4a)
        encodePcmToAac(mixBufferL, mixBufferR, sampleRate, channelCount, outputM4aFile)
    }

    private fun mixTrackIntoMasterBuffer(
        file: File,
        audio: AudioSegment,
        bufferL: FloatArray,
        bufferR: FloatArray,
        startSample: Int,
        durationSamples: Int,
        sampleRate: Int,
        voiceoverIntervals: List<Pair<Double, Double>>
    ) {
        val totalSamples = bufferL.size
        val isVoiceover = audio.trackIndex == 1 || audio.layerType == "voiceover"

        // Check if file is valid WAV
        var isWav = false
        if (file.exists() && file.length() > 44L) {
            try {
                RandomAccessFile(file, "r").use { raf ->
                    val header = ByteArray(4)
                    raf.read(header)
                    if (String(header) == "RIFF") isWav = true
                }
            } catch (_: Exception) {}
        }

        if (isWav) {
            mixWavFileWithAutomation(file, audio, bufferL, bufferR, startSample, durationSamples, sampleRate, isVoiceover, voiceoverIntervals)
        } else if (file.exists() && file.length() > 1000L) {
            // Compressed format (MP3, M4A, AAC)
            val decoded = decodeCompressedAudioWithMediaCodec(file, audio, bufferL, bufferR, startSample, durationSamples, sampleRate, isVoiceover, voiceoverIntervals)
            if (!decoded) {
                // Procedural synthesis fallback
                synthesizeProceduralLayer(audio, bufferL, bufferR, startSample, durationSamples, sampleRate, isVoiceover, voiceoverIntervals)
            }
        } else {
            // Procedural synthesis fallback
            synthesizeProceduralLayer(audio, bufferL, bufferR, startSample, durationSamples, sampleRate, isVoiceover, voiceoverIntervals)
        }
    }

    private fun mixWavFileWithAutomation(
        file: File,
        audio: AudioSegment,
        bufferL: FloatArray,
        bufferR: FloatArray,
        startSample: Int,
        durationSamples: Int,
        sampleRate: Int,
        isVoiceover: Boolean,
        voiceoverIntervals: List<Pair<Double, Double>>
    ) {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(44)
                raf.read(header)
                val channels = if (header.size >= 24) header[22].toInt() and 0xFF else 2
                val wavSampleRate = if (header.size >= 28) {
                    (header[24].toInt() and 0xFF) or
                            ((header[25].toInt() and 0xFF) shl 8) or
                            ((header[26].toInt() and 0xFF) shl 16) or
                            ((header[27].toInt() and 0xFF) shl 24)
                } else 44100

                val dataSize = raf.length() - 44
                if (dataSize <= 0) return

                val pcmBytes = ByteArray(4096)
                var currentSample = 0
                val totalMasterSamples = bufferL.size

                while (currentSample < durationSamples) {
                    var bytesRead = raf.read(pcmBytes)
                    if (bytesRead <= 0) {
                        if (audio.isLooping) {
                            raf.seek(44)
                            bytesRead = raf.read(pcmBytes)
                            if (bytesRead <= 0) break
                        } else {
                            break
                        }
                    }

                    val bytesPerFrame = if (channels >= 2) 4 else 2
                    val framesInChunk = bytesRead / bytesPerFrame

                    for (i in 0 until framesInChunk) {
                        val sampleIdx = startSample + currentSample + i
                        if (sampleIdx >= totalMasterSamples) return

                        val timelineSec = sampleIdx.toDouble() / sampleRate
                        val isVoActive = !isVoiceover && voiceoverIntervals.any { timelineSec in it.first..it.second }
                        val automatedVolume = audio.getEffectiveVolumeAt(timelineSec, isVoActive)

                        if (channels >= 2) {
                            val lLow = pcmBytes[i * 4].toInt() and 0xFF
                            val lHigh = pcmBytes[i * 4 + 1].toInt()
                            val lShort = ((lHigh shl 8) or lLow).toShort()

                            val rLow = pcmBytes[i * 4 + 2].toInt() and 0xFF
                            val rHigh = pcmBytes[i * 4 + 3].toInt()
                            val rShort = ((rHigh shl 8) or rLow).toShort()

                            bufferL[sampleIdx] += (lShort.toFloat() / 32768.0f) * automatedVolume
                            bufferR[sampleIdx] += (rShort.toFloat() / 32768.0f) * automatedVolume
                        } else {
                            val low = pcmBytes[i * 2].toInt() and 0xFF
                            val high = pcmBytes[i * 2 + 1].toInt()
                            val mono = (((high shl 8) or low).toShort().toFloat() / 32768.0f) * automatedVolume
                            bufferL[sampleIdx] += mono
                            bufferR[sampleIdx] += mono
                        }
                    }
                    currentSample += framesInChunk
                }
            }
        } catch (e: Exception) {
            Log.w("AudioMixerEngine", "mixWavFileWithAutomation failed: ${e.message}")
        }
    }

    private fun decodeCompressedAudioWithMediaCodec(
        file: File,
        audio: AudioSegment,
        bufferL: FloatArray,
        bufferR: FloatArray,
        startSample: Int,
        durationSamples: Int,
        sampleRate: Int,
        isVoiceover: Boolean,
        voiceoverIntervals: List<Pair<Double, Double>>
    ): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(file.absolutePath) }
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) return false

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return false
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            var currentSample = 0
            val totalMasterSamples = bufferL.size

            while (currentSample < durationSamples) {
                if (!isEos) {
                    val inIdx = decoder.dequeueInputBuffer(5000L)
                    if (inIdx >= 0) {
                        val inputBuf = decoder.getInputBuffer(inIdx)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                if (audio.isLooping && currentSample < durationSamples) {
                                    extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                                } else {
                                    decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    isEos = true
                                }
                            } else {
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 5000L)
                if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        outBuf.order(ByteOrder.LITTLE_ENDIAN)
                        val shortBuf = outBuf.asShortBuffer()
                        val numShorts = shortBuf.remaining()

                        if (channelCount >= 2) {
                            val pairs = numShorts / 2
                            for (i in 0 until pairs) {
                                val sIdx = startSample + currentSample + i
                                if (sIdx >= totalMasterSamples) break
                                val timelineSec = sIdx.toDouble() / sampleRate
                                val isVoActive = !isVoiceover && voiceoverIntervals.any { timelineSec in it.first..it.second }
                                val automatedVolume = audio.getEffectiveVolumeAt(timelineSec, isVoActive)

                                val l = shortBuf.get().toFloat() / 32768.0f
                                val r = shortBuf.get().toFloat() / 32768.0f
                                bufferL[sIdx] += l * automatedVolume
                                bufferR[sIdx] += r * automatedVolume
                            }
                            currentSample += pairs
                        } else {
                            for (i in 0 until numShorts) {
                                val sIdx = startSample + currentSample + i
                                if (sIdx >= totalMasterSamples) break
                                val timelineSec = sIdx.toDouble() / sampleRate
                                val isVoActive = !isVoiceover && voiceoverIntervals.any { timelineSec in it.first..it.second }
                                val automatedVolume = audio.getEffectiveVolumeAt(timelineSec, isVoActive)

                                val mono = shortBuf.get().toFloat() / 32768.0f
                                bufferL[sIdx] += mono * automatedVolume
                                bufferR[sIdx] += mono * automatedVolume
                            }
                            currentSample += numShorts
                        }
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && isEos) {
                    break
                }
            }
            return true
        } catch (e: Exception) {
            Log.w("AudioMixerEngine", "MediaCodec compressed decode failed: ${e.message}")
            return false
        } finally {
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    private fun synthesizeProceduralLayer(
        audio: AudioSegment,
        bufferL: FloatArray,
        bufferR: FloatArray,
        startSample: Int,
        durationSamples: Int,
        sampleRate: Int,
        isVoiceover: Boolean,
        voiceoverIntervals: List<Pair<Double, Double>>
    ) {
        val totalMasterSamples = bufferL.size
        val secondsPerBeat = 0.5

        for (s in 0 until durationSamples) {
            val targetIdx = startSample + s
            if (targetIdx >= totalMasterSamples) break

            val timelineSec = targetIdx.toDouble() / sampleRate
            val isVoActive = !isVoiceover && voiceoverIntervals.any { timelineSec in it.first..it.second }
            val automatedVolume = audio.getEffectiveVolumeAt(timelineSec, isVoActive)

            val t = s.toDouble() / sampleRate
            val chord = sin(2.0 * PI * 220.0 * t) * 0.15 + sin(2.0 * PI * 330.0 * t) * 0.10
            val beat = sin(2.0 * PI * 65.0 * t) * (1.0 - (t % secondsPerBeat) / secondsPerBeat).coerceAtLeast(0.0) * 0.30
            val sampleVal = ((chord + beat) * automatedVolume).toFloat()

            bufferL[targetIdx] += sampleVal
            bufferR[targetIdx] += sampleVal
        }
    }

    private fun applySoftLimiter(bufferL: FloatArray, bufferR: FloatArray) {
        val count = bufferL.size
        for (i in 0 until count) {
            bufferL[i] = softKneeTanh(bufferL[i])
            bufferR[i] = softKneeTanh(bufferR[i])
        }
    }

    private fun softKneeTanh(x: Float): Float {
        return if (x > 1.0f) {
            1.0f - (1.0f / (1.0f + (x - 1.0f))) * 0.1f
        } else if (x < -1.0f) {
            -1.0f + (1.0f / (1.0f + (-x - 1.0f))) * 0.1f
        } else {
            x * 0.95f
        }
    }

    private fun encodePcmToAac(
        bufferL: FloatArray,
        bufferR: FloatArray,
        sampleRate: Int,
        channelCount: Int,
        outputFile: File
    ): Boolean {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 192000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            var muxerStarted = false

            val totalSamples = bufferL.size
            var sampleOffset = 0
            val samplesPerChunk = 2048
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false

            var lastWrittenPtsUs = -1L

            while (true) {
                if (!inputEos) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(5000L)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                            val remaining = totalSamples - sampleOffset
                            val toProcess = minOf(remaining, samplesPerChunk)

                            if (toProcess > 0) {
                                for (i in 0 until toProcess) {
                                    val idx = sampleOffset + i
                                    val sL = (bufferL[idx] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                                    val sR = (bufferR[idx] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                                    inputBuffer.putShort(sL)
                                    inputBuffer.putShort(sR)
                                }

                                val ptsUs = (sampleOffset.toDouble() / sampleRate * 1_000_000).toLong()
                                encoder.queueInputBuffer(inputBufferIndex, 0, toProcess * 4, ptsUs, 0)
                                sampleOffset += toProcess
                            } else {
                                inputEos = true
                                val ptsUs = (sampleOffset.toDouble() / sampleRate * 1_000_000).toLong()
                                encoder.queueInputBuffer(inputBufferIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                        }
                    }
                }

                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 5000L)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        // Guarantee monotonic presentation timestamps
                        if (bufferInfo.presentationTimeUs > lastWrittenPtsUs) {
                            muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                            lastWrittenPtsUs = bufferInfo.presentationTimeUs
                        }
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("AudioMixerEngine", "AAC Encoding failed: ${e.message}", e)
            return false
        } finally {
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }
    }
}
