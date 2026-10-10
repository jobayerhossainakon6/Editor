# SYSTEM PROMPT & ARCHITECTURAL SPECIFICATION: CAPCUT-STYLE VIDEO & SUBTITLE EDITOR

## 1. PROJECT OVERVIEW
Build a high-performance, studio-grade video editing application modeled after CapCut. The application delivers smooth 60 FPS timeline scrubbing, free-form multi-track clip arrangement, multi-layer text/subtitles with visual outline styling, synchronized background audio playback, and hardware-accelerated video rendering.

---

## 2. TIMELINE SYSTEM & DRAG-AND-DROP MECHANICS

### 2.1. Absolute Coordinate Placement & Gap Preservation
- Traditional video editors often force clips into a linear list where clips automatically snap end-to-end. This editor requires an absolute layout canvas (e.g., Box with pixel offsets or a Canvas/Stack).
- Every media clip possesses two core temporal attributes:
  1. `startTimeSeconds: Double` (Absolute start time on the project timeline)
  2. `durationSeconds: Double` (Play duration of this clip)
- Position calculation formula:
  ```kotlin
  val leftOffsetDp = (clip.startTimeSeconds * baseWidthPerSecond * zoomScale).dp
  val clipWidthDp = (clip.durationSeconds * baseWidthPerSecond * zoomScale).dp
  ```
- **Handling Gaps:** When a user leaves a time gap between clips (e.g., Scene 1 ends at 3.0s and Scene 2 begins at 5.0s), the timeline must not pull Scene 2 back to 3.0s. During the 3.0s-5.0s window, the player renders a clean black canvas without UI stutter or desync.

### 2.2. Dynamic Multi-Layering (Vertical Dragging)
- Implement vertical gesture detection alongside horizontal scrubbing:
  ```kotlin
  detectDragGestures(
      onDragStart = { isDragging = true },
      onDragEnd = {
          val dy = dragAccumY
          val dx = dragAccumX
          val thresholdY = 45f // pixels
          val pxPerSec = (baseWidthPerSecond * zoomScale * density).toFloat()
          val deltaTimeSec = if (pxPerSec > 0f) (dx / pxPerSec).toDouble() else 0.0
          
          val newStartTime = (clip.startTimeSeconds + deltaTimeSec).coerceAtLeast(0.0)
          val newLayerIndex = when {
              dy > thresholdY -> clip.trackIndex + 1
              dy < -thresholdY && clip.trackIndex > 0 -> clip.trackIndex - 1
              else -> clip.trackIndex
          }
          onUpdateClipPositionAndLayer(clip.id, newStartTime, newLayerIndex)
          isDragging = false
      },
      onDrag = { change, dragAmount ->
          change.consume()
          dragAccumX += dragAmount.x
          dragAccumY += dragAmount.y
      }
  )
  ```
- Dragging downwards past `thresholdY` increments the clip's `trackIndex` (creating video track lanes: `V1`, `V2`, `V3`, etc.).
- While being dragged, the clip opacity drops to `0.65f` to give visual feedback.

### 2.3. Responsive Red Playhead Scrubber
- The red scrubber needle line spans the entire height of the timeline.
- Wrap it with a touch target hit-box of at least 48dp to 64dp centered horizontally over the 2dp vertical line.
- Calculate seek delta with sensitivity inversely scaled to current zoom:
  ```kotlin
  val sensitivityMultiplier = (1.0f / zoomScale.coerceIn(0.2f, 3.0f))
  val deltaSeconds = (dragAmount.x * sensitivityMultiplier) / (baseWidthPerSecond * zoomScale * density)
  val newTime = (currentPlayTime + deltaSeconds).coerceIn(0.0, totalTimelineDuration)
  onSeek(newTime)
  ```

---

## 3. MULTI-LAYER JSON SPECIFICATION

### 3.1. `image_sync.json` (Media Layers & PIP Overlays)
Enables syncing visual scenes with explicit layer indices and absolute start times:
```json
{
  "scenes": [
    {
      "id": "scene_01",
      "file": "scene_01.jpg",
      "start_time": 0.0,
      "duration": 3.0,
      "layer_index": 0,
      "transition": "Crossfade",
      "effect": "FilmGrain"
    },
    {
      "id": "scene_overlay_pip",
      "file": "overlay_pip.png",
      "start_time": 1.2,
      "duration": 2.5,
      "layer_index": 1,
      "transition": "None",
      "effect": "Normal"
    }
  ]
}
```

### 3.2. `text_sync.json` (Subtitles & Text Formatting)
```json
{
  "subtitles": [
    {
      "id": "sub_01",
      "start_seconds": 0.0,
      "end_seconds": 2.5,
      "text": "THE UNTOLD TRUTH ABOUT MONEY",
      "preset": "Classic Video Outline",
      "animation": "Pop-up",
      "pos_x": 0.5,
      "pos_y": 0.82,
      "font_size": 38,
      "font_name": "Impact",
      "text_color": "#FFFFFF",
      "outline_color": "#000000",
      "outline_width": 6.0
    }
  ]
}
```

### 3.3. `audio_sync.json` (Background Music & Voiceover)
```json
{
  "audios": [
    {
      "id": "aud_01",
      "name": "Background Beat.mp3",
      "file": "music.mp3",
      "start_seconds": 0.0,
      "duration_seconds": 15.0,
      "volume": 0.75,
      "is_muted": false
    }
  ]
}
```

---

## 4. PREVIEW CANVAS & TEXT OUTLINE RENDERING

### 4.1. Real-Time Canvas Multi-Layer Composition
The preview canvas represents a vertical 9:16 (or custom aspect ratio) monitor:
1. **Base Track (`layer_index = 0`):**
   Evaluate all Track 0 clips against `currentPlayTime`. If the playhead falls into an empty gap, render a solid dark background (`Color(0xFF000000)`).
2. **PIP Overlays (`layer_index >= 1`):**
   Filter all clips where:
   `currentPlayTime >= clip.startTime && currentPlayTime < (clip.startTime + clip.duration)`
   Sort these clips by ascending `layer_index` and draw them over the base canvas.
3. **Dual-Pass Text Outline Rendering (Classic Video Outline Preset):**
   In both live preview canvas and video export render engines, text outlines must be drawn in two passes to avoid muddy strokes:
   ```kotlin
   // Pass 1: Outline stroke
   val strokePaint = Paint().apply {
       typeface = impactTypeface
       textSize = calculatedFontSize
       color = android.graphics.Color.BLACK
       style = Paint.Style.STROKE
       strokeWidth = outlineWidth * 2f // e.g. 12f
       strokeCap = Paint.Cap.ROUND
       strokeJoin = Paint.Join.ROUND
       textAlign = Paint.Align.CENTER
   }
   canvas.drawText(cleanText, targetX, targetY, strokePaint)

   // Pass 2: Foreground solid fill
   val fillPaint = Paint().apply {
       typeface = impactTypeface
       textSize = calculatedFontSize
       color = android.graphics.Color.WHITE
       style = Paint.Style.FILL
       textAlign = Paint.Align.CENTER
   }
   canvas.drawText(cleanText, targetX, targetY, fillPaint)
   ```

---

## 5. ADVANCED SUBSTATION ALPHA (.ASS) SUBTITLE ENGINE

### 5.1. What is .ASS and Why Use It?
Advanced SubStation Alpha (`.ass`) is the industry standard for hardcoding styled subtitles in FFmpeg. Unlike `.srt`, which only supports plain text, `.ass` allows precise font size, border thickness, drop shadows, per-dialogue screen coordinates, and fade animations.

### 5.2. Automated .ASS Generator Implementation
```kotlin
object AssSubtitleParser {
    fun buildAssScript(
        subtitles: List<SubtitleSegment>,
        width: Int = 1080,
        height: Int = 1920
    ): String {
        val sb = StringBuilder()
        sb.appendLine("[Script Info]")
        sb.appendLine("ScriptType: v4.00+")
        sb.appendLine("PlayResX: $width")
        sb.appendLine("PlayResY: $height")
        sb.appendLine()
        sb.appendLine("[V4+ Styles]")
        sb.appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, BackColour, Bold, Outline, Shadow, Alignment, MarginV")
        sb.appendLine("Style: ClassicOutline,Impact,48,&H00FFFFFF,&H00000000,-1,6,2,2,100")
        sb.appendLine()
        sb.appendLine("[Events]")
        sb.appendLine("Format: Layer, Start, End, Style, Text")

        for (sub in subtitles) {
            val start = secondsToAssTime(sub.startSeconds)
            val end = secondsToAssTime(sub.endSeconds)
            val posX = (sub.posX * width).toInt()
            val posY = (sub.posY * height).toInt()
            sb.appendLine("Dialogue: 0,$start,$end,ClassicOutline,{\\pos($posX,$posY)}${sub.text}")
        }
        return sb.toString()
    }

    fun secondsToAssTime(seconds: Double): String {
        val totalSec = seconds.coerceAtLeast(0.0)
        val h = (totalSec / 3600).toInt()
        val m = ((totalSec % 3600) / 60).toInt()
        val s = (totalSec % 60).toInt()
        val cs = ((totalSec - totalSec.toInt()) * 100).toInt()
        return String.format(java.util.Locale.US, "%d:%02d:%02d.%02d", h, m, s, cs)
    }
}
```

---

## 6. AUDIO PLAYER ENGINE & FFMPEG AUDIO MIXING

### 6.1. Audio Player Engine (Real-Time Playback Sync)
- Maintains active `MediaPlayer` instances mapped by audio track ID.
- Continuously synchronized with the global timeline playhead:
  ```kotlin
  fun syncPlayback(audios: List<AudioSegment>, currentPlayTime: Double, isPlaying: Boolean) {
      if (!isPlaying) {
          players.values.forEach { if (it.isPlaying) it.pause() }
          return
      }
      for (audio in audios) {
          val start = audio.startSeconds
          val end = start + audio.durationSeconds
          if (currentPlayTime in start..end) {
              val offsetMs = ((currentPlayTime - start) * 1000).toInt()
              val player = players.getOrPut(audio.id) { createPlayer(audio) }
              player.setVolume(audio.volume, audio.volume)
              if (Math.abs(player.currentPosition - offsetMs) > 250) {
                  player.seekTo(offsetMs)
              }
              if (!player.isPlaying) player.start()
          } else {
              players[audio.id]?.takeIf { it.isPlaying }?.pause()
          }
      }
  }
  ```

### 6.2. FFmpeg Export Audio Mixing (`amix`)
When exporting the final MP4, mix base video audio with all imported audio tracks:
```kotlin
fun buildAudioMixArguments(
    videoFile: File,
    audioTracks: List<AudioSegment>,
    outputFile: File,
    totalDuration: Double
): Array<String> {
    val validAudios = audioTracks.filter { File(it.localPath).exists() && !it.isMuted }
    if (validAudios.isEmpty()) {
        return arrayOf("-y", "-i", videoFile.absolutePath, "-c", "copy", outputFile.absolutePath)
    }

    val list = mutableListOf("-y", "-i", videoFile.absolutePath)
    for (aud in validAudios) {
        list.add("-i")
        list.add(aud.localPath)
    }

    val totalInputs = validAudios.size
    val filterComplex = (1..totalInputs).joinToString("") { "[$it:a]" } +
        "amix=inputs=$totalInputs:duration=first:dropout_transition=2[aout]"

    list.addAll(listOf(
        "-filter_complex", filterComplex,
        "-map", "0:v",
        "-map", "[aout]",
        "-c:v", "copy",
        "-c:a", "aac",
        "-b:a", "192k",
        "-t", String.format(java.util.Locale.US, "%.3f", totalDuration),
        outputFile.absolutePath
    ))
    return list.toTypedArray()
}
```

---

## 7. VIDEO RENDER ENGINE (HARDWARE-ACCELERATED PIPELINE)
- Use Android `MediaCodec` (H.264 / AVC Baseline/High Profile) paired with `MediaMuxer` and an OpenGL/Hardware input Surface.
- Pre-scale all input bitmaps to target resolution (e.g. 1080x1920) before encoding to achieve sub-millisecond per-frame draw times.
- Enforce strictly monotonic presentation timestamps:
  ```kotlin
  val pts = outputFrameIndex * (1_000_000L / frameRate)
  bufferInfo.presentationTimeUs = pts
  ```
  This eliminates clock desync and prevents trailing black frames.
