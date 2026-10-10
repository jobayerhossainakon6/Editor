package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.ExportSettings
import com.example.model.RenderState
import java.io.File

@Composable
fun ExportRenderScreen(
    renderState: RenderState,
    totalImages: Int,
    totalSubtitles: Int,
    onUpdateExportSettings: (ExportSettings) -> Unit,
    onStartRender: () -> Unit,
    onSaveToGallery: () -> Unit,
    onExportZip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val terminalListState = rememberLazyListState()

    var selectedResolution by remember(renderState.exportSettings.resolution) {
        mutableStateOf(renderState.exportSettings.resolution)
    }
    var selectedFps by remember(renderState.exportSettings.frameRate) {
        mutableStateOf(renderState.exportSettings.frameRate)
    }
    var selectedRatio by remember(renderState.exportSettings.aspectRatio) {
        mutableStateOf(renderState.exportSettings.aspectRatio)
    }
    var selectedFraming by remember(renderState.exportSettings.framingMode) {
        mutableStateOf(renderState.exportSettings.framingMode)
    }

    LaunchedEffect(renderState.logs.size) {
        if (renderState.logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(renderState.logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Pro Studio Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pro 2-Pass Export Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "4K / 2K / 1080p · Samsung A53 Guaranteed",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$selectedResolution · $selectedRatio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Resolution Selector Chips (4K, 2K, 1080p, 720p)
                Text(
                    text = "Resolution (রেজোলিউশন নির্বাচন করুন)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("4K", "4K Ultra HD (2160p)"),
                        Pair("2K", "2K Quad HD (1440p)"),
                        Pair("1080p", "1080p Full HD"),
                        Pair("720p", "720p HD")
                    ).forEach { (res, label) ->
                        FilterChip(
                            selected = selectedResolution == res,
                            onClick = {
                                selectedResolution = res
                                onUpdateExportSettings(renderState.exportSettings.copy(resolution = res))
                            },
                            label = { Text(if (res == "4K" || res == "2K") res else res.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Aspect Ratio Selector (YouTube 16:9, Shorts 9:16, Feed 1:1)
                Text(
                    text = "Video Format (ভিডিও প্ল্যাটফর্ম ও রেশিও)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair("16:9", "16:9 YouTube"),
                        Pair("9:16", "9:16 Shorts/Reels"),
                        Pair("1:1", "1:1 Square"),
                        Pair("4:5", "4:5 Post")
                    ).forEach { (ratio, label) ->
                        FilterChip(
                            selected = selectedRatio == ratio,
                            onClick = {
                                selectedRatio = ratio
                                onUpdateExportSettings(renderState.exportSettings.copy(aspectRatio = ratio))
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4F46E5),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Framing Mode (Fixes 16:9 portrait image distortion!)
                Text(
                    text = "Framing Mode (ইউটিউবে ছবি ফিট করার নিয়ম)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Pair("CropToFill", "Crop to Fill (স্ক্রিন জুড়ে পূর্ণ)"),
                        Pair("FitBlurred", "Fit with Blur (ব্লার ব্যাকগ্রাউন্ড)")
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = selectedFraming == mode,
                            onClick = {
                                selectedFraming = mode
                                onUpdateExportSettings(renderState.exportSettings.copy(framingMode = mode))
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF065F46),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Frame Rate (30 FPS / 60 FPS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Frame Rate: ${selectedFps} FPS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 60).forEach { fps ->
                            FilterChip(
                                selected = selectedFps == fps,
                                onClick = {
                                    selectedFps = fps
                                    onUpdateExportSettings(renderState.exportSettings.copy(frameRate = fps))
                                },
                                label = { Text("$fps FPS", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Glowing CTA: Render 2-Pass Video
        Button(
            onClick = onStartRender,
            enabled = !renderState.isRendering,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0284C7),
                disabledContainerColor = Color(0xFF0284C7).copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_render_button")
        ) {
            if (renderState.isRendering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Rendering $selectedResolution Video (${renderState.progressPercent}%)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export 2-Pass ($selectedResolution $selectedRatio)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Rendering Progress Card
        if (renderState.isRendering || renderState.progressPercent > 0) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (renderState.currentPass == 1) "PASS 1/2: Image Framing & Zero-Black Concat" else "PASS 2/2: Subtitles & Gapless H.264",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "${renderState.progressPercent}%",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { renderState.progressPercent / 100f },
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF334155),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = renderState.currentStepDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Success Action Banner
        if (renderState.isSuccess && renderState.outputVideoPath != null) {
            val videoFile = File(renderState.outputVideoPath)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ভিডিও সফলভাবে রেন্ডার হয়েছে!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                            Text(
                                text = "$selectedResolution $selectedRatio · Samsung Gallery Ready",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSaveToGallery,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("save_gallery_button")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("গ্যালারিতে সেভ", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_SUBJECT, "Rendered Faceless Video")
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            videoFile
                                        )
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    } catch (_: Exception) {}
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("share_video_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Video", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Live FFmpeg Terminal Console
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FFmpeg Console & Gapless Engine Logs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "${renderState.logs.size} lines",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF030712))
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                state = terminalListState,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(renderState.logs) { line ->
                    val color = when {
                        line.startsWith("[PASS") -> Color(0xFF38BDF8)
                        line.startsWith("[SUCCESS") -> Color(0xFF34D399)
                        line.startsWith("[ERROR") -> Color(0xFFF87171)
                        line.startsWith(">>") -> Color(0xFFFBBF24)
                        else -> Color(0xFF94A3B8)
                    }
                    Text(
                        text = line,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
