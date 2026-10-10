package com.example.video

import com.example.model.AudioTrackItem
import java.util.Locale

object AudioExportCommandBuilder {
    fun buildFfmpegAudioFilters(
        audioTracks: List<AudioTrackItem>,
        inputsList: MutableList<String>,
        startInputIndex: Int
    ): Pair<String?, List<String>> {
        val filterParts = mutableListOf<String>()
        val audioLabels = mutableListOf<String>()
        var inputIndex = startInputIndex

        audioTracks.filterNot { it.isMuted }.forEachIndexed { idx, track ->
            inputsList.add("-i")
            inputsList.add(track.audioPath)
            val trackInput = inputIndex
            inputIndex++

            val delayMs = (track.startSeconds * 1000).toLong()
            val volumeStr = String.format(Locale.US, "%.2f", track.volume)
            val audLabel = "a_mix_$idx"

            if (delayMs > 0) {
                filterParts.add("[$trackInput:a]volume=$volumeStr,adelay=$delayMs|$delayMs[$audLabel]")
            } else {
                filterParts.add("[$trackInput:a]volume=$volumeStr[$audLabel]")
            }
            audioLabels.add(audLabel)
        }

        if (audioLabels.isEmpty()) {
            return Pair(null, emptyList())
        }

        val finalAudioLabel = if (audioLabels.size == 1) {
            audioLabels[0]
        } else {
            val mixInputs = audioLabels.joinToString("") { "[$it]" }
            val combinedLabel = "a_final"
            filterParts.add(
                "$mixInputs amix=inputs=${audioLabels.size}:duration=longest:dropout_transition=0[$combinedLabel]"
            )
            combinedLabel
        }

        return Pair(finalAudioLabel, filterParts)
    }
}
