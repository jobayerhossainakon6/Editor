package com.example.model

data class ProjectRecord(
    val id: String,
    val name: String,
    val lastModified: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val totalDurationSeconds: Double = 0.0,
    val sceneCount: Int = 0,
    val subtitleCount: Int = 0
)

data class ImageSegment(
    val id: String,
    val fileName: String,
    val localPath: String,
    val durationSeconds: Double,
    val title: String = "",
    val transition: String = "None", // None (default - no auto blink), Crossfade, ZoomIn, SlideLeft, Wipe, DipToBlack
    val effect: String = "Normal", // Normal, FilmGrain, Cyberpunk, CinematicWarm, VintageVHS, BlackWhite, ZoomShake, Glitch
    val framingMode: String = "CropToFill", // CropToFill (no distortion, fills 16:9/9:16), FitBlurred (original aspect + blurred background)
    val trackIndex: Int = 0, // 0: Main track, 1: Overlay / PIP Track 1, 2: Overlay Track 2...
    val startTimeSeconds: Double = -1.0 // Optional explicit start time (-1.0 means sequential in track)
) {
    // layer_index alias matching user spec
    val layerIndex: Int get() = trackIndex
}

object ClassicTextStyle {
    const val NAME = "Classic Video Outline"
    const val TEXT_COLOR_HEX = "#FFFFFF"
    const val OUTLINE_COLOR_HEX = "#000000"
    const val OUTLINE_WIDTH = 6f
}

data class SubtitleSegment(
    val id: String,
    val startSeconds: Double,
    val endSeconds: Double,
    val text: String,
    val templateName: String = "hormozi_bold.ass",
    val animationPreset: String = "Pop-up", // Pop-up, Bounce, Word-Glow, Fade-In, Typewriter, Slide-Up, Zoom-Punch
    val textEffect: String = ClassicTextStyle.NAME, // Default pinned: Classic Video Outline
    val posX: Float = 0.5f, // 0.0f (left) to 1.0f (right), 0.5f is center
    val posY: Float = 0.82f, // 0.0f (top) to 1.0f (bottom), 0.82f is bottom third
    val fontSize: Int = 38,
    val fontName: String = "Impact", // e.g. Bebas Neue, Montserrat, Anton, Impact, etc.
    val textColorHex: String = ClassicTextStyle.TEXT_COLOR_HEX, // White text default
    val outlineColorHex: String = ClassicTextStyle.OUTLINE_COLOR_HEX, // Black outline stroke
    val outlineWidth: Float = ClassicTextStyle.OUTLINE_WIDTH, // Thick black stroke
    val bgBoxColorHex: String? = null, // Optional background pill box
    val trackIndex: Int = 0 // 0: Subtitle Track, 1: Title/Caption Track 2
)

data class AudioVolumeKeyframe(
    val timeOffsetSec: Double, // time relative to audio start (e.g. 0.0, 1.5)
    val volume: Float          // 0.0 to 1.0
)

data class AudioTrackItem(
    val id: String,
    val name: String,
    val localPath: String,
    val startSeconds: Double,
    val durationSeconds: Double,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val trackIndex: Int = 0,
    val layerType: String = "bgm",
    val fadeInSeconds: Double = 0.5,
    val fadeOutSeconds: Double = 0.5,
    val isLooping: Boolean = true,
    val isDuckingEnabled: Boolean = false,
    val duckVolume: Float = 0.25f,
    val volumeKeyframes: List<AudioVolumeKeyframe> = emptyList(),
    val audioOffsetSeconds: Double = 0.0
) {
    val title: String get() = name
    val audioPath: String get() = localPath
    val trackType: String get() = layerType
    val layerIndex: Int get() = trackIndex

    constructor(
        id: String,
        title: String,
        audioPath: String,
        startSeconds: Double,
        durationSeconds: Double,
        audioOffsetSeconds: Double = 0.0,
        volume: Float = 1.0f,
        isMuted: Boolean = false,
        trackType: String = "bgm"
    ) : this(
        id = id,
        name = title,
        localPath = audioPath,
        startSeconds = startSeconds,
        durationSeconds = durationSeconds,
        volume = volume,
        isMuted = isMuted,
        trackIndex = when (trackType.lowercase()) {
            "voiceover", "voice" -> 1
            "sfx" -> 2
            "ambient" -> 3
            else -> 0
        },
        layerType = trackType,
        fadeInSeconds = 0.5,
        fadeOutSeconds = 0.5,
        isLooping = true,
        isDuckingEnabled = false,
        duckVolume = 0.25f,
        volumeKeyframes = emptyList(),
        audioOffsetSeconds = audioOffsetSeconds
    )

    /**
     * Calculates the effective automation volume at an absolute timeline point.
     * Evaluates fade in, fade out, ducking, and custom automation keyframe curves.
     */
    fun getEffectiveVolumeAt(timelineTimeSec: Double, isVoiceoverActive: Boolean = false): Float {
        if (isMuted) return 0f
        if (timelineTimeSec < startSeconds || timelineTimeSec > startSeconds + durationSeconds) return 0f

        val relTime = timelineTimeSec - startSeconds
        var currentVol = volume.coerceIn(0f, 2f)

        // 1. Fade In automation
        if (fadeInSeconds > 0.0 && relTime < fadeInSeconds) {
            val factor = (relTime / fadeInSeconds).coerceIn(0.0, 1.0).toFloat()
            currentVol *= factor
        }

        // 2. Fade Out automation
        val timeRemaining = durationSeconds - relTime
        if (fadeOutSeconds > 0.0 && timeRemaining < fadeOutSeconds) {
            val factor = (timeRemaining / fadeOutSeconds).coerceIn(0.0, 1.0).toFloat()
            currentVol *= factor
        }

        // 3. Automation Keyframes curve interpolation
        if (volumeKeyframes.isNotEmpty()) {
            val sorted = volumeKeyframes.sortedBy { it.timeOffsetSec }
            when {
                relTime <= sorted.first().timeOffsetSec -> {
                    currentVol *= sorted.first().volume
                }
                relTime >= sorted.last().timeOffsetSec -> {
                    currentVol *= sorted.last().volume
                }
                else -> {
                    for (i in 0 until sorted.size - 1) {
                        val k1 = sorted[i]
                        val k2 = sorted[i + 1]
                        if (relTime in k1.timeOffsetSec..k2.timeOffsetSec) {
                            val span = (k2.timeOffsetSec - k1.timeOffsetSec).coerceAtLeast(0.001)
                            val factor = ((relTime - k1.timeOffsetSec) / span).toFloat()
                            val interpolated = k1.volume + (k2.volume - k1.volume) * factor
                            currentVol *= interpolated
                            break
                        }
                    }
                }
            }
        }

        // 4. Ducking automation: if this track is background music and voiceover is actively speaking
        if ((isDuckingEnabled || trackIndex == 0 || layerType == "bgm" || layerType == "music") && isVoiceoverActive && trackIndex != 1 && layerType != "voiceover") {
            currentVol *= duckVolume.coerceIn(0f, 1f)
        }

        return currentVol.coerceIn(0f, 2f)
    }
}

typealias AudioSegment = AudioTrackItem

data class AssTemplateInfo(
    val name: String,
    val fileName: String,
    val styleName: String,
    val fontName: String,
    val fontSize: Int,
    val primaryColorHex: String,
    val outlineColorHex: String,
    val description: String,
    val rawHeader: String
)

data class OnlineFontAsset(
    val name: String,
    val category: String, // "Trending Display", "Clean Sans", "Dramatic Comic", "Script / Handwriting"
    val sampleText: String,
    val isDownloaded: Boolean = true
)

data class OnlineEffectAsset(
    val id: String,
    val name: String,
    val type: String, // "video_filter", "text_animation", "text_style"
    val description: String,
    val iconName: String,
    val previewColorHex: String
)

data class ExportSettings(
    val resolution: String = "1080p", // "4K" (3840x2160/2160x3840), "2K" (2560x1440/1440x2560), "1080p", "720p"
    val frameRate: Int = 30, // 30 or 60 fps
    val aspectRatio: String = "16:9", // "16:9" (YouTube Landscape), "9:16" (Shorts/Reels/TikTok), "1:1" (Square), "4:5" (Instagram)
    val framingMode: String = "CropToFill", // "CropToFill" (fills 16:9 without stretching), "FitBlurred" (blurred background)
    val quality: String = "High (Baseline 3.0)"
)

data class RenderState(
    val isRendering: Boolean = false,
    val currentPass: Int = 0,
    val totalPasses: Int = 2,
    val progressPercent: Int = 0,
    val currentStepDescription: String = "",
    val pass1Arguments: List<String> = emptyList(),
    val pass2Arguments: List<String> = emptyList(),
    val concatFileContent: String = "",
    val assFileContent: String = "",
    val logs: List<String> = emptyList(),
    val isSuccess: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val outputVideoPath: String? = null,
    val exportSettings: ExportSettings = ExportSettings()
)
