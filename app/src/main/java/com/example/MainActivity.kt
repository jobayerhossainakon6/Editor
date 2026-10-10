package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FacelessViewModel
import com.example.ui.components.CapcutHomeScreen
import com.example.ui.components.CapcutTextInspectorSheet
import com.example.ui.components.CapcutTimelineEditor
import com.example.ui.components.DualJsonManager
import com.example.ui.components.ExportRenderScreen
import com.example.ui.components.OnlineAssetHub
import com.example.ui.components.VideoPreviewCanvas
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: FacelessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // BackHandler returns to CapCut Home Screen
                if (uiState.currentScreen == 1) {
                    BackHandler {
                        viewModel.returnToHome()
                    }
                }

                if (uiState.currentScreen == 0) {
                    CapcutHomeScreen(
                        projects = uiState.recentProjects,
                        onOpenProject = { viewModel.openProject(it) },
                        onCreateNewProjectWithImages = { viewModel.createNewProjectWithImages(it) },
                        onDeleteProject = { viewModel.deleteProject(it) }
                    )
                } else {
                    MainCapcutStudioScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCapcutStudioScreen(viewModel: FacelessViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showZipDialog by remember { mutableStateOf(false) }
    var currentZipFile by remember { mutableStateOf<File?>(null) }

    val activeSelectedSub = remember(uiState.subtitles, uiState.selectedSubtitleId) {
        uiState.subtitles.find { it.id == uiState.selectedSubtitleId }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.returnToHome() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = uiState.currentProjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${uiState.exportSettings.resolution} · ${uiState.aspectRatio}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zero Black Blink",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Export Project ZIP Button
                    IconButton(
                        onClick = {
                            viewModel.exportProjectZip { zip ->
                                currentZipFile = zip
                                showZipDialog = true
                            }
                        },
                        modifier = Modifier.testTag("topbar_zip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "Export Project ZIP",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // CapCut Glowing Export Button
                    Button(
                        onClick = {
                            viewModel.selectTab(3)
                            viewModel.startTwoPassRender()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .height(34.dp)
                            .testTag("topbar_render_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        bottomBar = {
            // CapCut Pro Dark Bottom Navigation Dock (4 Clean Studio Tabs)
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.ViewTimeline, contentDescription = "Timeline") },
                    label = { Text("Timeline", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Online Assets") },
                    label = { Text("Online Hub", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Dual-JSON") },
                    label = { Text("Dual-JSON", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = uiState.activeTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Export") },
                    label = { Text("Export", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF1E293B),
                        selectedIconColor = Color(0xFFFBBF24),
                        selectedTextColor = Color(0xFFFBBF24),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F17))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // CapCut Live Compact Preview Viewport (visible on editing tabs)
                if (uiState.activeTab != 3) {
                    VideoPreviewCanvas(
                        images = uiState.images,
                        subtitles = uiState.subtitles,
                        activeTemplate = uiState.selectedTemplate,
                        currentPlayTime = uiState.currentPlayTime,
                        totalDuration = viewModel.totalDuration,
                        isPlaying = uiState.isPlaying,
                        aspectRatio = uiState.aspectRatio,
                        selectedSubtitleId = uiState.selectedSubtitleId,
                        onSelectSubtitle = { viewModel.selectSubtitle(it) },
                        onSelectImage = { viewModel.selectImage(it) },
                        onUpdateAspectRatio = { viewModel.updateAspectRatio(it) },
                        onUpdateSubtitlePosition = { id, x, y -> viewModel.updateSubtitlePosition(id, x, y) },
                        onTogglePlay = { viewModel.togglePlayback() },
                        onSeek = { viewModel.seekTo(it) },
                        onRestart = { viewModel.seekTo(0.0) }
                    )
                }

                // CapCut Multi-Track Horizontal Timeline
                if (uiState.activeTab == 0) {
                    CapcutTimelineEditor(
                        images = uiState.images,
                        subtitles = uiState.subtitles,
                        audios = uiState.audios,
                        selectedImageId = uiState.selectedImageId,
                        selectedSubtitleId = uiState.selectedSubtitleId,
                        selectedAudioId = uiState.selectedAudioId,
                        currentPlayTime = uiState.currentPlayTime,
                        totalDuration = viewModel.totalDuration,
                        isPlaying = uiState.isPlaying,
                        zoomScale = uiState.zoomScale,
                        onUpdateZoomScale = { viewModel.updateZoomScale(it) },
                        onSelectImage = { viewModel.selectImage(it) },
                        onSelectSubtitle = { viewModel.selectSubtitle(it) },
                        onSelectAudio = { viewModel.selectAudio(it) },
                        onAddSubtitle = { viewModel.addSubtitle() },
                        onUpdateSubtitleSegment = { viewModel.updateSubtitleSegment(it) },
                        onAddAudio = { viewModel.addSampleMusic() },
                        onAddAudioUri = { viewModel.addAudioFromUri(it) },
                        onAdjustDuration = { id, delta -> viewModel.adjustImageDuration(id, delta) },
                        onAdjustImageDurationWithDrag = { id, dur -> viewModel.adjustImageDurationWithDrag(id, dur) },
                        onAdjustSubtitleTiming = { id, start, end -> viewModel.adjustSubtitleStartAndEnd(id, start, end) },
                        onMoveImage = { from, to -> viewModel.moveImage(from, to) },
                        onUpdateImageLayer = { id, layer -> viewModel.updateImageLayer(id, layer) },
                        onUpdateImageTimingAndLayer = { id, startT, layer -> viewModel.updateImagePositionAndLayer(id, startT, layer) },
                        onUpdateSubtitleTimingAndLayer = { id, startT, layer -> viewModel.updateSubtitleTimingAndLayer(id, startT, layer) },
                        onAddSubtitleTrack = { viewModel.addSubtitleTrack() },
                        onUpdateAudioTimingAndLayer = { id, startT, layer -> viewModel.updateAudioTimingAndLayer(id, startT, layer) },
                        onAddAudioTrack = { viewModel.addAudioTrackRow() },
                        onAddAudioSampleNamed = { viewModel.addAudioSampleTrack(it) },
                        onSplitImage = { viewModel.splitImageAtPlayhead(it) },
                        onSplitSubtitle = { viewModel.splitSubtitleAtPlayhead(it) },
                        onSplitAudio = { viewModel.splitAudioAtPlayhead(it) },
                        onDuplicateAudio = { viewModel.duplicateAudioTrack(it) },
                        onUpdateAudioVolume = { id, vol -> viewModel.updateAudioVolume(id, vol) },
                        onUpdateAudioAutomation = { id, fadeIn, fadeOut, ducking, duckVol ->
                            viewModel.updateAudioAutomation(id, fadeIn, fadeOut, ducking, duckVol)
                        },
                        onUpdateAudioLayerType = { id, layerType -> viewModel.updateAudioLayerType(id, layerType) },
                        onAddVoiceoverTrack = { viewModel.addVoiceoverTrack() },
                        onAddSfxTrack = { viewModel.addSfxTrack(it) },
                        onToggleAudioMute = { viewModel.toggleAudioMute(it) },
                        onUpdateTransition = { id, trans -> viewModel.updateImageTransition(id, trans) },
                        onDuplicateImage = { viewModel.duplicateImage(it) },
                        onDeleteImage = { viewModel.deleteImage(it) },
                        onDeleteSubtitle = { viewModel.deleteSubtitle(it) },
                        onDeleteAudio = { viewModel.deleteAudioTrack(it) },
                        onAddMultipleImages = { uris -> viewModel.addMultipleCustomImages(uris) },
                        onSeek = { viewModel.seekTo(it) }
                    )
                }

                // Dynamic Content Pane for Secondary Views
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.activeTab) {
                        0 -> {
                            // Timeline Quick Info
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active Sequence (${uiState.images.size} Scenes)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tap text on preview to open CapCut styling",
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                        1 -> {
                            OnlineAssetHub(
                                onlineFonts = uiState.onlineFonts,
                                onlineTextEffects = uiState.onlineTextEffects,
                                onlineVideoFilters = uiState.onlineVideoFilters,
                                onlineTransitions = uiState.onlineTransitions,
                                onlineAudioTracks = uiState.onlineAudioTracks,
                                downloadingAssetId = uiState.downloadingAssetId,
                                downloadProgress = uiState.downloadProgress,
                                activeAssetStatus = uiState.activeAssetStatus,
                                onApplyFont = { viewModel.applyOnlineFontToActiveSubtitle(it) },
                                onApplyTextEffect = { viewModel.applyOnlineTextEffectToActiveSubtitle(it) },
                                onApplyVideoFilter = { viewModel.applyOnlineVideoFilterToActiveScene(it) },
                                onApplyTransition = { viewModel.applyOnlineTransitionToActiveScene(it) },
                                onAddAudioTrack = { viewModel.addAudioSampleTrack(it) }
                            )
                        }
                        2 -> {
                            DualJsonManager(
                                currentImageJson = viewModel.getImageSyncJsonString(),
                                currentTextJson = viewModel.getTextSyncJsonString(),
                                currentAudioJson = viewModel.getAudioSyncJsonString(),
                                onApplyImageJson = { viewModel.importImageSyncJson(it) },
                                onApplyTextJson = { viewModel.importTextSyncJson(it) },
                                onApplyAudioJson = { viewModel.importAudioSyncJson(it) },
                                onExportZip = {
                                    viewModel.exportProjectZip { zip ->
                                        currentZipFile = zip
                                        showZipDialog = true
                                    }
                                }
                            )
                        }
                        3 -> {
                            ExportRenderScreen(
                                renderState = uiState.renderState,
                                totalImages = uiState.images.size,
                                totalSubtitles = uiState.subtitles.size,
                                onUpdateExportSettings = { viewModel.updateExportSettings(it) },
                                onStartRender = { viewModel.startTwoPassRender() },
                                onSaveToGallery = { viewModel.saveToSamsungGallery() },
                                onExportZip = {
                                    viewModel.exportProjectZip { zip ->
                                        currentZipFile = zip
                                        showZipDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // CapCut In-Context Text Inspector Sheet (Slides up when text is tapped!)
            val onlineRepo = remember { com.example.data.OnlineAssetRepository(context) }
            val allAnimations = remember { onlineRepo.getAllAnimations() }

            AnimatedVisibility(
                visible = activeSelectedSub != null && uiState.activeTab == 0,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (activeSelectedSub != null) {
                    CapcutTextInspectorSheet(
                        subtitle = activeSelectedSub,
                        onlineFonts = uiState.onlineFonts,
                        allAnimations = allAnimations,
                        allTextEffects = uiState.onlineTextEffects,
                        onUpdateSubtitle = { viewModel.updateSubtitleSegment(it) },
                        onDeleteSubtitle = {
                            viewModel.deleteSubtitle(it)
                            viewModel.selectSubtitle("")
                        },
                        onClose = { viewModel.selectSubtitle("") }
                    )
                }
            }
        }
    }

    // ZIP Export Dialog with direct Share / Download
    if (showZipDialog && currentZipFile != null) {
        val zip = currentZipFile!!
        AlertDialog(
            onDismissRequest = { showZipDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Project ZIP Ready (with APK)")
                }
            },
            text = {
                Column {
                    Text("Your complete CapCut-style standalone project package:")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• FacelessVideoEditor_SamsungA53.apk (Installable directly)\n• image_sync.json\n• text_sync.json\n• Online Asset Directory\n• High-res scene media",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "File: ${zip.name} (${zip.length() / 1024} KB)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_SUBJECT, "Faceless Project Bundle")
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    zip
                                )
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } catch (_: Exception) {}
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Project ZIP"))
                        showZipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Save ZIP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showZipDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
