package com.example.engine

import com.example.model.ImageSegment
import java.io.File
import java.util.Locale

object FFmpegCommandBuilder {

    /**
     * Generates concat text file content.
     * CRITICAL BUG FIX IMPLEMENTATION:
     * Repeat the last image path at the end of the generated concat text file
     * to prevent FFmpeg concat demuxer from truncating the final image duration.
     */
    fun buildConcatFileContent(images: List<ImageSegment>): String {
        if (images.isEmpty()) return ""
        val sb = StringBuilder()

        val firstExisting = images.firstOrNull { File(it.localPath).exists() }?.localPath

        for (img in images) {
            val resolved = if (File(img.localPath).exists()) img.localPath else (firstExisting ?: img.localPath)
            val safePath = resolved.replace("'", "'\\''")
            sb.append("file '$safePath'\n")
            sb.append(String.format(Locale.US, "duration %.3f\n", img.durationSeconds))
        }

        // CRITICAL BUG FIX: Repeat the last image path at the end of the concat file
        val lastImg = images.last()
        val lastResolved = if (File(lastImg.localPath).exists()) lastImg.localPath else (firstExisting ?: lastImg.localPath)
        val lastPath = lastResolved.replace("'", "'\\''")
        sb.append("file '$lastPath'\n")

        return sb.toString()
    }

    /**
     * PASS 1 Arguments Array:
     * - Enforces exact duration if provided: -t <totalDuration> (PREVENTS 3-HOUR BUG!)
     * - Concat images with exact duration
     * - Force even dimensions: -vf "fps=30,scale=trunc(iw/2)*2:trunc(ih/2)*2,setsar=1"
     * - Guarantee Android Gallery playback: -c:v libx264 -pix_fmt yuv420p -profile:v baseline -level 3.0 -movflags +faststart
     * - Array format (NOT single strings)
     */
    fun buildPass1Arguments(
        concatFile: File,
        pass1OutputFile: File,
        totalDuration: Double = 0.0,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): Array<String> {
        val list = mutableListOf(
            "-y",
            "-f", "concat",
            "-safe", "0",
            "-i", concatFile.absolutePath
        )

        if (totalDuration > 0.05) {
            list.add("-t")
            list.add(String.format(Locale.US, "%.3f", totalDuration))
        }

        list.addAll(
            listOf(
                "-vf", "fps=30,scale=trunc(iw/2)*2:trunc(ih/2)*2,setsar=1",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-profile:v", "baseline",
                "-level", "3.0",
                "-movflags", "+faststart",
                pass1OutputFile.absolutePath
            )
        )
        return list.toTypedArray()
    }

    /**
     * PASS 2 Arguments Array:
     * - Takes Pass 1 video output
     * - Burns in local .ass subtitle template effects via subtitles filter
     * - Enforces exact duration: -t <totalDuration>
     * - Guarantee Android Gallery playback: -c:v libx264 -pix_fmt yuv420p -profile:v baseline -level 3.0 -movflags +faststart
     * - Array format (NOT single strings)
     */
    fun buildPass2Arguments(
        pass1OutputFile: File,
        assFile: File,
        finalOutputFile: File,
        totalDuration: Double = 0.0
    ): Array<String> {
        val list = mutableListOf(
            "-y",
            "-i", pass1OutputFile.absolutePath
        )

        if (totalDuration > 0.05) {
            list.add("-t")
            list.add(String.format(Locale.US, "%.3f", totalDuration))
        }

        list.addAll(
            listOf(
                "-vf", "subtitles=${assFile.absolutePath}",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-profile:v", "baseline",
                "-level", "3.0",
                "-movflags", "+faststart",
                finalOutputFile.absolutePath
            )
        )
        return list.toTypedArray()
    }

    /**
     * Builds a chained FFmpeg drawtext filter string for ALL text layers.
     * Ensures all subsequent texts are rendered sequentially without stopping at the first one.
     */
    fun buildDrawtextChainFilter(
        subtitles: List<com.example.model.SubtitleSegment>,
        fontFile: File? = null
    ): String {
        if (subtitles.isEmpty()) return "null"
        return subtitles.joinToString(",") { sub ->
            val cleanText = sub.text.replace("'", "'\\''").replace(":", "\\:").replace("%", "\\%")
            val fontArg = if (fontFile != null && fontFile.exists()) ":fontfile='${fontFile.absolutePath}'" else ""
            val startTime = String.format(Locale.US, "%.3f", sub.startSeconds)
            val endTime = String.format(Locale.US, "%.3f", sub.endSeconds)
            val x = (sub.posX * 100).toInt()
            val y = (sub.posY * 100).toInt()
            val outlineW = sub.outlineWidth.toInt().coerceAtLeast(2)
            "drawtext=text='$cleanText'$fontArg:fontsize=${sub.fontSize}:fontcolor=${sub.textColorHex}:borderw=$outlineW:bordercolor=${sub.outlineColorHex}:x=(w*${x}/100)-(tw/2):y=(h*${y}/100)-(th/2):enable='between(t,$startTime,$endTime)'"
        }
    }

    /**
     * Builds xfade transition filter between consecutive clips for FFmpeg complex filter graphs.
     */
    fun buildXfadeFilterComplex(
        images: List<ImageSegment>,
        transitionDuration: Double = 0.5
    ): String {
        if (images.size < 2) return ""
        val sb = StringBuilder()
        var offset = 0.0
        for (i in 0 until images.size - 1) {
            val img = images[i]
            offset += img.durationSeconds - transitionDuration
            val inputA = if (i == 0) "[0:v]" else "[v$i]"
            val inputB = "[${i + 1}:v]"
            val output = if (i == images.size - 2) "[vout]" else "[v${i + 1}]"
            val trans = when (img.transition.lowercase()) {
                "slide left" -> "slideleft"
                "slide right" -> "slideright"
                "push up" -> "smoothup"
                "push down" -> "smoothdown"
                "dip to black", "fade" -> "fadeblack"
                "dip to white" -> "fadewhite"
                "wipe", "wipe circle" -> "circlecrop"
                "zoom in" -> "zoomin"
                else -> "fade"
            }
            sb.append("$inputA$inputB xfade=transition=$trans:duration=${String.format(Locale.US, "%.2f", transitionDuration)}:offset=${String.format(Locale.US, "%.2f", offset)}$output;")
        }
        return sb.toString()
    }

    /**
     * Builds FFmpeg arguments to mix audio tracks into video using amix filter.
     * Uses `-filter_complex amix=inputs=...:duration=first:dropout_transition=2` so audio mixes seamlessly with video.
     */
    fun buildAudioMixArguments(
        videoFile: File,
        audioTracks: List<com.example.model.AudioSegment>,
        outputFile: File,
        totalDuration: Double = 0.0
    ): Array<String> {
        val validAudios = audioTracks.filter { it.localPath.isNotEmpty() && File(it.localPath).exists() && !it.isMuted }
        if (validAudios.isEmpty()) {
            return arrayOf("-y", "-i", videoFile.absolutePath, "-c", "copy", outputFile.absolutePath)
        }

        val list = mutableListOf("-y", "-i", videoFile.absolutePath)
        for (aud in validAudios) {
            list.add("-i")
            list.add(aud.localPath)
        }

        // Build amix filter complex
        // Map audio inputs: [1:a][2:a]... amix=inputs=N:duration=first:dropout_transition=2 [aout]
        val sb = StringBuilder()
        val totalAudioInputs = validAudios.size
        for (i in 1..totalAudioInputs) {
            sb.append("[$i:a]")
        }
        sb.append("amix=inputs=$totalAudioInputs:duration=first:dropout_transition=2[aout]")

        list.add("-filter_complex")
        list.add(sb.toString())

        list.add("-map")
        list.add("0:v")
        list.add("-map")
        list.add("[aout]")

        list.add("-c:v")
        list.add("copy")
        list.add("-c:a")
        list.add("aac")
        list.add("-b:a")
        list.add("192k")

        if (totalDuration > 0.05) {
            list.add("-t")
            list.add(String.format(Locale.US, "%.3f", totalDuration))
        }

        list.add(outputFile.absolutePath)
        return list.toTypedArray()
    }
}
