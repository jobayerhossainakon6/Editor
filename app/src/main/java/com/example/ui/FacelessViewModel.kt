package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OnlineAssetRepository
import com.example.data.ProjectRepository
import com.example.engine.AssSubtitleParser
import com.example.engine.AudioPlayerEngine
import com.example.engine.VideoRenderEngine
import com.example.model.AssTemplateInfo
import com.example.model.AudioSegment
import com.example.model.ExportSettings
import com.example.model.ImageSegment
import com.example.model.OnlineEffectAsset
import com.example.model.OnlineFontAsset
import com.example.model.ProjectRecord
import com.example.model.RenderState
import com.example.model.SubtitleSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val isLoading: Boolean = true,
    val currentScreen: Int = 0, // 0: Capcut Home Screen (Projects List), 1: Capcut Studio Editor
    val currentProjectId: String = "proj_viral_hook",
    val currentProjectName: String = "Untitled Project",
    val recentProjects: List<ProjectRecord> = emptyList(),
    val images: List<ImageSegment> = emptyList(),
    val subtitles: List<SubtitleSegment> = emptyList(),
    val audios: List<com.example.model.AudioSegment> = emptyList(),
    val templates: List<AssTemplateInfo> = emptyList(),
    val selectedTemplate: AssTemplateInfo? = null,
    val onlineFonts: List<OnlineFontAsset> = emptyList(),
    val onlineTextEffects: List<OnlineEffectAsset> = emptyList(),
    val onlineVideoFilters: List<OnlineEffectAsset> = emptyList(),
    val onlineTransitions: List<OnlineEffectAsset> = emptyList(),
    val onlineAudioTracks: List<OnlineEffectAsset> = emptyList(),
    val activeTab: Int = 0, // 0: Timeline, 1: Online Hub, 2: Dual-JSON, 3: Export
    val aspectRatio: String = "16:9", // "16:9", "9:16", "1:1", "4:5"
    val currentPlayTime: Double = 0.0,
    val isPlaying: Boolean = false,
    val zoomScale: Float = 1.0f, // Timeline Zoom Scale: 0.1f (max compression) to 3.0f
    val selectedImageId: String? = null,
    val selectedSubtitleId: String? = null,
    val selectedAudioId: String? = null,
    val downloadingAssetId: String? = null,
    val downloadProgress: Int = 0,
    val activeAssetStatus: String? = null,
    val exportSettings: ExportSettings = ExportSettings(),
    val renderState: RenderState = RenderState(),
    val statusMessage: String? = null,
    val exportedZipFile: File? = null
)

class FacelessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val onlineRepo = OnlineAssetRepository(application)
    private val renderEngine = VideoRenderEngine(application)
    private val audioPlayerEngine = AudioPlayerEngine(application)
    private val audioMixerEngine = com.example.engine.AudioMixerEngine(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    init {
        loadInitialData()
        viewModelScope.launch(Dispatchers.IO) {
            audioMixerEngine.ensureSampleAudioFilesExist()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.initializeDefaultAssetsIfNeeded()
            val templates = repository.loadAssTemplates()
            val recentProjects = repository.getRecentProjects()

            // Restore last active project if exists
            val lastActiveId = repository.getLastActiveProjectId()
            val targetProject = recentProjects.find { it.id == lastActiveId } ?: recentProjects.firstOrNull()

            val activeId = targetProject?.id ?: "proj_viral_hook"
            val activeName = targetProject?.name ?: "Viral Short Hook Demo"

            val loadedProject = targetProject?.let { repository.loadFullProject(it.id) }
            val (images, subtitles, audios) = loadedProject ?: repository.loadInitialData()

            val defaultTemplate = templates.find { it.fileName == "hormozi_bold.ass" } ?: templates.firstOrNull()

            val onlineFonts = onlineRepo.getOnlineFonts()
            val onlineTextFx = onlineRepo.getOnlineTextEffects()
            val onlineVideoFilters = onlineRepo.getOnlineVideoFilters()
            val onlineTransitions = onlineRepo.getOnlineTransitions()
            val onlineAudioTracks = onlineRepo.getOnlineAudioTracks()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentScreen = 0, // Start at Home Screen
                    currentProjectId = activeId,
                    currentProjectName = activeName,
                    recentProjects = recentProjects,
                    images = images,
                    subtitles = subtitles,
                    audios = audios,
                    templates = templates,
                    selectedTemplate = defaultTemplate,
                    onlineFonts = onlineFonts,
                    onlineTextEffects = onlineTextFx,
                    onlineVideoFilters = onlineVideoFilters,
                    onlineTransitions = onlineTransitions,
                    onlineAudioTracks = onlineAudioTracks,
                    selectedImageId = images.firstOrNull()?.id,
                    selectedSubtitleId = subtitles.firstOrNull()?.id,
                    selectedAudioId = audios.firstOrNull()?.id
                )
            }
        }
    }

    // --- Screen Navigation & Full Project Persistence ---

    fun openProject(project: ProjectRecord) {
        viewModelScope.launch {
            val loaded = repository.loadFullProject(project.id)
            val images = loaded?.first ?: _uiState.value.images
            val subtitles = loaded?.second ?: _uiState.value.subtitles
            val audios = loaded?.third ?: _uiState.value.audios
            repository.setLastActiveProjectId(project.id)

            _uiState.update {
                it.copy(
                    currentScreen = 1,
                    currentProjectId = project.id,
                    currentProjectName = project.name,
                    images = images,
                    subtitles = subtitles,
                    audios = audios,
                    selectedImageId = images.firstOrNull()?.id,
                    selectedSubtitleId = subtitles.firstOrNull()?.id,
                    selectedAudioId = audios.firstOrNull()?.id,
                    activeTab = 0
                )
            }
        }
    }

    fun returnToHome() {
        pausePlayback()
        autoSaveActiveProject()
        _uiState.update { it.copy(currentScreen = 0) }
    }

    fun createNewProjectWithImages(uris: List<Uri>) {
        viewModelScope.launch {
            val segments = if (uris.isNotEmpty()) {
                repository.importMultipleCustomImages(uris)
            } else {
                repository.loadInitialData().first
            }

            val projectName = "Project #${_uiState.value.recentProjects.size + 1}"
            val newRecord = repository.createNewProject(projectName, segments)
            val updatedProjects = repository.getRecentProjects()

            _uiState.update {
                it.copy(
                    currentScreen = 1,
                    currentProjectId = newRecord.id,
                    currentProjectName = projectName,
                    recentProjects = updatedProjects,
                    images = segments,
                    selectedImageId = segments.firstOrNull()?.id,
                    activeTab = 0,
                    statusMessage = "Created $projectName with ${segments.size} scenes"
                )
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProjectRecord(id)
            val updated = repository.getRecentProjects()
            _uiState.update { it.copy(recentProjects = updated, statusMessage = "Project deleted") }
        }
    }

    private fun autoSaveActiveProject() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveFullProject(
                projectId = state.currentProjectId,
                name = state.currentProjectName,
                images = state.images,
                subtitles = state.subtitles,
                audios = state.audios
            )
            val updated = repository.getRecentProjects()
            _uiState.update { it.copy(recentProjects = updated) }
        }
    }

    // --- Studio Navigation & Aspect Ratio ---

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun updateAspectRatio(ratio: String) {
        _uiState.update {
            it.copy(
                aspectRatio = ratio,
                exportSettings = it.exportSettings.copy(aspectRatio = ratio)
            )
        }
    }

    fun updateZoomScale(zoom: Float) {
        val clamped = zoom.coerceIn(0.1f, 3.0f)
        _uiState.update { it.copy(zoomScale = clamped) }
    }

    fun selectImage(id: String) {
        val images = _uiState.value.images
        var startT = 0.0
        for (img in images) {
            if (img.id == id) break
            startT += img.durationSeconds
        }
        pausePlayback()
        _uiState.update {
            it.copy(
                selectedImageId = id,
                currentPlayTime = startT.coerceAtLeast(0.0)
            )
        }
    }

    fun selectSubtitle(id: String) {
        val sub = _uiState.value.subtitles.find { it.id == id }
        val targetTime = sub?.let { (it.startSeconds + 0.05).coerceAtLeast(0.0) } ?: _uiState.value.currentPlayTime
        pausePlayback()
        _uiState.update {
            it.copy(
                selectedSubtitleId = id,
                currentPlayTime = targetTime
            )
        }
    }

    fun selectAudio(id: String) {
        val aud = _uiState.value.audios.find { it.id == id }
        val targetTime = aud?.let { it.startSeconds.coerceAtLeast(0.0) } ?: _uiState.value.currentPlayTime
        pausePlayback()
        _uiState.update {
            it.copy(
                selectedAudioId = id,
                currentPlayTime = targetTime
            )
        }
    }

    // --- Playback Engine ---

    val totalDuration: Double
        get() {
            var sequentialImgEnd = 0.0
            var maxExplicitImgEnd = 0.0
            for (img in _uiState.value.images) {
                if (img.startTimeSeconds >= 0.0) {
                    val end = img.startTimeSeconds + img.durationSeconds
                    if (end > maxExplicitImgEnd) maxExplicitImgEnd = end
                } else {
                    sequentialImgEnd += img.durationSeconds
                }
            }
            val imgDur = maxOf(sequentialImgEnd, maxExplicitImgEnd)
            val subDur = _uiState.value.subtitles.maxOfOrNull { it.endSeconds } ?: 0.0
            val audDur = _uiState.value.audios.maxOfOrNull { it.startSeconds + it.durationSeconds } ?: 0.0
            return maxOf(imgDur, subDur, audDur).coerceAtLeast(1.0)
        }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun togglePlayback() = togglePlayPause()

    fun startPlayback() {
        playbackJob?.cancel()
        val startMediaTime = _uiState.value.currentPlayTime
        val startWallTime = android.os.SystemClock.elapsedRealtime()
        _uiState.update { it.copy(isPlaying = true) }
        // Start audio playback immediately with zero latency
        audioPlayerEngine.syncPlayback(_uiState.value.audios, startMediaTime, true)
        playbackJob = viewModelScope.launch {
            while (isActive) {
                delay(33) // ~30 fps preview
                val total = totalDuration
                if (total <= 0) break
                val elapsedSec = (android.os.SystemClock.elapsedRealtime() - startWallTime) / 1000.0
                val nextTime = Math.round((startMediaTime + elapsedSec) * 100.0) / 100.0
                if (nextTime >= total) {
                    _uiState.update { it.copy(currentPlayTime = 0.0, isPlaying = false) }
                    audioPlayerEngine.syncPlayback(_uiState.value.audios, 0.0, false)
                    break
                } else {
                    _uiState.update { it.copy(currentPlayTime = nextTime) }
                    audioPlayerEngine.syncPlayback(_uiState.value.audios, nextTime, true)
                }
            }
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlaying = false) }
        audioPlayerEngine.pauseAll()
    }

    fun seekTo(seconds: Double) {
        val bounded = (Math.round(seconds.coerceIn(0.0, totalDuration) * 100.0) / 100.0)
        _uiState.update { it.copy(currentPlayTime = bounded) }
        audioPlayerEngine.seekTo(_uiState.value.audios, bounded)
    }

    // --- Image & Media Actions ---

    fun adjustImageDuration(id: String, delta: Double) {
        _uiState.update { state ->
            val updated = state.images.map { img ->
                if (img.id == id) {
                    val newDur = (img.durationSeconds + delta).coerceIn(0.5, 30.0)
                    img.copy(durationSeconds = Math.round(newDur * 10.0) / 10.0)
                } else img
            }
            state.copy(images = updated)
        }
        autoSaveActiveProject()
    }

    fun updateImageTransition(id: String, transition: String) {
        _uiState.update { state ->
            val updated = state.images.map {
                if (it.id == id) it.copy(transition = transition) else it
            }
            state.copy(images = updated)
        }
        autoSaveActiveProject()
    }

    fun updateSceneEffect(id: String, effect: String) {
        _uiState.update { state ->
            val updated = state.images.map {
                if (it.id == id) it.copy(effect = effect) else it
            }
            state.copy(images = updated, statusMessage = "Applied '$effect' filter to active scene")
        }
        autoSaveActiveProject()
    }

    fun duplicateImage(id: String) {
        _uiState.update { state ->
            val target = state.images.find { it.id == id } ?: return@update state
            val idx = state.images.indexOf(target)
            val duplicated = target.copy(
                id = "img_${System.currentTimeMillis()}",
                title = "${target.title} (Copy)"
            )
            val list = state.images.toMutableList()
            list.add(idx + 1, duplicated)
            state.copy(images = list, selectedImageId = duplicated.id)
        }
        autoSaveActiveProject()
    }

    fun deleteImage(id: String) {
        _uiState.update { state ->
            if (state.images.size > 1) {
                val remaining = state.images.filterNot { it.id == id }
                val nextSelected = if (state.selectedImageId == id) remaining.firstOrNull()?.id else state.selectedImageId
                state.copy(images = remaining, selectedImageId = nextSelected)
            } else {
                state.copy(statusMessage = "Cannot delete the only image")
            }
        }
        autoSaveActiveProject()
    }

    fun addMultipleCustomImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val segments = repository.importMultipleCustomImages(uris)
            _uiState.update {
                it.copy(
                    images = it.images + segments,
                    selectedImageId = segments.lastOrNull()?.id ?: it.selectedImageId,
                    statusMessage = "Imported ${segments.size} scenes into timeline"
                )
            }
            autoSaveActiveProject()
        }
    }

    // --- Subtitle Actions with Pixel Coordinates & Individual Styling ---

    fun updateSubtitlePosition(id: String, posX: Float, posY: Float) {
        _uiState.update { state ->
            val updated = state.subtitles.map {
                if (it.id == id) it.copy(posX = posX.coerceIn(0.05f, 0.95f), posY = posY.coerceIn(0.05f, 0.95f)) else it
            }
            state.copy(subtitles = updated)
        }
        autoSaveActiveProject()
    }

    fun updateSubtitleLayer(id: String, newLayerIndex: Int) {
        _uiState.update { state ->
            val updated = state.subtitles.map {
                if (it.id == id) it.copy(trackIndex = newLayerIndex.coerceAtLeast(0)) else it
            }
            state.copy(subtitles = updated, statusMessage = "Moved text to Text Layer $newLayerIndex")
        }
        autoSaveActiveProject()
    }

    fun updateSubtitleTimingAndLayer(id: String, newStart: Double, newLayerIndex: Int) {
        _uiState.update { state ->
            val target = state.subtitles.find { it.id == id } ?: return@update state
            val dur = (target.endSeconds - target.startSeconds).coerceAtLeast(0.3)
            val clampedStart = (Math.round(newStart * 100.0) / 100.0).coerceAtLeast(0.0)
            val clampedLayer = newLayerIndex.coerceAtLeast(0)
            val updated = state.subtitles.map {
                if (it.id == id) {
                    it.copy(
                        startSeconds = clampedStart,
                        endSeconds = clampedStart + dur,
                        trackIndex = clampedLayer
                    )
                } else it
            }
            state.copy(
                subtitles = updated,
                statusMessage = "Moved text to ${String.format(java.util.Locale.US, "%.1fs", clampedStart)} (Layer $clampedLayer)"
            )
        }
        autoSaveActiveProject()
    }

    fun addSubtitleTrack() {
        val maxLayer = (_uiState.value.subtitles.maxOfOrNull { it.trackIndex } ?: 0).coerceAtLeast(0)
        val newLayer = maxLayer + 1
        val newPosY = (0.82f - (newLayer * 0.20f)).coerceIn(0.15f, 0.85f)
        val newSub = SubtitleSegment(
            id = "sub_${System.currentTimeMillis()}",
            startSeconds = _uiState.value.currentPlayTime,
            endSeconds = _uiState.value.currentPlayTime + 3.0,
            text = "LAYER $newLayer TEXT",
            posY = newPosY,
            trackIndex = newLayer
        )
        _uiState.update {
            it.copy(
                subtitles = it.subtitles + newSub,
                selectedSubtitleId = newSub.id,
                statusMessage = "Added Text Track T$newLayer"
            )
        }
        autoSaveActiveProject()
    }

    fun updateSubtitleSegment(sub: SubtitleSegment) {
        _uiState.update { state ->
            val updated = state.subtitles.map {
                if (it.id == sub.id) sub else it
            }
            state.copy(
                subtitles = updated,
                statusMessage = "Updated: ${sub.animationPreset} & ${sub.textEffect}"
            )
        }
        autoSaveActiveProject()
    }

    fun addSubtitle() {
        val total = totalDuration
        val playhead = _uiState.value.currentPlayTime
        val newStart = playhead.coerceAtLeast(0.0)
        val newEnd = newStart + 2.5
        val count = _uiState.value.subtitles.size + 1
        val newSub = SubtitleSegment(
            id = "sub_${System.currentTimeMillis()}",
            startSeconds = newStart,
            endSeconds = newEnd,
            text = "NEW HOOK CAPTION #$count",
            templateName = _uiState.value.selectedTemplate?.fileName ?: "hormozi_bold.ass",
            animationPreset = "Pop-up",
            fontName = com.example.model.ClassicTextStyle.NAME,
            fontSize = 38,
            textEffect = com.example.model.ClassicTextStyle.NAME,
            textColorHex = com.example.model.ClassicTextStyle.TEXT_COLOR_HEX,
            outlineColorHex = com.example.model.ClassicTextStyle.OUTLINE_COLOR_HEX,
            outlineWidth = com.example.model.ClassicTextStyle.OUTLINE_WIDTH
        )
        _uiState.update {
            it.copy(
                subtitles = it.subtitles + newSub,
                selectedSubtitleId = newSub.id,
                statusMessage = "Added caption at ${String.format(java.util.Locale.US, "%.1fs", newStart)}"
            )
        }
        autoSaveActiveProject()
    }

    fun deleteSubtitle(id: String) {
        _uiState.update { state ->
            val remaining = state.subtitles.filterNot { it.id == id }
            val nextSelected = if (state.selectedSubtitleId == id) remaining.firstOrNull()?.id else state.selectedSubtitleId
            state.copy(subtitles = remaining, selectedSubtitleId = nextSelected)
        }
        autoSaveActiveProject()
    }

    fun selectTemplate(template: AssTemplateInfo) {
        _uiState.update { it.copy(selectedTemplate = template, statusMessage = "Applied '${template.name}' template") }
    }

    // --- Online Hub Integration ---

    fun applyOnlineFontToActiveSubtitle(fontName: String) {
        val activeId = _uiState.value.selectedSubtitleId ?: _uiState.value.subtitles.firstOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingAssetId = fontName, downloadProgress = 25) }
            delay(80)
            _uiState.update { it.copy(downloadProgress = 70) }
            delay(80)
            _uiState.update { it.copy(downloadProgress = 100) }
            delay(40)

            _uiState.update { state ->
                val updated = state.subtitles.map { sub ->
                    if (sub.id == activeId) sub.copy(fontName = fontName) else sub
                }
                state.copy(
                    subtitles = updated,
                    downloadingAssetId = null,
                    downloadProgress = 0,
                    activeAssetStatus = "Font '$fontName' downloaded & active!",
                    statusMessage = "Applied '$fontName' to selected caption"
                )
            }
            autoSaveActiveProject()
        }
    }

    fun applyOnlineFontToActiveSubtitle(font: OnlineFontAsset) = applyOnlineFontToActiveSubtitle(font.name)

    fun applyOnlineTextEffectToActiveSubtitle(effect: OnlineEffectAsset) {
        val activeId = _uiState.value.selectedSubtitleId ?: _uiState.value.subtitles.firstOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingAssetId = effect.id, downloadProgress = 30) }
            delay(100)
            _uiState.update { it.copy(downloadProgress = 75) }
            delay(100)
            _uiState.update { it.copy(downloadProgress = 100) }
            delay(50)

            _uiState.update { state ->
                val updated = state.subtitles.map { sub ->
                    if (sub.id == activeId) {
                        when (effect.id) {
                            "fx_hormozi" -> sub.copy(fontName = "Impact", textColorHex = "#FFFF00", outlineColorHex = "#000000", textEffect = "Glow", animationPreset = "Pop-up", bgBoxColorHex = null)
                            "fx_cyber_cyan" -> sub.copy(fontName = "Righteous", textColorHex = "#00FFFF", outlineColorHex = "#EC4899", textEffect = "Neon", animationPreset = "Word-Glow", bgBoxColorHex = null)
                            "fx_mrbeast" -> sub.copy(fontName = "Anton", textColorHex = "#F59E0B", outlineColorHex = "#000000", textEffect = "3D Stroke", animationPreset = "Bounce", bgBoxColorHex = null)
                            "fx_ali_abdaal" -> sub.copy(fontName = "Montserrat", textColorHex = "#FFFFFF", outlineColorHex = "#000000", textEffect = "Clean", animationPreset = "Fade-In", bgBoxColorHex = null)
                            "fx_anime_slash" -> sub.copy(fontName = "Bangers", textColorHex = "#EF4444", outlineColorHex = "#FFFFFF", textEffect = "3D Stroke", animationPreset = "Zoom-Punch", bgBoxColorHex = null)
                            "fx_karaoke" -> sub.copy(fontName = "Bebas Neue", textColorHex = "#10B981", outlineColorHex = "#000000", textEffect = "Glow", animationPreset = "Karaoke", bgBoxColorHex = null)
                            "fx_glitch_rgb" -> sub.copy(fontName = "Righteous", textColorHex = "#8B5CF6", outlineColorHex = "#00FFFF", textEffect = "Glitch Matrix", animationPreset = "Glitch Flicker", bgBoxColorHex = null)
                            else -> sub.copy(textEffect = "Glow", bgBoxColorHex = null)
                        }
                    } else sub
                }
                state.copy(
                    subtitles = updated,
                    downloadingAssetId = null,
                    downloadProgress = 0,
                    activeAssetStatus = "Downloaded '${effect.name}'!",
                    statusMessage = "Applied '${effect.name}' to selected caption"
                )
            }
            autoSaveActiveProject()
        }
    }

    fun applyOnlineVideoFilterToActiveScene(filterId: String) {
        val activeImgId = _uiState.value.selectedImageId ?: _uiState.value.images.firstOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingAssetId = filterId, downloadProgress = 35) }
            delay(100)
            _uiState.update { it.copy(downloadProgress = 80) }
            delay(100)
            _uiState.update { it.copy(downloadProgress = 100) }
            delay(50)

            updateSceneEffect(activeImgId, filterId)
            _uiState.update {
                it.copy(
                    downloadingAssetId = null,
                    downloadProgress = 0,
                    activeAssetStatus = "Filter '$filterId' downloaded & active in preview!"
                )
            }
        }
    }

    fun applyOnlineTransitionToActiveScene(transitionName: String) {
        val activeImgId = _uiState.value.selectedImageId ?: _uiState.value.images.firstOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingAssetId = transitionName, downloadProgress = 30) }
            delay(100)
            _uiState.update { it.copy(downloadProgress = 75) }
            delay(100)
            _uiState.update { it.copy(downloadProgress = 100) }
            delay(50)

            updateImageTransition(activeImgId, transitionName)
            _uiState.update {
                it.copy(
                    downloadingAssetId = null,
                    downloadProgress = 0,
                    activeAssetStatus = "Transition '$transitionName' downloaded & active on timeline!",
                    statusMessage = "Applied '$transitionName' transition"
                )
            }
        }
    }

    // --- Export Settings ---

    fun updateExportSettings(settings: ExportSettings) {
        _uiState.update { it.copy(exportSettings = settings, aspectRatio = settings.aspectRatio) }
    }

    // --- Dual JSON Import & Export ---

    fun getImageSyncJsonString(): String {
        return repository.generateImageSyncJson(_uiState.value.images)
    }

    fun getTextSyncJsonString(): String {
        return repository.generateTextSyncJson(_uiState.value.subtitles)
    }

    fun getAudioSyncJsonString(): String {
        return repository.generateAudioSyncJson(_uiState.value.audios)
    }

    fun importImageSyncJson(json: String) {
        viewModelScope.launch {
            try {
                val clean = json.trim()
                if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                    _uiState.update {
                        it.copy(
                            images = emptyList(),
                            selectedImageId = null,
                            statusMessage = "ইমেজ লেয়ার খালি করা হয়েছে (Blank Canvas)"
                        )
                    }
                    autoSaveActiveProject()
                    return@launch
                }
                val currentImages = _uiState.value.images
                val parsed = repository.parseImageSyncJson(json, currentImages)
                _uiState.update {
                    it.copy(
                        images = parsed,
                        selectedImageId = parsed.firstOrNull()?.id ?: it.selectedImageId,
                        statusMessage = "ইমেজ জেসন সফলভাবে সিঙ্ক হয়েছে (${parsed.size}টি সিন, ছবি অক্ষুণ্ণ রাখা হয়েছে)"
                    )
                }
                autoSaveActiveProject()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "JSON Error: ${e.message}") }
            }
        }
    }

    fun importTextSyncJson(json: String) {
        viewModelScope.launch {
            try {
                val clean = json.trim()
                if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                    _uiState.update {
                        it.copy(
                            subtitles = emptyList(),
                            selectedSubtitleId = null,
                            statusMessage = "টেক্সট লেয়ার খালি করা হয়েছে (Blank Canvas)"
                        )
                    }
                    autoSaveActiveProject()
                    return@launch
                }
                val parsed = repository.parseTextSyncJson(json)
                _uiState.update {
                    it.copy(
                        subtitles = parsed,
                        selectedSubtitleId = parsed.firstOrNull()?.id ?: it.selectedSubtitleId,
                        statusMessage = "টেক্সট জেসন সফলভাবে সিঙ্ক হয়েছে (${parsed.size}টি সাবটাইটেল)"
                    )
                }
                autoSaveActiveProject()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "JSON Error: ${e.message}") }
            }
        }
    }

    fun importAudioSyncJson(json: String) {
        viewModelScope.launch {
            try {
                val clean = json.trim()
                if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                    _uiState.update {
                        it.copy(
                            audios = emptyList(),
                            selectedAudioId = null,
                            statusMessage = "অডিও লেয়ার খালি করা হয়েছে (Blank Audio Canvas)"
                        )
                    }
                    autoSaveActiveProject()
                    return@launch
                }
                val parsed = repository.parseAudioSyncJson(json)
                _uiState.update {
                    it.copy(
                        audios = parsed,
                        selectedAudioId = parsed.firstOrNull()?.id ?: it.selectedAudioId,
                        statusMessage = "অডিও জেসন সফলভাবে সিঙ্ক হয়েছে (${parsed.size}টি অডিও ট্র্যাক)"
                    )
                }
                autoSaveActiveProject()
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "JSON Error: ${e.message}") }
            }
        }
    }

    // --- Audio Layer Actions ---

    fun addAudioTrack(name: String, path: String, duration: Double = 15.0) {
        val newAudio = com.example.model.AudioSegment(
            id = "audio_${System.currentTimeMillis()}",
            name = name,
            localPath = path,
            startSeconds = _uiState.value.currentPlayTime,
            durationSeconds = duration
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Added audio track '$name'"
            )
        }
        autoSaveActiveProject()
    }

    fun addSampleMusic() {
        val count = _uiState.value.audios.count { it.layerType == "music" || it.trackIndex == 0 } + 1
        val path = audioMixerEngine.getSampleAudioPath("Lo-Fi Beat #$count", "music")
        val newAudio = com.example.model.AudioSegment(
            id = "audio_${System.currentTimeMillis()}",
            name = "Background Beat #$count",
            localPath = path,
            startSeconds = _uiState.value.currentPlayTime,
            durationSeconds = 15.0,
            volume = 0.85f,
            trackIndex = 0,
            layerType = "music",
            fadeInSeconds = 0.8,
            fadeOutSeconds = 1.0,
            isLooping = true,
            isDuckingEnabled = true,
            duckVolume = 0.25f
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Added background music (Layer 0) with Auto-Ducking"
            )
        }
        autoSaveActiveProject()
    }

    fun addVoiceoverTrack(duration: Double = 8.0) {
        val count = _uiState.value.audios.count { it.layerType == "voiceover" || it.trackIndex == 1 } + 1
        val path = audioMixerEngine.getSampleAudioPath("AI Voiceover Narration", "voiceover")
        val newAudio = com.example.model.AudioSegment(
            id = "audio_vo_${System.currentTimeMillis()}",
            name = "Voiceover Narration #$count",
            localPath = path,
            startSeconds = _uiState.value.currentPlayTime,
            durationSeconds = duration,
            volume = 0.95f,
            trackIndex = 1,
            layerType = "voiceover",
            fadeInSeconds = 0.2,
            fadeOutSeconds = 0.3,
            isLooping = false
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Added Voiceover Track (Layer 1) - BGM will duck automatically!"
            )
        }
        autoSaveActiveProject()
    }

    fun addSfxTrack(name: String = "Impact Punch", duration: Double = 2.5) {
        val path = audioMixerEngine.getSampleAudioPath(name, "sfx")
        val newAudio = com.example.model.AudioSegment(
            id = "audio_sfx_${System.currentTimeMillis()}",
            name = name,
            localPath = path,
            startSeconds = _uiState.value.currentPlayTime,
            durationSeconds = duration,
            volume = 0.90f,
            trackIndex = 2,
            layerType = "sfx",
            fadeInSeconds = 0.0,
            fadeOutSeconds = 0.4,
            isLooping = false
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Added Sound Effect '$name' (Layer 2)"
            )
        }
        autoSaveActiveProject()
    }

    fun updateAudioAutomation(
        id: String,
        fadeInSec: Double,
        fadeOutSec: Double,
        ducking: Boolean,
        duckVol: Float
    ) {
        _uiState.update { state ->
            val updated = state.audios.map {
                if (it.id == id) {
                    it.copy(
                        fadeInSeconds = fadeInSec.coerceAtLeast(0.0),
                        fadeOutSeconds = fadeOutSec.coerceAtLeast(0.0),
                        isDuckingEnabled = ducking,
                        duckVolume = duckVol.coerceIn(0f, 1f)
                    )
                } else it
            }
            state.copy(audios = updated, statusMessage = "Updated audio automation curves")
        }
        autoSaveActiveProject()
    }

    fun updateAudioLayerType(id: String, layerType: String) {
        val targetTrack = when (layerType) {
            "voiceover" -> 1
            "sfx" -> 2
            "ambient" -> 3
            else -> 0
        }
        _uiState.update { state ->
            val updated = state.audios.map {
                if (it.id == id) it.copy(layerType = layerType, trackIndex = targetTrack) else it
            }
            state.copy(audios = updated, statusMessage = "Changed audio type to '$layerType' (Layer $targetTrack)")
        }
        autoSaveActiveProject()
    }

    fun addAudioFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val audioDir = File(context.filesDir, "audio_tracks").apply { if (!exists()) mkdirs() }
                val targetFile = File(audioDir, "audio_${System.currentTimeMillis()}.mp3")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val trackName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Imported Audio Track"
                addAudioTrack(trackName, targetFile.absolutePath, 30.0)
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Audio import error: ${e.message}") }
            }
        }
    }

    fun updateImageLayer(id: String, newLayerIndex: Int) {
        _uiState.update { state ->
            val updated = state.images.map { img ->
                if (img.id == id) img.copy(trackIndex = newLayerIndex.coerceAtLeast(0)) else img
            }
            state.copy(images = updated, statusMessage = "Moved scene to Layer $newLayerIndex")
        }
        autoSaveActiveProject()
    }

    fun updateImagePositionAndLayer(id: String, newStartTime: Double, newLayerIndex: Int) {
        _uiState.update { state ->
            val clampedTime = (Math.round(newStartTime * 100.0) / 100.0).coerceAtLeast(0.0)
            val clampedLayer = newLayerIndex.coerceAtLeast(0)
            val updated = state.images.map { img ->
                if (img.id == id) {
                    img.copy(
                        startTimeSeconds = clampedTime,
                        trackIndex = clampedLayer
                    )
                } else img
            }
            state.copy(
                images = updated,
                statusMessage = "Moved scene to ${String.format(java.util.Locale.US, "%.1fs", clampedTime)} (Layer $clampedLayer)"
            )
        }
        autoSaveActiveProject()
    }

    fun deleteAudioTrack(id: String) {
        _uiState.update { state ->
            val remaining = state.audios.filterNot { it.id == id }
            state.copy(audios = remaining, selectedAudioId = remaining.firstOrNull()?.id)
        }
        autoSaveActiveProject()
    }

    fun updateAudioVolume(id: String, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { state ->
            val updated = state.audios.map {
                if (it.id == id) it.copy(volume = clamped) else it
            }
            state.copy(audios = updated)
        }
        audioPlayerEngine.updateVolume(id, clamped)
        autoSaveActiveProject()
    }

    fun toggleAudioMute(id: String) {
        var isNowMuted = false
        _uiState.update { state ->
            val updated = state.audios.map {
                if (it.id == id) {
                    isNowMuted = !it.isMuted
                    it.copy(isMuted = isNowMuted)
                } else it
            }
            state.copy(audios = updated)
        }
        audioPlayerEngine.updateMute(id, isNowMuted)
        autoSaveActiveProject()
    }

    fun adjustAudioStart(id: String, delta: Double) {
        _uiState.update { state ->
            val updated = state.audios.map {
                if (it.id == id) {
                    val newStart = (it.startSeconds + delta).coerceAtLeast(0.0)
                    it.copy(startSeconds = Math.round(newStart * 10.0) / 10.0)
                } else it
            }
            state.copy(audios = updated)
        }
        autoSaveActiveProject()
    }

    fun adjustAudioDuration(id: String, delta: Double) {
        _uiState.update { state ->
            val updated = state.audios.map {
                if (it.id == id) {
                    val newDur = (it.durationSeconds + delta).coerceIn(0.5, 300.0)
                    it.copy(durationSeconds = Math.round(newDur * 10.0) / 10.0)
                } else it
            }
            state.copy(audios = updated)
        }
        autoSaveActiveProject()
    }

    fun updateAudioTimingAndLayer(id: String, newStart: Double, newLayerIndex: Int) {
        _uiState.update { state ->
            val clampedStart = (Math.round(newStart * 100.0) / 100.0).coerceAtLeast(0.0)
            val clampedLayer = newLayerIndex.coerceAtLeast(0)
            val updated = state.audios.map {
                if (it.id == id) {
                    it.copy(
                        startSeconds = clampedStart,
                        trackIndex = clampedLayer
                    )
                } else it
            }
            state.copy(
                audios = updated,
                statusMessage = "Moved audio to ${String.format(java.util.Locale.US, "%.1fs", clampedStart)} (Track A$clampedLayer)"
            )
        }
        autoSaveActiveProject()
    }

    fun addAudioTrackRow() {
        val maxTrack = (_uiState.value.audios.maxOfOrNull { it.trackIndex } ?: 0).coerceAtLeast(0)
        val newTrack = maxTrack + 1
        val path = audioMixerEngine.getSampleAudioPath("Audio Track A$newTrack")
        val newAudio = com.example.model.AudioSegment(
            id = "audio_${System.currentTimeMillis()}",
            name = "Audio Track A$newTrack",
            localPath = path,
            startSeconds = _uiState.value.currentPlayTime,
            durationSeconds = 15.0,
            volume = 0.85f,
            trackIndex = newTrack
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Added Audio Track A$newTrack"
            )
        }
        autoSaveActiveProject()
    }

    fun duplicateAudioTrack(id: String) {
        val audios = _uiState.value.audios
        val target = audios.find { it.id == id } ?: return
        val newAudio = target.copy(
            id = "audio_${System.currentTimeMillis()}",
            name = "${target.name} (Copy)",
            startSeconds = target.startSeconds + target.durationSeconds
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Duplicated audio clip"
            )
        }
        autoSaveActiveProject()
    }

    fun splitAudioAtPlayhead(id: String? = null) {
        val audios = _uiState.value.audios
        if (audios.isEmpty()) return
        val playhead = _uiState.value.currentPlayTime

        val targetAud = (if (!id.isNullOrEmpty()) audios.find { it.id == id } else null)?.takeIf { aud ->
            val end = aud.startSeconds + aud.durationSeconds
            playhead > (aud.startSeconds + 0.1) && playhead < (end - 0.1)
        } ?: audios.find { aud ->
            val end = aud.startSeconds + aud.durationSeconds
            playhead > (aud.startSeconds + 0.1) && playhead < (end - 0.1)
        }

        if (targetAud == null) {
            _uiState.update { it.copy(statusMessage = "Cannot split audio: Move playhead inside an audio clip") }
            return
        }

        val audIdx = audios.indexOfFirst { it.id == targetAud.id }
        if (audIdx == -1) return

        val audEnd = targetAud.startSeconds + targetAud.durationSeconds
        val dur1 = Math.round((playhead - targetAud.startSeconds) * 100.0) / 100.0
        val dur2 = Math.round((audEnd - playhead) * 100.0) / 100.0

        if (dur1 >= 0.1 && dur2 >= 0.1) {
            val part1 = targetAud.copy(durationSeconds = dur1)
            val part2 = targetAud.copy(
                id = "audio_${System.currentTimeMillis()}",
                name = "${targetAud.name} (Part 2)",
                startSeconds = playhead,
                durationSeconds = dur2
            )
            val updated = audios.toMutableList().apply {
                set(audIdx, part1)
                add(audIdx + 1, part2)
            }
            _uiState.update {
                it.copy(
                    audios = updated,
                    selectedAudioId = part2.id,
                    statusMessage = "Split audio '${targetAud.name}' at ${String.format(java.util.Locale.US, "%.1fs", playhead)}"
                )
            }
            autoSaveActiveProject()
        }
    }

    fun addAudioSampleTrack(name: String, duration: Double = 15.0) {
        val layerType = when {
            name.contains("voice", ignoreCase = true) || name.contains("speech", ignoreCase = true) -> "voiceover"
            name.contains("punch", ignoreCase = true) || name.contains("impact", ignoreCase = true) || name.contains("whoosh", ignoreCase = true) -> "sfx"
            else -> "music"
        }
        val trackIdx = when (layerType) {
            "voiceover" -> 1
            "sfx" -> 2
            else -> 0
        }
        val path = audioMixerEngine.getSampleAudioPath(name, layerType)
        val newAudio = com.example.model.AudioSegment(
            id = "audio_${System.currentTimeMillis()}",
            name = name,
            localPath = path,
            startSeconds = _uiState.value.currentPlayTime,
            durationSeconds = duration,
            volume = if (layerType == "voiceover") 0.95f else 0.85f,
            trackIndex = trackIdx,
            layerType = layerType,
            fadeInSeconds = if (layerType == "music") 0.8 else 0.1,
            fadeOutSeconds = if (layerType == "music") 1.0 else 0.3,
            isLooping = (layerType == "music"),
            isDuckingEnabled = (layerType == "music"),
            duckVolume = 0.25f
        )
        _uiState.update {
            it.copy(
                audios = it.audios + newAudio,
                selectedAudioId = newAudio.id,
                statusMessage = "Added track: $name (Layer $trackIdx)"
            )
        }
        autoSaveActiveProject()
    }

    // --- CapCut Timeline Advanced Drag & Editing Operations ---

    fun moveImage(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.images.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _uiState.update { it.copy(images = current) }
            autoSaveActiveProject()
        }
    }

    fun splitImageAtPlayhead(id: String? = null) {
        val playhead = _uiState.value.currentPlayTime
        val images = _uiState.value.images
        if (images.isEmpty()) return

        // Compute effective start and end for every image
        val trackAccumulators = mutableMapOf<Int, Double>()
        val clipTimings = mutableMapOf<String, Pair<Double, Double>>()
        for (img in images) {
            val seqAcc = trackAccumulators.getOrDefault(img.trackIndex, 0.0)
            val start = if (img.startTimeSeconds >= 0.0) img.startTimeSeconds else seqAcc
            val end = start + img.durationSeconds
            clipTimings[img.id] = Pair(start, end)
            trackAccumulators[img.trackIndex] = end
        }

        // Target clip: either specific id if provided and intersecting playhead, or the clip under playhead
        val targetImg = (if (!id.isNullOrEmpty()) images.find { it.id == id } else null)?.takeIf { clip ->
            val timing = clipTimings[clip.id] ?: return@takeIf false
            playhead > (timing.first + 0.1) && playhead < (timing.second - 0.1)
        } ?: images.find { clip ->
            val timing = clipTimings[clip.id] ?: return@find false
            playhead > (timing.first + 0.1) && playhead < (timing.second - 0.1)
        }

        if (targetImg == null) {
            _uiState.update { it.copy(statusMessage = "Cannot split scene: Move playhead inside a scene clip") }
            return
        }

        val imgIdx = images.indexOfFirst { it.id == targetImg.id }
        if (imgIdx == -1) return

        val (startT, endT) = clipTimings[targetImg.id]!!
        val splitOffset = playhead - startT
        val part1Dur = Math.round(splitOffset * 100.0) / 100.0
        val part2Dur = Math.round((targetImg.durationSeconds - splitOffset) * 100.0) / 100.0

        if (part1Dur >= 0.1 && part2Dur >= 0.1) {
            val part1 = targetImg.copy(
                durationSeconds = part1Dur,
                startTimeSeconds = if (targetImg.startTimeSeconds >= 0.0) targetImg.startTimeSeconds else -1.0
            )
            val part2 = targetImg.copy(
                id = "img_${System.currentTimeMillis()}",
                title = "${targetImg.title} (Part 2)",
                durationSeconds = part2Dur,
                startTimeSeconds = if (targetImg.startTimeSeconds >= 0.0) (targetImg.startTimeSeconds + part1Dur) else -1.0
            )
            val updated = images.toMutableList().apply {
                set(imgIdx, part1)
                add(imgIdx + 1, part2)
            }
            _uiState.update {
                it.copy(
                    images = updated,
                    selectedImageId = part2.id,
                    statusMessage = "Split scene '${targetImg.title}' at ${String.format(java.util.Locale.US, "%.1fs", playhead)}"
                )
            }
            autoSaveActiveProject()
        }
    }

    fun splitSubtitleAtPlayhead(id: String) {
        val subs = _uiState.value.subtitles
        val subIdx = subs.indexOfFirst { it.id == id }
        if (subIdx == -1) return
        val sub = subs[subIdx]
        val playhead = _uiState.value.currentPlayTime
        
        if (playhead > (sub.startSeconds + 0.3) && playhead < (sub.endSeconds - 0.3)) {
            val words = sub.text.split(" ")
            val midWord = (words.size / 2).coerceAtLeast(1)
            val text1 = words.take(midWord).joinToString(" ")
            val text2 = words.drop(midWord).joinToString(" ").ifEmpty { "..." }
            
            val part1 = sub.copy(endSeconds = playhead, text = text1)
            val part2 = sub.copy(
                id = "sub_${System.currentTimeMillis()}",
                startSeconds = playhead,
                text = text2
            )
            val updated = subs.toMutableList().apply {
                set(subIdx, part1)
                add(subIdx + 1, part2)
            }
            _uiState.update { it.copy(subtitles = updated, selectedSubtitleId = part2.id, statusMessage = "Split caption at playhead") }
            autoSaveActiveProject()
        }
    }

    fun adjustImageDurationWithDrag(id: String, newDuration: Double) {
        val clamped = (Math.round(newDuration * 10.0) / 10.0).coerceIn(0.5, 60.0)
        _uiState.update { state ->
            val updated = state.images.map {
                if (it.id == id) it.copy(durationSeconds = clamped) else it
            }
            state.copy(images = updated)
        }
        autoSaveActiveProject()
    }

    fun adjustSubtitleStartAndEnd(id: String, newStart: Double, newEnd: Double) {
        val startClamped = newStart.coerceAtLeast(0.0)
        val endClamped = newEnd.coerceAtLeast(startClamped + 0.3)
        _uiState.update { state ->
            val updated = state.subtitles.map {
                if (it.id == id) it.copy(startSeconds = startClamped, endSeconds = endClamped) else it
            }
            state.copy(subtitles = updated)
        }
        autoSaveActiveProject()
    }

    fun clearImages() {
        _uiState.update { it.copy(images = emptyList(), selectedImageId = null, statusMessage = "Cleared all image scenes") }
        autoSaveActiveProject()
    }

    fun clearSubtitles() {
        _uiState.update { it.copy(subtitles = emptyList(), selectedSubtitleId = null, statusMessage = "Cleared all subtitle tracks") }
        autoSaveActiveProject()
    }

    fun clearAudios() {
        _uiState.update { it.copy(audios = emptyList(), selectedAudioId = null, statusMessage = "Cleared all audio tracks") }
        autoSaveActiveProject()
    }

    fun exportProjectZip(onReady: (File) -> Unit) {
        viewModelScope.launch {
            val template = _uiState.value.selectedTemplate ?: return@launch
            val zip = repository.exportProjectZip(
                _uiState.value.images,
                _uiState.value.subtitles,
                template
            )
            _uiState.update { it.copy(exportedZipFile = zip, statusMessage = "Project ZIP bundled with APK: ${zip.name}") }
            onReady(zip)
        }
    }

    // --- Ultra-Fast Render Engine with Exact Duration Guarantee ---

    fun startTwoPassRender() {
        val template = _uiState.value.selectedTemplate ?: return
        val images = _uiState.value.images
        val subtitles = _uiState.value.subtitles
        val settings = _uiState.value.exportSettings

        if (images.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Please add at least one image before exporting") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeTab = 3, // switch to render tab
                    renderState = RenderState(
                        isRendering = true,
                        currentPass = 1,
                        currentStepDescription = "Starting Ultra-Fast Hardware Render (${settings.resolution} / ${settings.aspectRatio} / ${settings.frameRate}fps)...",
                        logs = listOf(
                            "[CAPCUT ULTRA-FAST ENGINE] Initializing Hardware Pipeline...",
                            "[CAPCUT ULTRA-FAST ENGINE] Resolution: ${settings.resolution} | Format: ${settings.aspectRatio} (${settings.framingMode})",
                            "[CAPCUT ULTRA-FAST ENGINE] Exact Duration Locking Active"
                        ),
                        exportSettings = settings
                    )
                )
            }

            try {
                val outputFile = renderEngine.renderTwoPassVideoWithSettings(
                    images = images,
                    subtitles = subtitles,
                    template = template,
                    exportSettings = settings,
                    audios = _uiState.value.audios,
                    onPassStart = { passNum, desc, args ->
                        _uiState.update { state ->
                            val currentLogs = state.renderState.logs
                            val pass1 = if (passNum == 1) args.toList() else state.renderState.pass1Arguments
                            val pass2 = if (passNum == 2) args.toList() else state.renderState.pass2Arguments
                            state.copy(
                                renderState = state.renderState.copy(
                                    currentPass = passNum,
                                    currentStepDescription = desc,
                                    pass1Arguments = pass1,
                                    pass2Arguments = pass2,
                                    logs = currentLogs + "[PASS $passNum] $desc"
                                )
                            )
                        }
                    },
                    onLog = { line ->
                        _uiState.update { state ->
                            state.copy(
                                renderState = state.renderState.copy(
                                    logs = state.renderState.logs.takeLast(60) + line
                                )
                            )
                        }
                    },
                    onProgress = { progressPercent ->
                        _uiState.update { state ->
                            state.copy(
                                renderState = state.renderState.copy(
                                    progressPercent = progressPercent
                                )
                            )
                        }
                    }
                )

                repository.updateProjectExportStatus(_uiState.value.currentProjectId, "EXPORTED", outputFile.absolutePath)
                _uiState.update { state ->
                    state.copy(
                        renderState = state.renderState.copy(
                            isRendering = false,
                            isSuccess = true,
                            outputVideoPath = outputFile.absolutePath,
                            currentStepDescription = "Render Completed! Video is ready in Samsung Gallery."
                        )
                    )
                }
            } catch (e: Exception) {
                repository.updateProjectExportStatus(_uiState.value.currentProjectId, "FAILED", null)
                _uiState.update { state ->
                    state.copy(
                        renderState = state.renderState.copy(
                            isRendering = false,
                            isError = true,
                            errorMessage = e.message ?: "Render failed",
                            logs = state.renderState.logs + "[FATAL ERROR] ${e.message}"
                        )
                    )
                }
            }
        }
    }

    fun saveToSamsungGallery() {
        val path = _uiState.value.renderState.outputVideoPath ?: return
        val file = File(path)
        if (file.exists()) {
            val totalDuration = _uiState.value.images.sumOf { it.durationSeconds }
            val uri = renderEngine.saveVideoToGallery(file, totalDuration)
            if (uri != null) {
                _uiState.update { it.copy(statusMessage = "Saved to Gallery / Movies / FacelessEditor! (Exact duration locked)") }
            } else {
                _uiState.update { it.copy(statusMessage = "Could not save to MediaStore") }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerEngine.release()
    }
}
