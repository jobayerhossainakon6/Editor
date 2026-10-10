package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.AssSubtitleParser
import com.example.model.AssTemplateInfo
import com.example.model.ImageSegment
import com.example.model.SubtitleSegment
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun VideoPreviewCanvas(
    images: List<ImageSegment>,
    subtitles: List<SubtitleSegment>,
    activeTemplate: AssTemplateInfo?,
    currentPlayTime: Double,
    totalDuration: Double,
    isPlaying: Boolean,
    aspectRatio: String = "16:9",
    selectedSubtitleId: String? = null,
    onSelectSubtitle: (String) -> Unit = {},
    onSelectImage: (String) -> Unit = {},
    onUpdateAspectRatio: (String) -> Unit = {},
    onUpdateSubtitlePosition: (id: String, posX: Float, posY: Float) -> Unit = { _, _, _ -> },
    onTogglePlay: () -> Unit,
    onSeek: (Double) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Active base track (Track 0) image scene and start time at current playhead
    val (activeImage, sceneStartTime) = remember(images, currentPlayTime) {
        val baseClips = images.filter { it.trackIndex == 0 }
        var found: ImageSegment? = null
        var foundStart = 0.0
        var sequentialTime = 0.0

        for (img in baseClips) {
            val start = if (img.startTimeSeconds >= 0.0) img.startTimeSeconds else sequentialTime
            val end = start + img.durationSeconds
            sequentialTime = end
            if (currentPlayTime >= start && currentPlayTime < end) {
                found = img
                foundStart = start
                break
            }
        }
        Pair(found, foundStart)
    }

    // Active subtitles list at current playhead (All active text layers, not just the first one!)
    val activeSubtitles = remember(subtitles, currentPlayTime, selectedSubtitleId, isPlaying) {
        val matching = subtitles.filter { currentPlayTime >= it.startSeconds && currentPlayTime <= it.endSeconds }
        if (!isPlaying && selectedSubtitleId != null && matching.none { it.id == selectedSubtitleId }) {
            val selected = subtitles.find { it.id == selectedSubtitleId }
            if (selected != null) matching + selected else matching
        } else {
            matching
        }
    }

    // Aspect ratio ratio float
    val aspectFloat = when (aspectRatio) {
        "16:9" -> 16f / 9f
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        "21:9" -> 21f / 9f
        else -> 9f / 16f // 9:16 default
    }

    // Calculate time into scene & motion effects (Ken Burns, Fade In, Fade Out)
    val timeIntoScene = (currentPlayTime - sceneStartTime).coerceAtLeast(0.0)
    val sceneDuration = (activeImage?.durationSeconds ?: 3.0).coerceAtLeast(0.5)
    val sceneProgress = (timeIntoScene / sceneDuration).coerceIn(0.0, 1.0).toFloat()

    var imageScale = 1.0f
    var imageAlpha = 1.0f
    var imageOffsetXPx = 0f

    if (activeImage != null) {
        when (activeImage.effect) {
            "FadeIn" -> {
                val t = (timeIntoScene / 0.7).coerceIn(0.0, 1.0).toFloat()
                imageAlpha = t
            }
            "FadeOut" -> {
                val remain = (sceneDuration - timeIntoScene).coerceAtLeast(0.0)
                val t = (remain / 0.7).coerceIn(0.0, 1.0).toFloat()
                imageAlpha = t
            }
            "KenBurnsZoomIn" -> {
                imageScale = 1.0f + sceneProgress * 0.15f
            }
            "KenBurnsZoomOut" -> {
                imageScale = 1.15f - sceneProgress * 0.15f
            }
            "PanLeftToRight" -> {
                imageOffsetXPx = (sceneProgress - 0.5f) * 40f
                imageScale = 1.08f
            }
        }
    }

    // Transition handling: None is instant cut (0ms transition duration); Crossfade is 180ms
    val isCleanCut = activeImage?.transition == "None" || activeImage?.transition == "Cut" || activeImage?.transition.isNullOrEmpty()
    val transitionDurationMs = if (isCleanCut) 0 else 180

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070B14))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aspect Ratio Selector Bar directly above preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Pair("16:9", "16:9 YouTube"),
                    Pair("9:16", "9:16 Shorts"),
                    Pair("1:1", "1:1 Feed"),
                    Pair("4:5", "4:5 Post")
                ).forEach { (ratio, label) ->
                    val isSelected = aspectRatio == ratio
                    Surface(
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onUpdateAspectRatio(ratio) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Timecode badge
            Text(
                text = String.format(
                    Locale.US,
                    "%02d:%04.1f / %02d:%04.1f",
                    (currentPlayTime / 60).toInt(),
                    currentPlayTime % 60,
                    (totalDuration / 60).toInt(),
                    totalDuration % 60
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFBBF24),
                fontFamily = FontFamily.Monospace
            )
        }

        // Compact Preview Viewport (Seamless edge-to-edge video canvas, zero black frames!)
        Box(
            modifier = Modifier
                .height(185.dp)
                .aspectRatio(aspectFloat)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    activeImage?.let { onSelectImage(it.id) }
                    onTogglePlay()
                }
                .testTag("preview_canvas"),
            contentAlignment = Alignment.Center
        ) {
            // Gapless Image Crossfade (or 0ms Clean Cut if transition is None)
            val context = LocalContext.current
            Crossfade(
                targetState = activeImage?.localPath,
                animationSpec = tween(durationMillis = transitionDurationMs),
                label = "smooth_image_transition"
            ) { path ->
                val resolvedFile = remember(path, images) {
                    if (path != null) {
                        val f = File(path)
                        if (f.exists() && f.length() > 0L) return@remember f
                        val inPersist = File(context.filesDir, "persistent_media/" + f.name)
                        if (inPersist.exists() && inPersist.length() > 0L) return@remember inPersist
                        val inMedia = File(context.filesDir, "media/" + f.name)
                        if (inMedia.exists() && inMedia.length() > 0L) return@remember inMedia
                        null
                    } else {
                        null
                    }
                }

                if (resolvedFile != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = resolvedFile,
                            contentDescription = activeImage?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(imageScale)
                                .alpha(imageAlpha)
                                .offset(x = imageOffsetXPx.dp)
                        )

                        // LIVE SCENE VIDEO FILTER OVERLAY IN PREVIEW (Only applied over actual image!)
                        if (activeImage != null && activeImage.effect != "Normal" && activeImage.effect != "FadeIn" && activeImage.effect != "FadeOut") {
                            when (activeImage.effect) {
                                "FilmGrain" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = 0.08f))
                                    )
                                }
                                "CinematicWarm" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFD97706).copy(alpha = 0.12f))
                                    )
                                }
                                "TealOrange" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF06B6D4).copy(alpha = 0.10f), Color(0xFFF97316).copy(alpha = 0.10f))
                                                )
                                            )
                                    )
                                }
                                "Cyberpunk" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF00FFFF).copy(alpha = 0.12f), Color(0xFFEC4899).copy(alpha = 0.12f))
                                                )
                                            )
                                    )
                                }
                                "VintageVHS" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.10f))
                                    )
                                }
                                "BlackWhite" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.40f))
                                    )
                                }
                            }
                        }

                        // LIVE CLIP-TO-CLIP TRANSITION PREVIEW
                        val activeIndex = images.indexOf(activeImage)
                        val nextImage = if (activeIndex in 0 until images.lastIndex) images[activeIndex + 1] else null
                        val timeRemaining = sceneDuration - timeIntoScene
                        val transitionDurationSec = 0.35
                        val hasTransition = activeImage != null && activeImage.transition != "None" && activeImage.transition != "Cut" && nextImage != null
                        val inTransition = hasTransition && timeRemaining < transitionDurationSec
                        val transProgress = if (inTransition) (1.0f - (timeRemaining / transitionDurationSec).toFloat()).coerceIn(0f, 1f) else 0f

                        if (inTransition && nextImage != null) {
                            val nextF = File(nextImage.localPath)
                            val validNext = if (nextF.exists() && nextF.length() > 0L) nextF else null
                            if (validNext != null) {
                                when (activeImage.transition) {
                                    "Crossfade", "Dissolve" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().alpha(transProgress)
                                        )
                                    }
                                    "Dip to Black" -> {
                                        val blackAlpha = if (transProgress < 0.5f) transProgress * 2f else (1f - transProgress) * 2f
                                        if (transProgress >= 0.5f) {
                                            AsyncImage(
                                                model = validNext,
                                                contentDescription = nextImage.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = blackAlpha)))
                                    }
                                    "Dip to White", "Blur Flash" -> {
                                        val whiteAlpha = if (transProgress < 0.5f) transProgress * 2f else (1f - transProgress) * 2f
                                        if (transProgress >= 0.5f) {
                                            AsyncImage(
                                                model = validNext,
                                                contentDescription = nextImage.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = whiteAlpha)))
                                    }
                                    "Slide Left" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().offset(x = ((1f - transProgress) * 260).dp)
                                        )
                                    }
                                    "Slide Right" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().offset(x = (-(1f - transProgress) * 260).dp)
                                        )
                                    }
                                    "Push Up" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().offset(y = ((1f - transProgress) * 180).dp)
                                        )
                                    }
                                    "Push Down" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().offset(y = (-(1f - transProgress) * 180).dp)
                                        )
                                    }
                                    "Zoom In" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().scale(0.85f + transProgress * 0.15f).alpha(transProgress)
                                        )
                                    }
                                    "Zoom Out" -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().scale(1.15f - transProgress * 0.15f).alpha(transProgress)
                                        )
                                    }
                                    else -> {
                                        AsyncImage(
                                            model = validNext,
                                            contentDescription = nextImage.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().alpha(transProgress)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty timeline gap / no image or video at current playhead:
                    // Render clean black frame as requested (no placeholder icons or scene text)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }

            // MULTI-LAYER OVERLAY MEDIA TRACKS (Higher layer_index clips overlay lower ones)
            val overlayImages = remember(images, currentPlayTime, sceneStartTime) {
                images.filter { img ->
                    img.trackIndex > 0 && run {
                        val start = if (img.startTimeSeconds >= 0.0) img.startTimeSeconds else sceneStartTime
                        val end = start + img.durationSeconds
                        currentPlayTime >= start && currentPlayTime <= end
                    }
                }.sortedBy { it.trackIndex }
            }

            for (ovImg in overlayImages) {
                val ovFile = remember(ovImg.localPath) {
                    val f = File(ovImg.localPath)
                    if (f.exists() && f.length() > 0L) f else null
                }
                if (ovFile != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                onSelectImage(ovImg.id)
                            }
                    ) {
                        AsyncImage(
                            model = ovFile,
                            contentDescription = ovImg.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // DYNAMICALLY ANIMATED TEXT OVERLAYS (All active subtitles rendered, draggable, with stroke + fill)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidthPx = constraints.maxWidth.toFloat()
                val boxHeightPx = constraints.maxHeight.toFloat()

                for (activeSubtitle in activeSubtitles) {
                    val isSelected = selectedSubtitleId == activeSubtitle.id
                    val rawText = remember(activeSubtitle.text) {
                        AssSubtitleParser.stripAssTags(activeSubtitle.text)
                    }
                    val textColor = remember(activeSubtitle.textColorHex) {
                        parseAssColor(activeSubtitle.textColorHex)
                    }
                    val outlineColor = remember(activeSubtitle.outlineColorHex) {
                        parseAssColor(activeSubtitle.outlineColorHex)
                    }

                    // TIME PROGRESS WITHIN ACTIVE SUBTITLE
                    val timeIntoSub = (currentPlayTime - activeSubtitle.startSeconds).coerceAtLeast(0.0)
                    val subDuration = (activeSubtitle.endSeconds - activeSubtitle.startSeconds).coerceAtLeast(0.1)
                    val progress = (timeIntoSub / subDuration).coerceIn(0.0, 1.0).toFloat()

                    // DYNAMIC ANIMATION ENGINE (POP-UP, BOUNCE, TYPEWRITER, GLOW, SLIDE-UP)
                    var animScale = 1.0f
                    var animOffsetY = 0f
                    var animAlpha = 1.0f
                    var animRotate = 0f
                    var displayText = rawText

                    when (activeSubtitle.animationPreset) {
                        "Pop-up", "Viral Hook Pop" -> {
                            val t = (timeIntoSub / 0.25).coerceIn(0.0, 1.0).toFloat()
                            animScale = if (t < 0.6f) 0.35f + (t / 0.6f) * 0.8f else 1.15f - ((t - 0.6f) / 0.4f) * 0.15f
                        }
                        "Bounce", "Gaming Victory Bounce" -> {
                            val t = (timeIntoSub / 0.4).coerceIn(0.0, 1.0).toFloat()
                            animOffsetY = -sin(t * Math.PI.toFloat()) * 18f
                            animScale = 1.0f + sin(t * Math.PI.toFloat()) * 0.12f
                        }
                        "Word-Glow", "Karaoke" -> {
                            val pulse = (sin(timeIntoSub * 8.0) * 0.08 + 1.0).toFloat()
                            animScale = pulse
                        }
                        "Typewriter" -> {
                            val charsCount = (progress * rawText.length * 1.6f).toInt().coerceIn(1, rawText.length)
                            displayText = rawText.take(charsCount)
                        }
                        "Slide-Up" -> {
                            val t = (timeIntoSub / 0.3).coerceIn(0.0, 1.0).toFloat()
                            animOffsetY = (1f - t) * 35f
                            animAlpha = t
                        }
                        "Fade-In" -> {
                            val t = (timeIntoSub / 0.3).coerceIn(0.0, 1.0).toFloat()
                            animAlpha = t
                        }
                        "Zoom-Punch", "MrBeast Fast Punch" -> {
                            val t = (timeIntoSub / 0.2).coerceIn(0.0, 1.0).toFloat()
                            animScale = 1.4f - t * 0.4f
                        }
                        "Glitch Flicker" -> {
                            val jitter = (sin(timeIntoSub * 24.0) * 4f).toFloat()
                            animOffsetY = jitter
                            animRotate = if (timeIntoSub % 0.2 < 0.08) 2f else 0f
                        }
                    }

                    // EFFECT SHADOW & GLOW
                    val textShadow = when (activeSubtitle.textEffect) {
                        "Neon", "Cyberpunk Neon Cyan" -> Shadow(color = Color(0xFF00FFFF), offset = Offset(0f, 0f), blurRadius = 14f)
                        "Glow", "Hormozi Gold Pop" -> Shadow(color = textColor, offset = Offset(0f, 0f), blurRadius = 12f)
                        "3D Stroke", "Shadow", "MrBeast Comic Punch" -> Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 4f)
                        else -> Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(2f, 2f), blurRadius = 3f)
                    }

                    val posX = activeSubtitle.posX.coerceIn(0.05f, 0.95f) * boxWidthPx
                    val posY = activeSubtitle.posY.coerceIn(0.05f, 0.95f) * boxHeightPx

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (posX - 70).roundToInt().coerceIn(0, (boxWidthPx - 100).toInt().coerceAtLeast(0)),
                                    (posY - 20 + animOffsetY).roundToInt().coerceIn(0, (boxHeightPx - 40).toInt().coerceAtLeast(0))
                                )
                            }
                            .scale(animScale)
                            .rotate(animRotate)
                            .pointerInput(activeSubtitle.id, boxWidthPx, boxHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        onSelectSubtitle(activeSubtitle.id)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newPosX = ((activeSubtitle.posX * boxWidthPx) + dragAmount.x) / boxWidthPx
                                        val newPosY = ((activeSubtitle.posY * boxHeightPx) + dragAmount.y) / boxHeightPx
                                        onUpdateSubtitlePosition(
                                            activeSubtitle.id,
                                            newPosX.coerceIn(0.05f, 0.95f),
                                            newPosY.coerceIn(0.05f, 0.95f)
                                        )
                                    }
                                )
                            }
                            .clickable {
                                onSelectSubtitle(activeSubtitle.id)
                            }
                    ) {
                        val computedFontSize = (activeSubtitle.fontSize * 0.38f).coerceIn(12f, 32f).sp
                        val resolvedFontFamily = when (activeSubtitle.fontName.lowercase()) {
                            "cinzel", "playfair display", "abril fatface", "prata", "serif" -> FontFamily.Serif
                            "courier", "press start 2p", "space mono", "vt323", "monospace" -> FontFamily.Monospace
                            "caveat", "pacifico", "lobster", "script", "dancing script", "kalam" -> FontFamily.Cursive
                            else -> FontFamily.SansSerif
                        }

                        // Background Pill Box (if enabled)
                        Box(
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF00FFFF) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(
                                    if (activeSubtitle.bgBoxColorHex != null) Color.Black.copy(alpha = 0.85f)
                                    else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            // Layer 1: Black Stroke Outline
                            if (activeSubtitle.outlineWidth > 0f) {
                                Text(
                                    text = displayText,
                                    color = outlineColor,
                                    fontSize = computedFontSize,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontFamily = resolvedFontFamily,
                                        drawStyle = Stroke(
                                            width = (activeSubtitle.outlineWidth * 0.8f).coerceAtLeast(1.5f),
                                            join = StrokeJoin.Round
                                        )
                                    )
                                )
                            }

                            // Layer 2: Text Fill (White or custom color)
                            Text(
                                text = displayText,
                                color = textColor.copy(alpha = animAlpha),
                                fontSize = computedFontSize,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    shadow = textShadow,
                                    fontFamily = resolvedFontFamily,
                                    drawStyle = Fill
                                )
                            )
                        }

                        // Floating CapCut Edit Icon Badge if selected
                        if (isSelected) {
                            Surface(
                                color = Color(0xFF00FFFF),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = Color.Black,
                                    modifier = Modifier.padding(2.5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Play / Pause Indicator Icon
            if (!isPlaying) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Compact Scrubber Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(30.dp).testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Slider(
                value = currentPlayTime.toFloat(),
                onValueChange = { onSeek(it.toDouble()) },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0.1f),
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .testTag("timeline_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            IconButton(
                onClick = onRestart,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun parseAssColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("&H").removePrefix("#")
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else if (clean.length == 8) {
            val r = clean.substring(6, 8).toInt(16)
            val g = clean.substring(4, 6).toInt(16)
            val b = clean.substring(2, 4).toInt(16)
            Color(r, g, b)
        } else {
            Color(0xFFFFFF00)
        }
    } catch (_: Exception) {
        Color(0xFFFFFF00)
    }
}
