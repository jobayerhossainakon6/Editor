package com.example.engine

import com.example.model.AssTemplateInfo
import com.example.model.SubtitleSegment
import java.util.Locale

object AssSubtitleParser {

    /**
     * Formats seconds into ASS timestamp: H:MM:SS.cs (e.g., 0:00:01.20)
     */
    fun secondsToAssTime(seconds: Double): String {
        val totalCentis = (seconds * 100).toLong().coerceAtLeast(0)
        val centis = totalCentis % 100
        val totalSecs = totalCentis / 100
        val secs = totalSecs % 60
        val totalMins = totalSecs / 60
        val mins = totalMins % 60
        val hours = totalMins / 60
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hours, mins, secs, centis)
    }

    /**
     * Parses ASS or SRT or standard timestamp string to seconds
     */
    fun parseTimeToSeconds(timeStr: String): Double {
        val clean = timeStr.trim().replace(",", ".")
        val parts = clean.split(":")
        return try {
            when (parts.size) {
                3 -> {
                    val h = parts[0].toDouble()
                    val m = parts[1].toDouble()
                    val s = parts[2].toDouble()
                    h * 3600.0 + m * 60.0 + s
                }
                2 -> {
                    val m = parts[0].toDouble()
                    val s = parts[1].toDouble()
                    m * 60.0 + s
                }
                1 -> parts[0].toDouble()
                else -> 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Strips ASS formatting tags like {\pos(540,1500)\fad(100,100)} for raw text preview
     */
    fun stripAssTags(rawText: String): String {
        return rawText.replace(Regex("""\{[^}]*\}"""), "").trim()
    }

    /**
     * Converts hex #RRGGBB or #AARRGGBB to ASS &HAABBGGRR format
     */
    fun hexToAssColor(hex: String, alphaHex: String = "00"): String {
        val clean = hex.removePrefix("#").removePrefix("&H")
        return if (clean.length == 6) {
            val r = clean.substring(0, 2)
            val g = clean.substring(2, 4)
            val b = clean.substring(4, 6)
            "&H$alphaHex$b$g$r"
        } else {
            "&H0000FFFF" // Yellow default
        }
    }

    /**
     * Builds full ASS file with Header, Styles, and Events dialogues
     * with exact pixel positioning (\pos) and custom animation tags.
     */
    fun buildAssScript(
        subtitles: List<SubtitleSegment>,
        templateInfo: AssTemplateInfo,
        playResX: Int = 1080,
        playResY: Int = 1920
    ): String {
        val sb = StringBuilder()
        sb.append("[Script Info]\n")
        sb.append("Title: Faceless Video Pro Subtitles\n")
        sb.append("ScriptType: v4.00+\n")
        sb.append("WrapStyle: 0\n")
        sb.append("ScaledBorderAndShadow: yes\n")
        sb.append("YCbCr Matrix: TV.601\n")
        sb.append("PlayResX: $playResX\n")
        sb.append("PlayResY: $playResY\n\n")

        sb.append("[V4+ Styles]\n")
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n")
        sb.append("Style: ${templateInfo.styleName},${templateInfo.fontName},${templateInfo.fontSize},${templateInfo.primaryColorHex},&H000000FF,${templateInfo.outlineColorHex},&H80000000,-1,0,0,0,105,105,2,0,1,6,3,2,60,60,260,1\n\n")

        sb.append("[Events]\n")
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n")

        for (sub in subtitles) {
            val startTime = secondsToAssTime(sub.startSeconds)
            val endTime = secondsToAssTime(sub.endSeconds)
            val styleName = templateInfo.styleName
            val escapedText = sub.text.replace("\n", "\\N")

            // Exact Pixel Coordinate Positioning
            val targetX = (sub.posX * playResX).toInt()
            val targetY = (sub.posY * playResY).toInt()

            // Animation tags according to preset
            val animTags = when (sub.animationPreset) {
                "Pop-up" -> "\\t(0,100,\\fscx115\\fscy115)\\t(100,200,\\fscx100\\fscy100)"
                "Bounce" -> "\\fad(80,80)\\t(0,80,\\fscy120)\\t(80,160,\\fscy100)"
                "Word-Glow" -> "\\fad(60,60)\\be1\\bord8"
                "Fade-In" -> "\\fad(180,180)"
                "Typewriter" -> "\\fad(50,50)"
                "Slide-Up" -> "\\move($targetX,${targetY + 80},$targetX,$targetY,0,180)"
                else -> "\\fad(80,80)"
            }

            val posTag = if (sub.animationPreset == "Slide-Up") animTags else "\\pos($targetX,$targetY)$animTags"
            val fontOverride = if (sub.fontName.isNotEmpty()) "\\fn${sub.fontName}" else ""
            val colorOverride = if (sub.textColorHex.isNotEmpty()) "\\1c${hexToAssColor(sub.textColorHex)}" else ""
            val outlineOverride = if (sub.outlineColorHex.isNotEmpty()) "\\3c${hexToAssColor(sub.outlineColorHex)}" else ""
            val sizeOverride = if (sub.fontSize > 0) "\\fs${sub.fontSize * 2}" else ""

            sb.append("Dialogue: 0,$startTime,$endTime,$styleName,,0,0,0,,{$posTag$fontOverride$colorOverride$outlineOverride$sizeOverride}$escapedText\n")
        }

        return sb.toString()
    }
}
