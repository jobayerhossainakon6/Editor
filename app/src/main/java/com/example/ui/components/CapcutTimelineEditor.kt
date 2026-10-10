package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Divider
import coil.compose.AsyncImage
import com.example.model.ImageSegment
import com.example.model.SubtitleSegment
import java.io.File
import java.util.Locale

enum class TimelineTrackType {
    VIDEO,
    TEXT,
    AUDIO
}

@Composable
fun CapcutTimelineEditor(
    images: List<ImageSegment>,
    subtitles: List<SubtitleSegment>,
    audios: List<com.example.model.AudioSegment> = emptyList(),
    selectedImageId: String?,
    selectedSubtitleId: String? = null,
    selectedAudioId: String? = null,
    currentPlayTime: Double,
    totalDuration: Double,
    isPlaying: Boolean = false,
    zoomScale: Float = 1.0f,
    onUpdateZoomScale: (Float) -> Unit = {},
    onSelectImage: (String) -> Unit,
    onSelectSubtitle: (String) -> Unit = {},
    onSelectAudio: (String) -> Unit = {},
    onAddSubtitle: () -> Unit = {},
    onUpdateSubtitleSegment: (SubtitleSegment) -> Unit = {},
    onAddAudio: () -> Unit = {},
    onAddAudioUri: (Uri) -> Unit = {},
    onAdjustDuration: (id: String, delta: Double) -> Unit,
    onAdjustImageDurationWithDrag: (id: String, newDuration: Double) -> Unit = { _, _ -> },
    onAdjustSubtitleTiming: (id: String, newStart: Double, newEnd: Double) -> Unit = { _, _, _ -> },
    onSplitImage: (id: String) -> Unit = {},
    onSplitSubtitle: (id: String) -> Unit = {},
    onSplitAudio: (id: String) -> Unit = {},
    onDuplicateAudio: (id: String) -> Unit = {},
    onUpdateAudioVolume: (id: String, volume: Float) -> Unit = { _, _ -> },
    onUpdateAudioAutomation: (id: String, fadeIn: Double, fadeOut: Double, ducking: Boolean, duckVol: Float) -> Unit = { _, _, _, _, _ -> },
    onUpdateAudioLayerType: (id: String, layerType: String) -> Unit = { _, _ -> },
    onAddVoiceoverTrack: () -> Unit = {},
    onAddSfxTrack: (String) -> Unit = {},
    onToggleAudioMute: (id: String) -> Unit = {},
    onAddAudioSampleNamed: (name: String) -> Unit = {},
    onMoveImage: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onUpdateImageLayer: (id: String, layerIndex: Int) -> Unit = { _, _ -> },
    onUpdateImageTimingAndLayer: (id: String, newStartTime: Double, layerIndex: Int) -> Unit = { _, _, _ -> },
    onUpdateSubtitleTimingAndLayer: (id: String, newStartTime: Double, layerIndex: Int) -> Unit = { _, _, _ -> },
    onAddSubtitleTrack: () -> Unit = {},
    onUpdateAudioTimingAndLayer: (id: String, newStartTime: Double, layerIndex: Int) -> Unit = { _, _, _ -> },
    onAddAudioTrack: () -> Unit = {},
    onUpdateTransition: (id: String, transition: String) -> Unit,
    onDuplicateImage: (id: String) -> Unit,
    onDeleteImage: (id: String) -> Unit,
    onDeleteSubtitle: (id: String) -> Unit = {},
    onDeleteAudio: (id: String) -> Unit = {},
    onAddMultipleImages: (List<Uri>) -> Unit,
    onSeek: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var isDraggingPlayhead by remember { mutableStateOf(false) }
    var dragPlayheadTime by remember { mutableDoubleStateOf(currentPlayTime) }

    // 1. Gallery Multiple Media Picker (Images & Videos)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddMultipleImages(uris)
        }
    }

    // 2. Storage / File Manager Multiple Images Picker
    val filesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddMultipleImages(uris)
        }
    }

    // 3. Audio File Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAddAudioUri(uri)
        }
    }

    var activeTrackType by remember { mutableStateOf(TimelineTrackType.VIDEO) }
    var showTextEditDialog by remember { mutableStateOf(false) }
    var editingSubtitleId by remember { mutableStateOf("") }
    var editingSubtitleText by remember { mutableStateOf("") }

    LaunchedEffect(selectedAudioId, selectedSubtitleId, selectedImageId) {
        if (!selectedAudioId.isNullOrEmpty()) {
            activeTrackType = TimelineTrackType.AUDIO
        } else if (!selectedSubtitleId.isNullOrEmpty()) {
            activeTrackType = TimelineTrackType.TEXT
        } else if (!selectedImageId.isNullOrEmpty()) {
            activeTrackType = TimelineTrackType.VIDEO
        }
    }

    val selectedSegment = if (activeTrackType == TimelineTrackType.VIDEO) images.find { it.id == selectedImageId } ?: images.firstOrNull() else null
    val selectedSub = if (activeTrackType == TimelineTrackType.TEXT) subtitles.find { it.id == selectedSubtitleId } ?: subtitles.firstOrNull() else null
    val selectedAud = if (activeTrackType == TimelineTrackType.AUDIO) audios.find { it.id == selectedAudioId } ?: audios.firstOrNull() else null

    var showAudioMenu by remember { mutableStateOf(false) }
    var showAudioAutomationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(vertical = 6.dp)
    ) {
        // Track Mode Selector Tabs (Video Track, Text Track, Audio Track)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Video Track Tab
            Surface(
                color = if (activeTrackType == TimelineTrackType.VIDEO) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp),
                border = if (activeTrackType == TimelineTrackType.VIDEO) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        activeTrackType = TimelineTrackType.VIDEO
                        onSelectSubtitle("")
                        onSelectAudio("")
                    }
                    .testTag("track_tab_video")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Video (${images.size})",
                        fontSize = 10.5.sp,
                        fontWeight = if (activeTrackType == TimelineTrackType.VIDEO) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // 2. Text Track Tab
            Surface(
                color = if (activeTrackType == TimelineTrackType.TEXT) Color(0xFF7C3AED) else Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp),
                border = if (activeTrackType == TimelineTrackType.TEXT) BorderStroke(1.5.dp, Color(0xFFC084FC)) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        activeTrackType = TimelineTrackType.TEXT
                        onSelectImage("")
                        onSelectAudio("")
                    }
                    .testTag("track_tab_text")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Text (${subtitles.size})",
                        fontSize = 10.5.sp,
                        fontWeight = if (activeTrackType == TimelineTrackType.TEXT) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // 3. Audio Track Tab
            Surface(
                color = if (activeTrackType == TimelineTrackType.AUDIO) Color(0xFF059669) else Color(0xFF1E293B),
                shape = RoundedCornerShape(6.dp),
                border = if (activeTrackType == TimelineTrackType.AUDIO) BorderStroke(1.5.dp, Color(0xFF34D399)) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        activeTrackType = TimelineTrackType.AUDIO
                        onSelectImage("")
                        onSelectSubtitle("")
                    }
                    .testTag("track_tab_audio")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Audio (${audios.size})",
                        fontSize = 10.5.sp,
                        fontWeight = if (activeTrackType == TimelineTrackType.AUDIO) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Contextual Toolbar based on activeTrackType
        when (activeTrackType) {
            TimelineTrackType.VIDEO -> {
                // Video Track Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Clip controls (or split at playhead)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = {
                                if (selectedSegment != null) onSplitImage(selectedSegment.id) else onSplitImage("")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = "Split Scene", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Split", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (selectedSegment != null) {
                            val idx = images.indexOf(selectedSegment)
                            IconButton(
                                onClick = { if (idx > 0) onMoveImage(idx, idx - 1) },
                                enabled = idx > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("◀", fontSize = 11.sp, color = if (idx > 0) Color.White else Color(0xFF475569))
                            }
                            IconButton(
                                onClick = { if (idx < images.lastIndex) onMoveImage(idx, idx + 1) },
                                enabled = idx < images.lastIndex,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("▶", fontSize = 11.sp, color = if (idx < images.lastIndex) Color.White else Color(0xFF475569))
                            }

                            FilledTonalButton(
                                onClick = { onAdjustDuration(selectedSegment.id, -0.5) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("-0.5s", fontSize = 10.sp)
                            }

                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text(
                                        text = String.format(Locale.US, "%.1fs", selectedSegment.durationSeconds),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { onAdjustDuration(selectedSegment.id, +0.5) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("+0.5s", fontSize = 10.sp)
                            }

                            IconButton(
                                onClick = { onDuplicateImage(selectedSegment.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(15.dp))
                            }

                            IconButton(
                                onClick = { onDeleteImage(selectedSegment.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    // Right: Media Add Buttons (Images, Videos, Media Hub)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                try {
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } catch (_: Exception) {
                                    showImportDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp).testTag("add_image_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Image", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                } catch (_: Exception) {
                                    showImportDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp).testTag("add_video_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Video", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.size(30.dp).testTag("open_import_dialog_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Media Hub", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            TimelineTrackType.TEXT -> {
                // Text Track Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Text Editing Controls
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = {
                                if (selectedSub != null) onSplitSubtitle(selectedSub.id) else onSplitSubtitle("")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = "Split Text", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Split", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val targetSub = selectedSub ?: subtitles.find {
                                    currentPlayTime >= it.startSeconds && currentPlayTime <= it.endSeconds
                                } ?: subtitles.firstOrNull()
                                if (targetSub != null) {
                                    editingSubtitleId = targetSub.id
                                    editingSubtitleText = targetSub.text
                                    showTextEditDialog = true
                                } else {
                                    onAddSubtitle()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp).testTag("edit_text_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Edit Text", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (selectedSub != null) {
                            Surface(
                                color = Color(0xFF312E81),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text(
                                        text = String.format(Locale.US, "T%d: %.1fs", selectedSub.trackIndex + 1, selectedSub.endSeconds - selectedSub.startSeconds),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val newLayer = (selectedSub.trackIndex - 1).coerceAtLeast(0)
                                    onUpdateSubtitleTimingAndLayer(selectedSub.id, selectedSub.startSeconds, newLayer)
                                },
                                enabled = selectedSub.trackIndex > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("▲", fontSize = 10.sp, color = if (selectedSub.trackIndex > 0) Color.White else Color(0xFF475569))
                            }

                            IconButton(
                                onClick = {
                                    val newLayer = selectedSub.trackIndex + 1
                                    onUpdateSubtitleTimingAndLayer(selectedSub.id, selectedSub.startSeconds, newLayer)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("▼", fontSize = 10.sp, color = Color.White)
                            }

                            IconButton(
                                onClick = { onDeleteSubtitle(selectedSub.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Text", tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Right: Add Text & Add Text Track
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onAddSubtitle,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp).testTag("add_subtitle_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Add Text", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onAddSubtitleTrack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("+ Track", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            TimelineTrackType.AUDIO -> {
                // Audio Track Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Audio Controls (if audio selected)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = {
                                if (selectedAud != null) onSplitAudio(selectedAud.id) else onSplitAudio("")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = "Split Audio", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Split", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (selectedAud != null) {
                            IconButton(
                                onClick = { onToggleAudioMute(selectedAud.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(if (selectedAud.isMuted) "🔇" else "🔊", fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val nextVol = when {
                                        selectedAud.volume < 0.5f -> 0.8f
                                        selectedAud.volume < 0.9f -> 1.0f
                                        else -> 0.4f
                                    }
                                    onUpdateAudioVolume(selectedAud.id, nextVol)
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("${(selectedAud.volume * 100).toInt()}% Vol", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showAudioAutomationDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).testTag("audio_automation_button"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("🎛️ Auto", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }

                            IconButton(
                                onClick = { onDuplicateAudio(selectedAud.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Audio", tint = Color.White, modifier = Modifier.size(14.dp))
                            }

                            IconButton(
                                onClick = { onDeleteAudio(selectedAud.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Audio", tint = Color(0xFFF87171), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Right: Add Audio Buttons (Presets dropdown, direct storage file picker, add track)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Button(
                                onClick = { showAudioMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp).testTag("add_audio_button"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Audio ▾", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = showAudioMenu,
                                onDismissRequest = { showAudioMenu = false },
                                modifier = Modifier.background(Color(0xFF0F172A))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📁 Choose Audio from Storage...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showAudioMenu = false
                                        try {
                                            audioPickerLauncher.launch(arrayOf("audio/*"))
                                        } catch (_: Exception) {
                                            onAddAudio()
                                        }
                                    }
                                )
                                Divider(color = Color(0xFF334155))
                                DropdownMenuItem(
                                    text = { Text("🎙️ Add Voiceover Track (Auto-Duck)", color = Color(0xFFF472B6), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddVoiceoverTrack()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💥 Add Impact SFX (Layer 2 Punch)", color = Color(0xFFFB923C), fontSize = 11.sp) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddSfxTrack("Cinematic Impact Punch")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💨 Add Whoosh SFX (Layer 2 Swoosh)", color = Color(0xFF38BDF8), fontSize = 11.sp) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddSfxTrack("Dynamic Transition Whoosh")
                                    }
                                )
                                Divider(color = Color(0xFF334155))
                                DropdownMenuItem(
                                    text = { Text("🎧 Add Lo-Fi Study Chill (15s)", color = Color(0xFF34D399), fontSize = 11.sp) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddAudioSampleNamed("Lo-Fi Study Chill")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚡ Add Cyberpunk Synth Beat (15s)", color = Color(0xFF38BDF8), fontSize = 11.sp) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddAudioSampleNamed("Cyberpunk Synth Beat")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎬 Add Epic Cinematic Theme (15s)", color = Color(0xFFFBBF24), fontSize = 11.sp) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddAudioSampleNamed("Epic Cinematic Theme")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("✨ Add Upbeat Vlog Funk (15s)", color = Color(0xFFA855F7), fontSize = 11.sp) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddAudioSampleNamed("Upbeat Vlog Funk")
                                    }
                                )
                                Divider(color = Color(0xFF334155))
                                DropdownMenuItem(
                                    text = { Text("➕ Add New Audio Track Lane", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showAudioMenu = false
                                        onAddAudioTrack()
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                try {
                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                } catch (_: Exception) {
                                    onAddAudio()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp).testTag("import_audio_file_button"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("File", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onAddAudioTrack,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                        ) {
                            Text("+ Track", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Timeline Zoom Slider Bar (Compress from 0.1x up to 3.0x)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🔍 Zoom:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Surface(
                    color = if (zoomScale <= 0.15f) Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onUpdateZoomScale(0.1f) }
                ) {
                    Text("0.1x (Fit)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Surface(
                    color = if (zoomScale in 0.45f..0.55f) Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onUpdateZoomScale(0.5f) }
                ) {
                    Text("0.5x", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Surface(
                    color = if (zoomScale in 0.95f..1.05f) Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onUpdateZoomScale(1.0f) }
                ) {
                    Text("1.0x", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Surface(
                    color = if (zoomScale >= 1.9f) Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { onUpdateZoomScale(2.0f) }
                ) {
                    Text("2.0x", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(155.dp)) {
                Slider(
                    value = zoomScale,
                    onValueChange = { onUpdateZoomScale(it) },
                    valueRange = 0.1f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .testTag("timeline_zoom_slider")
                )
                Text(
                    text = String.format(Locale.US, "%.1fx", zoomScale),
                    fontSize = 9.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Horizontal Multi-Track Timeline Container
        val baseWidth = 50.0
        val density = LocalDensity.current
        val scrollState = rememberScrollState()

        LaunchedEffect(currentPlayTime, isPlaying) {
            if (isPlaying) {
                val playheadPx = with(density) { (currentPlayTime * baseWidth * zoomScale).dp.toPx() }.toInt()
                val viewportWidth = scrollState.viewportSize
                if (viewportWidth > 0) {
                    val targetScroll = (playheadPx - viewportWidth / 2).coerceIn(0, scrollState.maxValue)
                    if (Math.abs(scrollState.value - targetScroll) > viewportWidth * 0.4) {
                        scrollState.animateScrollTo(targetScroll)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF070B14))
                .border(1.dp, Color(0xFF1E293B))
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    // Time Ruler at the top (Touch or drag anywhere along the ruler to seek smoothly)
                    val stepSeconds = if (zoomScale < 0.25f) 10 else if (zoomScale < 0.6f) 5 else if (zoomScale < 1.4f) 2 else 1
                    Row(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(bottom = 2.dp)
                            .pointerInput(totalDuration, zoomScale, density) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()
                                        if (pxPerSec > 0f) {
                                            val rawSec = (offset.x / pxPerSec).toDouble().coerceIn(0.0, totalDuration)
                                            val seekSec = Math.round(rawSec * 100.0) / 100.0
                                            isDraggingPlayhead = true
                                            dragPlayheadTime = seekSec
                                            onSeek(seekSec)
                                        }
                                    },
                                    onDragEnd = {
                                        isDraggingPlayhead = false
                                        onSeek(dragPlayheadTime)
                                    },
                                    onDragCancel = {
                                        isDraggingPlayhead = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()
                                        if (pxPerSec > 0f) {
                                            val deltaSec = (dragAmount.x / pxPerSec).toDouble()
                                            val newTime = (Math.round((dragPlayheadTime + deltaSec) * 100.0) / 100.0).coerceIn(0.0, totalDuration)
                                            dragPlayheadTime = newTime
                                            onSeek(newTime)
                                        }
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val totalSecInt = totalDuration.toInt().coerceAtLeast(1)
                        for (sec in 0..totalSecInt step stepSeconds) {
                            val tickWidth = (stepSeconds * baseWidth * zoomScale).dp
                            Box(
                                modifier = Modifier
                                    .width(tickWidth)
                                    .clickable {
                                        dragPlayheadTime = sec.toDouble()
                                        onSeek(sec.toDouble())
                                    }
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60),
                                    fontSize = 8.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // TRACK 1+: Dynamic Multi-Layer Media Tracks with Absolute Positioning & Gaps
                    val maxMediaLayer = (images.maxOfOrNull { it.trackIndex } ?: 0).coerceAtLeast(0)
                    val timelineContentWidthDp = ((totalDuration * baseWidth * zoomScale) + 300.0).dp

                    for (layer in 0..maxMediaLayer) {
                        val layerClips = images.filter { it.trackIndex == layer }
                        val trackHeight = if (layer == 0) 68.dp else 52.dp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Surface(
                                color = if (activeTrackType == TimelineTrackType.VIDEO) Color(0xFF0284C7) else Color(0xFF0369A1),
                                shape = RoundedCornerShape(4.dp),
                                border = if (activeTrackType == TimelineTrackType.VIDEO) BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable {
                                        activeTrackType = TimelineTrackType.VIDEO
                                        onSelectSubtitle("")
                                        onSelectAudio("")
                                    }
                            ) {
                                Text(
                                    text = if (layer == 0) "V1" else "V${layer + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Absolute positioned track canvas: allows gaps and free positioning anywhere!
                            Box(
                                modifier = Modifier
                                    .width(timelineContentWidthDp)
                                    .height(trackHeight)
                                    .background(
                                        if (activeTrackType == TimelineTrackType.VIDEO) Color(0xFF0B132B).copy(alpha = 0.65f) else Color(0xFF0B132B).copy(alpha = 0.4f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = if (activeTrackType == TimelineTrackType.VIDEO) 2.dp else 1.dp,
                                        color = if (activeTrackType == TimelineTrackType.VIDEO) Color(0xFF38BDF8) else Color(0xFF1E293B).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        activeTrackType = TimelineTrackType.VIDEO
                                        onSelectSubtitle("")
                                        onSelectAudio("")
                                    }
                            ) {
                                // Fallback placeholder if layer is currently empty
                                if (layerClips.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            "Drop clip here into Layer $layer · Tap to select Video Track",
                                            fontSize = 9.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }

                                var sequentialTime = 0.0
                                layerClips.forEachIndexed { index, img ->
                                    val isSelected = img.id == selectedImageId
                                    val effectiveStartTime = if (img.startTimeSeconds >= 0.0) img.startTimeSeconds else sequentialTime
                                    sequentialTime = effectiveStartTime + img.durationSeconds

                                    val clipWidthDp = ((img.durationSeconds * baseWidth * zoomScale)).coerceAtLeast(24.0).dp
                                    val leftOffsetDp = ((effectiveStartTime * baseWidth * zoomScale)).coerceAtLeast(0.0).dp

                                    var dragAccumX by remember { mutableStateOf(0f) }
                                    var dragAccumY by remember { mutableStateOf(0f) }
                                    var isDragging by remember { mutableStateOf(false) }

                                    Box(
                                        modifier = Modifier
                                            .offset(
                                                x = leftOffsetDp + (dragAccumX / density.density).dp,
                                                y = (dragAccumY / density.density).dp
                                            )
                                            .width(clipWidthDp)
                                            .height(trackHeight)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (layer == 0) Color(0xFF1E293B) else Color(0xFF312E81)
                                            )
                                            .alpha(if (isDragging) 0.65f else 1.0f)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF38BDF8) else if (layer == 0) Color(0xFF334155) else Color(0xFF6366F1),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .pointerInput(img.id, layer, zoomScale, density) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = true
                                                        onSelectImage(img.id)
                                                    },
                                                    onDragEnd = {
                                                        val thresholdY = 45f
                                                        val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()
                                                        val deltaTimeSec = if (pxPerSec > 0f) (dragAccumX / pxPerSec).toDouble() else 0.0
                                                        val newStartTime = (effectiveStartTime + deltaTimeSec).coerceAtLeast(0.0)

                                                        val dy = dragAccumY
                                                        val newLayer = when {
                                                            dy > thresholdY -> layer + 1
                                                            dy < -thresholdY && layer > 0 -> layer - 1
                                                            else -> layer
                                                        }

                                                        if (Math.abs(deltaTimeSec) > 0.05 || newLayer != layer) {
                                                            onUpdateImageTimingAndLayer(img.id, newStartTime, newLayer)
                                                        }

                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = false
                                                    },
                                                    onDragCancel = {
                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = false
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumX += dragAmount.x
                                                        dragAccumY += dragAmount.y
                                                    }
                                                )
                                            }
                                            .clickable {
                                                onSelectImage(img.id)
                                                onSeek(effectiveStartTime)
                                            }
                                            .testTag("timeline_clip_${img.id}")
                                    ) {
                                        AsyncImage(
                                            model = File(img.localPath),
                                            contentDescription = img.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )

                                        // Dark overlay
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.35f))
                                        )

                                        // Left Trim Handle (Indicator)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(6.dp)
                                                .height(30.dp)
                                                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color.Transparent)
                                        )

                                        // Right Trim Handle (Indicator)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .width(6.dp)
                                                .height(30.dp)
                                                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color.Transparent)
                                        )

                                        // Title & Duration tag
                                        Column(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = img.title.ifEmpty { "Scene ${index + 1}" },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1fs @ %.1fs · L%d", img.durationSeconds, effectiveStartTime, layer),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFFBBF24)
                                            )
                                        }

                                        // Layer badge on top end
                                        if (layer > 0) {
                                            Surface(
                                                color = Color(0xFF4F46E5),
                                                shape = RoundedCornerShape(3.dp),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                            ) {
                                                Text("PIP", fontSize = 7.sp, color = Color.White, modifier = Modifier.padding(horizontal = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Button to add an overlay media track
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable {
                                val selectedImg = images.find { it.id == selectedImageId } ?: images.lastOrNull()
                                if (selectedImg != null) {
                                    onUpdateImageLayer(selectedImg.id, maxMediaLayer + 1)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+ Add Video Track (Layer ${maxMediaLayer + 1})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // TRACK 2: CapCut Multi-Layer Subtitle & Text Tracks (Absolute positioning, gaps, and vertical drag)
                    val maxTextLayer = (subtitles.maxOfOrNull { it.trackIndex } ?: 0).coerceAtLeast(0)
                    for (textLayer in 0..maxTextLayer) {
                        val layerSubs = subtitles.filter { it.trackIndex == textLayer }
                        val textTrackHeight = 38.dp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Surface(
                                color = if (activeTrackType == TimelineTrackType.TEXT) Color(0xFF7C3AED) else Color(0xFF4338CA),
                                shape = RoundedCornerShape(4.dp),
                                border = if (activeTrackType == TimelineTrackType.TEXT) BorderStroke(1.5.dp, Color(0xFFC084FC)) else null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable {
                                        activeTrackType = TimelineTrackType.TEXT
                                        onSelectImage("")
                                        onSelectAudio("")
                                    }
                            ) {
                                Text(
                                    text = "T${textLayer + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Absolute positioned text track canvas: allows gaps and free positioning anywhere!
                            Box(
                                modifier = Modifier
                                    .width(timelineContentWidthDp)
                                    .height(textTrackHeight)
                                    .background(
                                        if (activeTrackType == TimelineTrackType.TEXT) Color(0xFF1E1B4B).copy(alpha = 0.65f) else Color(0xFF1E1B4B).copy(alpha = 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = if (activeTrackType == TimelineTrackType.TEXT) 2.dp else 1.dp,
                                        color = if (activeTrackType == TimelineTrackType.TEXT) Color(0xFFC084FC) else Color(0xFF312E81).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        activeTrackType = TimelineTrackType.TEXT
                                        onSelectImage("")
                                        onSelectAudio("")
                                        if (layerSubs.isEmpty()) {
                                            onAddSubtitle()
                                        }
                                    }
                            ) {
                                if (layerSubs.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.clickable {
                                                activeTrackType = TimelineTrackType.TEXT
                                                onAddSubtitle()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "Track T${textLayer + 1} (Empty) · Tap to add text caption",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFE9D5FF)
                                            )
                                        }
                                    }
                                }

                                layerSubs.forEach { sub ->
                                    val duration = (sub.endSeconds - sub.startSeconds).coerceAtLeast(0.3)
                                    val subWidthDp = ((duration * baseWidth * zoomScale)).coerceAtLeast(24.0).dp
                                    val leftOffsetDp = ((sub.startSeconds * baseWidth * zoomScale)).coerceAtLeast(0.0).dp
                                    val isSubSelected = sub.id == selectedSubtitleId

                                    var dragAccumX by remember { mutableStateOf(0f) }
                                    var dragAccumY by remember { mutableStateOf(0f) }
                                    var isDragging by remember { mutableStateOf(false) }

                                    val trackStepPx = 42f * density.density
                                    val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()

                                    val currentDeltaTime = if (pxPerSec > 0f) (dragAccumX / pxPerSec).toDouble() else 0.0
                                    val previewStart = (sub.startSeconds + currentDeltaTime).coerceAtLeast(0.0)
                                    val layerDelta = kotlin.math.round(dragAccumY / trackStepPx).toInt()
                                    val targetLayer = (textLayer + layerDelta).coerceAtLeast(0)

                                    Box(
                                        modifier = Modifier
                                            .zIndex(if (isDragging) 99f else 1f)
                                            .offset(
                                                x = leftOffsetDp + (dragAccumX / density.density).dp,
                                                y = (dragAccumY / density.density).dp
                                            )
                                            .width(subWidthDp)
                                            .height(textTrackHeight)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSubSelected) Color(0xFF4338CA) else Color(0xFF1E1B4B)
                                            )
                                            .alpha(if (isDragging) 0.7f else 1.0f)
                                            .border(
                                                width = if (isSubSelected) 2.dp else 1.dp,
                                                color = if (isSubSelected) Color(0xFF00FFFF) else Color(0xFF6366F1),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .pointerInput(sub.id, textLayer, zoomScale, density) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = true
                                                        onSelectSubtitle(sub.id)
                                                    },
                                                    onDragEnd = {
                                                        val finalDeltaTimeSec = if (pxPerSec > 0f) (dragAccumX / pxPerSec).toDouble() else 0.0
                                                        val newStartTime = (sub.startSeconds + finalDeltaTimeSec).coerceAtLeast(0.0)
                                                        val finalLayerDelta = kotlin.math.round(dragAccumY / trackStepPx).toInt()
                                                        val newLayer = (textLayer + finalLayerDelta).coerceAtLeast(0)

                                                        if (Math.abs(finalDeltaTimeSec) > 0.02 || newLayer != textLayer) {
                                                            onUpdateSubtitleTimingAndLayer(sub.id, newStartTime, newLayer)
                                                        }

                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = false
                                                    },
                                                    onDragCancel = {
                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = false
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumX += dragAmount.x
                                                        dragAccumY += dragAmount.y
                                                    }
                                                )
                                            }
                                            .clickable {
                                                onSelectSubtitle(sub.id)
                                                onSeek(sub.startSeconds)
                                            }
                                    ) {
                                        // Floating drag indicator badge showing target layer and time
                                        if (isDragging) {
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = (-18).dp)
                                            ) {
                                                Text(
                                                    text = "T${targetLayer + 1} · ${String.format(Locale.US, "%.1fs", previewStart)}",
                                                    color = if (targetLayer != textLayer) Color(0xFF38BDF8) else Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // Left Trim Handle (Trims startSeconds)
                                        if (isSubSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterStart)
                                                    .width(10.dp)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF00FFFF).copy(alpha = 0.75f), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                                    .pointerInput(sub.id, zoomScale, density) {
                                                        detectDragGestures { change, dragAmount ->
                                                            change.consume()
                                                            val deltaSec = (dragAmount.x / (baseWidth * zoomScale * density.density)).toDouble()
                                                            val newStart = (sub.startSeconds + deltaSec).coerceIn(0.0, sub.endSeconds - 0.3)
                                                            onAdjustSubtitleTiming(sub.id, newStart, sub.endSeconds)
                                                        }
                                                    }
                                            )
                                        }

                                        // Content: text snippet and duration/layer label
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = if (isSubSelected) 12.dp else 6.dp)
                                        ) {
                                            Text(
                                                text = sub.text,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSubSelected) Color(0xFF00FFFF) else Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1fs · T%d", duration, textLayer + 1),
                                                fontSize = 8.sp,
                                                color = Color(0xFFE9D5FF)
                                            )
                                        }

                                        // Right Trim Handle (Trims endSeconds)
                                        if (isSubSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .width(10.dp)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFF00FFFF).copy(alpha = 0.75f), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                                                    .pointerInput(sub.id, zoomScale, density) {
                                                        detectDragGestures { change, dragAmount ->
                                                            change.consume()
                                                            val deltaSec = (dragAmount.x / (baseWidth * zoomScale * density.density)).toDouble()
                                                            val newEnd = (sub.endSeconds + deltaSec).coerceAtLeast(sub.startSeconds + 0.3)
                                                            onAdjustSubtitleTiming(sub.id, sub.startSeconds, newEnd)
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Button to add an overlay text track
                    Surface(
                        color = Color(0xFF1E1B4B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable {
                                onAddSubtitleTrack()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+ Add Text Track (Layer T${maxTextLayer + 2})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // TRACK 3: CapCut Multi-Layer Audio Tracks with Waveform visualization, absolute positioning & gaps
                    val maxAudioLayer = (audios.maxOfOrNull { it.trackIndex } ?: 0).coerceAtLeast(0)
                    for (audioLayer in 0..maxAudioLayer) {
                        val layerAudios = audios.filter { it.trackIndex == audioLayer }
                        val audioTrackHeight = 36.dp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Surface(
                                color = if (activeTrackType == TimelineTrackType.AUDIO) Color(0xFF059669) else Color(0xFF065F46),
                                shape = RoundedCornerShape(4.dp),
                                border = if (activeTrackType == TimelineTrackType.AUDIO) BorderStroke(1.5.dp, Color(0xFF34D399)) else null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable {
                                        activeTrackType = TimelineTrackType.AUDIO
                                        onSelectImage("")
                                        onSelectSubtitle("")
                                    }
                            ) {
                                Text(
                                    text = "A${audioLayer + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // Absolute positioned audio track canvas with gaps and dragging!
                            Box(
                                modifier = Modifier
                                    .width(timelineContentWidthDp)
                                    .height(audioTrackHeight)
                                    .background(
                                        if (activeTrackType == TimelineTrackType.AUDIO) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF064E3B).copy(alpha = 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = if (activeTrackType == TimelineTrackType.AUDIO) 2.dp else 1.dp,
                                        color = if (activeTrackType == TimelineTrackType.AUDIO) Color(0xFF34D399) else Color(0xFF047857).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        activeTrackType = TimelineTrackType.AUDIO
                                        onSelectImage("")
                                        onSelectSubtitle("")
                                        if (layerAudios.isEmpty()) {
                                            showAudioMenu = true
                                        }
                                    }
                            ) {
                                if (layerAudios.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.clickable {
                                                activeTrackType = TimelineTrackType.AUDIO
                                                showAudioMenu = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "Track A${audioLayer + 1} (Empty) · Tap to add music or audio file",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF6EE7B7)
                                            )
                                        }
                                    }
                                }

                                layerAudios.forEach { aud ->
                                    val audWidthDp = ((aud.durationSeconds * baseWidth * zoomScale)).coerceAtLeast(24.0).dp
                                    val leftOffsetDp = ((aud.startSeconds * baseWidth * zoomScale)).coerceAtLeast(0.0).dp
                                    val isAudSelected = aud.id == selectedAudioId

                                    var dragAccumX by remember { mutableStateOf(0f) }
                                    var dragAccumY by remember { mutableStateOf(0f) }
                                    var isDragging by remember { mutableStateOf(false) }

                                    val trackStepPx = 42f * density.density
                                    val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()

                                    val currentDeltaTime = if (pxPerSec > 0f) (dragAccumX / pxPerSec).toDouble() else 0.0
                                    val previewStart = (aud.startSeconds + currentDeltaTime).coerceAtLeast(0.0)
                                    val layerDelta = kotlin.math.round(dragAccumY / trackStepPx).toInt()
                                    val targetLayer = (audioLayer + layerDelta).coerceAtLeast(0)

                                    Box(
                                        modifier = Modifier
                                            .zIndex(if (isDragging) 99f else 1f)
                                            .offset(
                                                x = leftOffsetDp + (dragAccumX / density.density).dp,
                                                y = (dragAccumY / density.density).dp
                                            )
                                            .width(audWidthDp)
                                            .height(audioTrackHeight)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isAudSelected) Color(0xFF065F46) else Color(0xFF064E3B)
                                            )
                                            .alpha(if (isDragging) 0.7f else 1.0f)
                                            .border(
                                                width = if (isAudSelected) 2.dp else 1.dp,
                                                color = if (isAudSelected) Color(0xFF34D399) else Color(0xFF059669),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .pointerInput(aud.id, audioLayer, zoomScale, density) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = true
                                                        onSelectAudio(aud.id)
                                                    },
                                                    onDragEnd = {
                                                        val finalDeltaTimeSec = if (pxPerSec > 0f) (dragAccumX / pxPerSec).toDouble() else 0.0
                                                        val newStartTime = (aud.startSeconds + finalDeltaTimeSec).coerceAtLeast(0.0)
                                                        val finalLayerDelta = kotlin.math.round(dragAccumY / trackStepPx).toInt()
                                                        val newLayer = (audioLayer + finalLayerDelta).coerceAtLeast(0)

                                                        if (Math.abs(finalDeltaTimeSec) > 0.02 || newLayer != audioLayer) {
                                                            onUpdateAudioTimingAndLayer(aud.id, newStartTime, newLayer)
                                                        }

                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = false
                                                    },
                                                    onDragCancel = {
                                                        dragAccumX = 0f
                                                        dragAccumY = 0f
                                                        isDragging = false
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumX += dragAmount.x
                                                        dragAccumY += dragAmount.y
                                                    }
                                                )
                                            }
                                            .clickable {
                                                onSelectAudio(aud.id)
                                                onSeek(aud.startSeconds)
                                            }
                                    ) {
                                        // Floating drag indicator badge showing target layer and time
                                        if (isDragging) {
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = (-18).dp)
                                            ) {
                                                Text(
                                                    text = "A${targetLayer + 1} · ${String.format(Locale.US, "%.1fs", previewStart)}",
                                                    color = if (targetLayer != audioLayer) Color(0xFF34D399) else Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // Render AudioWaveformVisualizer Component
                                        AudioWaveformVisualizer(
                                            audio = aud,
                                            trackWidthDp = audWidthDp,
                                            trackHeightDp = audioTrackHeight,
                                            isSelected = isAudSelected,
                                            currentPlayTime = currentPlayTime,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Audio track label and volume badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = if (aud.isMuted) "🔇" else "🎵", fontSize = 9.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = aud.name,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFA7F3D0),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = String.format(Locale.US, "%.1fs @ %.1fs · A%d", aud.durationSeconds, aud.startSeconds, audioLayer + 1),
                                                fontSize = 8.sp,
                                                color = Color(0xFF6EE7B7)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Button to add an extra audio track
                    Surface(
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable {
                                onAddAudioTrack()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+ Add Audio Track (Track A${maxAudioLayer + 2})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                }

                // Ultra-Smooth 0.01s Precision Red Playhead Line with Wide 80dp Touch Target & CapCut Studio Needle Head
                val displayPlayheadTime = if (isDraggingPlayhead) dragPlayheadTime else currentPlayTime
                val playheadOffsetDp = (displayPlayheadTime * baseWidth * zoomScale).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp - 40.dp)
                        .width(80.dp) // Generous 80dp touch sensitive zone centered around the playhead
                        .fillMaxHeight()
                        .pointerInput(zoomScale, totalDuration, density) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDraggingPlayhead = true
                                    val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()
                                    if (pxPerSec > 0f) {
                                        // Micro-adjust starting point based on touch offset from needle center
                                        val deltaOffsetPx = offset.x - (with(density) { 40.dp.toPx() })
                                        val deltaSec = (deltaOffsetPx / pxPerSec).toDouble()
                                        val snapped = (Math.round((currentPlayTime + deltaSec) * 100.0) / 100.0).coerceIn(0.0, totalDuration)
                                        dragPlayheadTime = snapped
                                        onSeek(snapped)
                                    } else {
                                        dragPlayheadTime = currentPlayTime
                                        onSeek(currentPlayTime)
                                    }
                                },
                                onDragEnd = {
                                    isDraggingPlayhead = false
                                    onSeek(dragPlayheadTime)
                                },
                                onDragCancel = {
                                    isDraggingPlayhead = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val pxPerSec = (baseWidth * zoomScale * density.density).toFloat()
                                    if (pxPerSec > 0f) {
                                        val deltaSec = (dragAmount.x / pxPerSec).toDouble()
                                        // High-precision 0.01s (10ms) calculation for razor-sharp cutting precision
                                        val newTime = (Math.round((dragPlayheadTime + deltaSec) * 100.0) / 100.0).coerceIn(0.0, totalDuration)
                                        dragPlayheadTime = newTime
                                        onSeek(newTime) // Live scrubbing preview updates instantaneously
                                    }
                                }
                            )
                        }
                        .testTag("draggable_red_playhead"),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Studio-Grade CapCut Scrubber Needle Head (Clean, iconic rounded needle cap with center dot, NO time text)
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 18.dp)
                            .background(
                                color = Color(0xFFEF4444),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // White inner grip/accent dot
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White, CircleShape)
                        )
                    }

                    // Vertical Red Needle Line spanning the full height of the timeline
                    Box(
                        modifier = Modifier
                            .padding(top = 18.dp)
                            .width(2.5.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                    )
                }
            }
        }
    }

    // Multi-Format Media Import Dialog (Images & Videos)
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("মিডিয়া যোগ করুন (ইমেজ ও ভিডিও)")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "টাইমলাইনে নতুন সিন যুক্ত করতে নিচের যেকোনো একটি অপশন নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Gallery Images
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                try {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (_: Exception) {
                                    filesLauncher.launch(arrayOf("image/*"))
                                }
                            }
                            .testTag("import_gallery_images_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF0284C7),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "🖼️ ছবি ইম্পোর্ট করুন (Gallery Images)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "গ্যালারি থেকে একাধিক ফটো/ছবি সিলেক্ট করুন",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Option 2: Gallery Videos
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                try {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                } catch (_: Exception) {
                                    filesLauncher.launch(arrayOf("video/*"))
                                }
                            }
                            .testTag("import_gallery_videos_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF0D9488),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "🎬 ভিডিও ক্লিপ ইম্পোর্ট করুন (Gallery Videos)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "গ্যালারি থেকে ভিডিও ক্লিপ সিলেক্ট করুন (.mp4, .mov)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Option 3: Files / Storage Multi-Select
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showImportDialog = false
                                filesLauncher.launch(arrayOf("image/*", "video/*"))
                            }
                            .testTag("import_files_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF6366F1),
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "📁 ফাইল ম্যানেজার (Files & Storage)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "ফোন মেমোরি / SD Card থেকে যেকোনো ইমেজ ও ভিডিও ফাইল",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("বাতিল করুন (Cancel)")
                }
            }
        )
    }

    // Direct Quick Text Editor Dialog
    if (showTextEditDialog) {
        AlertDialog(
            onDismissRequest = { showTextEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("টেক্সট এডিট করুন (Edit Caption)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingSubtitleText,
                        onValueChange = { editingSubtitleText = it },
                        label = { Text("Caption Text", color = Color(0xFF94A3B8)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_subtitle_input"),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val subToUpdate = subtitles.find { it.id == editingSubtitleId }
                        if (subToUpdate != null && editingSubtitleText.isNotBlank()) {
                            onUpdateSubtitleSegment(subToUpdate.copy(text = editingSubtitleText))
                        }
                        showTextEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextEditDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Studio Audio Automation & Layer Inspector Dialog
    if (showAudioAutomationDialog && selectedAud != null) {
        var autoFadeIn by remember(selectedAud.id) { mutableDoubleStateOf(selectedAud.fadeInSeconds) }
        var autoFadeOut by remember(selectedAud.id) { mutableDoubleStateOf(selectedAud.fadeOutSeconds) }
        var autoDucking by remember(selectedAud.id) { mutableStateOf(selectedAud.isDuckingEnabled || selectedAud.trackIndex == 0) }
        var autoDuckVol by remember(selectedAud.id) { mutableDoubleStateOf(selectedAud.duckVolume.toDouble()) }
        var currentVol by remember(selectedAud.id) { mutableDoubleStateOf(selectedAud.volume.toDouble()) }
        var currentLayerType by remember(selectedAud.id) { mutableStateOf(selectedAud.layerType) }

        AlertDialog(
            onDismissRequest = { showAudioAutomationDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অডিও অটোমেশন (Audio Inspector)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = selectedAud.name,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    // Layer Type Selector
                    Text("Layer Type / Role:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("music" to "🎵 Music", "voiceover" to "🎙️ Voiceover", "sfx" to "💥 SFX").forEach { (type, label) ->
                            Surface(
                                color = if (currentLayerType == type) Color(0xFF059669) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                border = if (currentLayerType == type) BorderStroke(1.dp, Color(0xFF34D399)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        currentLayerType = type
                                        onUpdateAudioLayerType(selectedAud.id, type)
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text("${(currentVol * 100).toInt()}%", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Slider(
                        value = currentVol.toFloat(),
                        onValueChange = { currentVol = it.toDouble() },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF10B981),
                            activeTrackColor = Color(0xFF10B981)
                        )
                    )

                    // Fade In
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fade In", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text(String.format(Locale.US, "%.1fs", autoFadeIn), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Slider(
                        value = autoFadeIn.toFloat(),
                        onValueChange = { autoFadeIn = it.toDouble() },
                        valueRange = 0f..3f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        )
                    )

                    // Fade Out
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fade Out", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text(String.format(Locale.US, "%.1fs", autoFadeOut), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Slider(
                        value = autoFadeOut.toFloat(),
                        onValueChange = { autoFadeOut = it.toDouble() },
                        valueRange = 0f..3f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        )
                    )

                    // Auto-Ducking
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Ducking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "ভয়েসওভার চালু হলে ব্যাকগ্রাউন্ড মিউজিক ভলিউম কমে যাবে",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { autoDucking = !autoDucking },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (autoDucking) Color(0xFF059669) else Color(0xFF334155)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (autoDucking) "ON" else "OFF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (autoDucking) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ducked Volume", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    Text("${(autoDuckVol * 100).toInt()}%", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = autoDuckVol.toFloat(),
                                    onValueChange = { autoDuckVol = it.toDouble() },
                                    valueRange = 0.1f..0.6f
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateAudioVolume(selectedAud.id, currentVol.toFloat())
                        onUpdateAudioAutomation(
                            selectedAud.id,
                            autoFadeIn,
                            autoFadeOut,
                            autoDucking,
                            autoDuckVol.toFloat()
                        )
                        showAudioAutomationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Apply Automation", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioAutomationDialog = false }) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
