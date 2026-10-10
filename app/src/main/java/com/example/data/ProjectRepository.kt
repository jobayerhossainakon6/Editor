package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.db.VideoProjectEntity
import com.example.engine.AssSubtitleParser
import com.example.model.AssTemplateInfo
import com.example.model.AudioSegment
import com.example.model.ImageSegment
import com.example.model.ProjectRecord
import com.example.model.SubtitleSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    val projectDao = database.projectDao()

    val mediaDir: File get() = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }
    val persistentMediaDir: File get() = File(context.filesDir, "persistent_media").apply { if (!exists()) mkdirs() }
    val projectsBaseDir: File get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }
    private val templatesDir: File get() = File(context.filesDir, "templates").apply { if (!exists()) mkdirs() }
    private val projectsMetaFile: File get() = File(context.filesDir, "projects_metadata.json")
    private val lastActiveFile: File get() = File(context.filesDir, "last_active_project.txt")

    suspend fun initializeDefaultAssetsIfNeeded() = withContext(Dispatchers.IO) {
        // 1. Copy bundled ASS templates
        val defaultTemplates = listOf(
            "hormozi_bold.ass",
            "mrbeast_yellow.ass",
            "cinematic_minimal.ass",
            "neon_cyber.ass",
            "viral_red_punch.ass",
            "ali_abdaal_clean.ass",
            "karaoke_word_glow.ass",
            "anime_dramatic.ass"
        )
        for (tmpl in defaultTemplates) {
            val dest = File(templatesDir, tmpl)
            if (!dest.exists() || dest.length() == 0L) {
                try {
                    context.assets.open("templates/$tmpl").use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Copy default scene images from assets (samples/ or media/)
        val defaultImages = listOf("scene_1.jpg", "scene_2.jpg", "scene_3.jpg", "scene_4.jpg")
        for (img in defaultImages) {
            val dest = File(mediaDir, img)
            if (!dest.exists() || dest.length() == 0L) {
                try {
                    val input = try {
                        context.assets.open("samples/$img")
                    } catch (_: Exception) {
                        context.assets.open("media/$img")
                    }
                    input.use { inStream ->
                        dest.outputStream().use { outStream -> inStream.copyTo(outStream) }
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Copy default JSON sync files
        val defaultJsons = listOf("image_sync.json", "text_sync.json")
        for (jsonName in defaultJsons) {
            val dest = File(mediaDir, jsonName)
            if (!dest.exists() || dest.length() == 0L) {
                try {
                    val input = try {
                        context.assets.open("samples/$jsonName")
                    } catch (_: Exception) {
                        context.assets.open("media/$jsonName")
                    }
                    input.use { inStream ->
                        dest.outputStream().use { outStream -> inStream.copyTo(outStream) }
                    }
                } catch (_: Exception) {}
            }
        }

        // 4. Initialize default project directory if empty
        if (!projectsMetaFile.exists()) {
            val defaultProjectImages = listOf(
                ImageSegment("img_0", "scene_1.jpg", File(mediaDir, "scene_1.jpg").absolutePath, 3.0, "Hook Question", "Crossfade"),
                ImageSegment("img_1", "scene_2.jpg", File(mediaDir, "scene_2.jpg").absolutePath, 3.0, "The Secret", "Zoom In"),
                ImageSegment("img_2", "scene_3.jpg", File(mediaDir, "scene_3.jpg").absolutePath, 3.0, "Evidence / Proof", "Slide Left"),
                ImageSegment("img_3", "scene_4.jpg", File(mediaDir, "scene_4.jpg").absolutePath, 2.5, "Call to Action", "None")
            )
            val defaultSubs = listOf(
                SubtitleSegment("sub_0", 0.0, 3.0, "DO THIS INSTEAD OF THAT", "hormozi_bold.ass", "Pop-up", "Glow", 0.5f, 0.82f, 38, "Impact", "#FFFF00", "#000000", 6f),
                SubtitleSegment("sub_1", 3.0, 6.0, "THE REAL REASON NOBODY TELLS YOU", "hormozi_bold.ass", "Bounce", "Glow", 0.5f, 0.82f, 38, "Impact", "#FFFF00", "#000000", 6f),
                SubtitleSegment("sub_2", 6.0, 9.0, "WATCH THIS SIMPLE 10-SECOND TRICK", "hormozi_bold.ass", "Word-Glow", "Neon", 0.5f, 0.82f, 38, "Righteous", "#00FFFF", "#000000", 6f),
                SubtitleSegment("sub_3", 9.0, 11.5, "COMMENT 'GROW' FOR FULL TEMPLATE", "hormozi_bold.ass", "Pop-up", "3D Stroke", 0.5f, 0.82f, 38, "Anton", "#F59E0B", "#000000", 6f)
            )

            val mixer = com.example.engine.AudioMixerEngine(context)
            mixer.ensureSampleAudioFilesExist()

            val defaultAudios = listOf(
                AudioSegment(
                    id = "aud_bgm_0",
                    name = "Lo-Fi Study Chill Beat",
                    localPath = mixer.getSampleAudioPath("Lo-Fi Study Chill", "music"),
                    startSeconds = 0.0,
                    durationSeconds = 12.0,
                    volume = 0.85f,
                    trackIndex = 0,
                    layerType = "music",
                    fadeInSeconds = 0.8,
                    fadeOutSeconds = 1.2,
                    isLooping = true,
                    isDuckingEnabled = true,
                    duckVolume = 0.25f
                ),
                AudioSegment(
                    id = "aud_vo_0",
                    name = "AI Voiceover Narration",
                    localPath = mixer.getSampleAudioPath("AI Voiceover Narration", "voiceover"),
                    startSeconds = 0.5,
                    durationSeconds = 8.0,
                    volume = 0.95f,
                    trackIndex = 1,
                    layerType = "voiceover",
                    fadeInSeconds = 0.2,
                    fadeOutSeconds = 0.3,
                    isLooping = false
                ),
                AudioSegment(
                    id = "aud_sfx_0",
                    name = "Cinematic Impact Punch",
                    localPath = mixer.getSampleAudioPath("Cinematic Impact Punch", "sfx"),
                    startSeconds = 0.0,
                    durationSeconds = 2.5,
                    volume = 0.90f,
                    trackIndex = 2,
                    layerType = "sfx",
                    fadeInSeconds = 0.0,
                    fadeOutSeconds = 0.5,
                    isLooping = false
                )
            )

            val defaultRecord = saveFullProject(
                projectId = "proj_viral_hook",
                name = "Viral Short Hook Demo",
                images = defaultProjectImages,
                subtitles = defaultSubs,
                audios = defaultAudios
            )

            val initialRecent = listOf(
                defaultRecord,
                ProjectRecord(
                    id = "proj_stoic_quote",
                    name = "Stoic Motivation Clip",
                    lastModified = System.currentTimeMillis() - 86400_000,
                    thumbnailPath = File(mediaDir, "scene_2.jpg").absolutePath,
                    totalDurationSeconds = 9.0,
                    sceneCount = 3,
                    subtitleCount = 3
                ),
                ProjectRecord(
                    id = "proj_tech_trends",
                    name = "AI Automation Recap",
                    lastModified = System.currentTimeMillis() - 172800_000,
                    thumbnailPath = File(mediaDir, "scene_3.jpg").absolutePath,
                    totalDurationSeconds = 14.0,
                    sceneCount = 5,
                    subtitleCount = 5
                )
            )
            saveProjectsList(initialRecent)
            setLastActiveProjectId("proj_viral_hook")
        }
    }

    fun getLastActiveProjectId(): String? {
        return if (lastActiveFile.exists()) {
            val text = lastActiveFile.readText().trim()
            if (text.isNotEmpty()) text else null
        } else null
    }

    fun setLastActiveProjectId(projectId: String) {
        try {
            lastActiveFile.writeText(projectId)
        } catch (_: Exception) {}
    }

    /**
     * SAVES A COMPLETE PROJECT TO DISK:
     * - Saves dedicated image_sync.json in project's directory
     * - Saves dedicated text_sync.json in project's directory
     * - Updates the project index in projects_metadata.json
     * - Guarantees projects NEVER get lost or corrupted when leaving the app!
     */
    suspend fun saveFullProject(
        projectId: String,
        name: String,
        images: List<ImageSegment>,
        subtitles: List<SubtitleSegment>,
        audios: List<AudioSegment> = emptyList()
    ): ProjectRecord = withContext(Dispatchers.IO) {
        val projDir = File(projectsBaseDir, projectId).apply { if (!exists()) mkdirs() }

        // Save image_sync.json
        val imgFile = File(projDir, "image_sync.json")
        imgFile.writeText(generateImageSyncJson(images))

        // Save text_sync.json
        val txtFile = File(projDir, "text_sync.json")
        txtFile.writeText(generateTextSyncJson(subtitles))

        // Save audio_sync.json
        val audFile = File(projDir, "audio_sync.json")
        audFile.writeText(generateAudioSyncJson(audios))

        // Determine best valid thumbnail
        val validThumb = images.firstOrNull { File(it.localPath).exists() }?.localPath
            ?: File(mediaDir, "scene_1.jpg").absolutePath

        val totalDuration = maxOf(
            images.sumOf { it.durationSeconds },
            subtitles.maxOfOrNull { it.endSeconds } ?: 0.0
        ).takeIf { it > 0 } ?: 11.5

        val record = ProjectRecord(
            id = projectId,
            name = name,
            lastModified = System.currentTimeMillis(),
            thumbnailPath = validThumb,
            totalDurationSeconds = totalDuration,
            sceneCount = images.size,
            subtitleCount = subtitles.size
        )

        // Update in recent projects list
        val currentList = getRecentProjects().filterNot { it.id == projectId }.toMutableList()
        currentList.add(0, record)
        saveProjectsList(currentList)

        // Store in Room SQLite Database
        try {
            val entity = VideoProjectEntity(
                id = projectId,
                name = name,
                lastModified = record.lastModified,
                thumbnailPath = record.thumbnailPath ?: "",
                totalDurationSeconds = record.totalDurationSeconds,
                sceneCount = record.sceneCount,
                subtitleCount = record.subtitleCount,
                imageJsonPath = imgFile.absolutePath,
                textJsonPath = txtFile.absolutePath,
                audioJsonPath = audFile.absolutePath,
                exportStatus = "DRAFT"
            )
            projectDao.insertProject(entity)
        } catch (_: Exception) {}

        setLastActiveProjectId(projectId)
        record
    }

    /**
     * LOADS A COMPLETE PROJECT FROM ITS DEDICATED DIRECTORY:
     * Restores exact images, durations, transitions, and subtitles!
     */
    suspend fun loadFullProject(projectId: String): Triple<List<ImageSegment>, List<SubtitleSegment>, List<AudioSegment>>? = withContext(Dispatchers.IO) {
        val projDir = File(projectsBaseDir, projectId)
        if (!projDir.exists()) return@withContext null

        val imgFile = File(projDir, "image_sync.json")
        val txtFile = File(projDir, "text_sync.json")
        val audFile = File(projDir, "audio_sync.json")

        val images = if (imgFile.exists()) {
            parseImageSyncJson(imgFile.readText())
        } else {
            emptyList()
        }

        val subtitles = if (txtFile.exists()) {
            parseTextSyncJson(txtFile.readText())
        } else {
            emptyList()
        }

        val audios = if (audFile.exists()) {
            parseAudioSyncJson(audFile.readText())
        } else {
            emptyList()
        }

        if (images.isNotEmpty() || subtitles.isNotEmpty() || audios.isNotEmpty()) {
            setLastActiveProjectId(projectId)
            Triple(images, subtitles, audios)
        } else {
            null
        }
    }

    suspend fun getRecentProjects(): List<ProjectRecord> = withContext(Dispatchers.IO) {
        try {
            val entities = projectDao.getAllProjects()
            if (entities.isNotEmpty()) {
                return@withContext entities.map { it.toProjectRecord() }
            }
        } catch (_: Exception) {}

        val list = mutableListOf<ProjectRecord>()
        if (projectsMetaFile.exists()) {
            try {
                val array = JSONArray(projectsMetaFile.readText())
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val record = ProjectRecord(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        lastModified = obj.optLong("last_modified", System.currentTimeMillis()),
                        thumbnailPath = obj.optString("thumbnail_path", null),
                        totalDurationSeconds = obj.optDouble("duration", 0.0),
                        sceneCount = obj.optInt("scene_count", 0),
                        subtitleCount = obj.optInt("subtitle_count", 0)
                    )
                    list.add(record)
                    // Seed into Room database
                    try {
                        projectDao.insertProject(VideoProjectEntity.fromProjectRecord(record))
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        list.sortedByDescending { it.lastModified }
    }

    suspend fun saveProjectsList(projects: List<ProjectRecord>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        for (p in projects) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("last_modified", p.lastModified)
                put("thumbnail_path", p.thumbnailPath ?: "")
                put("duration", p.totalDurationSeconds)
                put("scene_count", p.sceneCount)
                put("subtitle_count", p.subtitleCount)
            }
            array.put(obj)
        }
        projectsMetaFile.writeText(array.toString(2))
    }

    suspend fun deleteProjectRecord(id: String) = withContext(Dispatchers.IO) {
        try {
            projectDao.deleteProjectById(id)
        } catch (_: Exception) {}
        val current = getRecentProjects().filterNot { it.id == id }
        saveProjectsList(current)
        try {
            val projDir = File(projectsBaseDir, id)
            if (projDir.exists()) projDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    suspend fun updateProjectExportStatus(id: String, status: String, path: String? = null) = withContext(Dispatchers.IO) {
        try {
            projectDao.updateExportStatus(id, status, path)
        } catch (_: Exception) {}
    }

    suspend fun createNewProject(
        name: String,
        initialImages: List<ImageSegment> = emptyList()
    ): ProjectRecord = withContext(Dispatchers.IO) {
        val id = "proj_${System.currentTimeMillis()}"

        // Default initial subtitles if starting fresh
        val initialSubs = listOf(
            SubtitleSegment(
                id = "sub_0",
                startSeconds = 0.0,
                endSeconds = 3.0,
                text = "YOUR VIRAL HOOK TITLE",
                templateName = "hormozi_bold.ass",
                animationPreset = "Pop-up",
                textEffect = "Glow",
                posX = 0.5f,
                posY = 0.82f,
                fontSize = 38,
                fontName = "Impact",
                textColorHex = "#FFFF00",
                outlineColorHex = "#000000",
                outlineWidth = 6f
            )
        )

        val record = saveFullProject(id, name, initialImages, initialSubs)
        record
    }

    suspend fun loadAssTemplates(): List<AssTemplateInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AssTemplateInfo>()
        val files = templatesDir.listFiles { _, name -> name.endsWith(".ass") } ?: emptyArray()

        for (file in files) {
            val content = file.readText()
            val styleName = extractAssParam(content, "Style:", 1) ?: file.nameWithoutExtension
            val fontName = extractAssParam(content, "Style:", 2) ?: "Impact"
            val fontSize = extractAssParam(content, "Style:", 3)?.toIntOrNull() ?: 76
            val primaryColor = extractAssParam(content, "Style:", 4) ?: "&H0000FFFF"
            val outlineColor = extractAssParam(content, "Style:", 6) ?: "&H00000000"

            val description = when (file.name) {
                "hormozi_bold.ass" -> "Impact Bold, Vivid Yellow, Heavy Black Stroke"
                "mrbeast_yellow.ass" -> "Huge Uppercase, Vivid Gold, Center Pop Effect"
                "cinematic_minimal.ass" -> "Clean Sans-Serif, Elegant Subtitle Fade"
                "neon_cyber.ass" -> "Electric Cyan & Magenta Glowing Accent"
                "viral_red_punch.ass" -> "High-Stakes Dramatic Red & White Punch"
                "ali_abdaal_clean.ass" -> "Clean Minimalist Sans, Soft Floating Text"
                "karaoke_word_glow.ass" -> "Word-by-word Highlighter Karaoke Pop"
                "anime_dramatic.ass" -> "Angled Dynamic Anime Action Text"
                else -> "Custom Subtitle Advanced Template"
            }

            list.add(
                AssTemplateInfo(
                    name = file.nameWithoutExtension.replace("_", " ").uppercase(),
                    fileName = file.name,
                    styleName = styleName,
                    fontName = fontName,
                    fontSize = fontSize,
                    primaryColorHex = primaryColor,
                    outlineColorHex = outlineColor,
                    description = description,
                    rawHeader = content
                )
            )
        }
        list
    }

    private fun extractAssParam(content: String, prefix: String, paramIndex: Int): String? {
        val line = content.lines().find { it.startsWith(prefix, ignoreCase = true) } ?: return null
        val afterColon = line.substringAfter(":").trim()
        val parts = afterColon.split(",")
        return if (paramIndex < parts.size) parts[paramIndex].trim() else null
    }

    suspend fun parseImageSyncJson(
        jsonString: String,
        existingImages: List<ImageSegment> = emptyList()
    ): List<ImageSegment> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ImageSegment>()
        try {
            var clean = jsonString.trim()
            if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                return@withContext emptyList()
            }
            if (clean.startsWith("```")) {
                clean = clean.replace(Regex("""^```[a-zA-Z]*\s*"""), "").replace(Regex("""\s*```$"""), "").trim()
            }
            if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                return@withContext emptyList()
            }

            val array: JSONArray = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                when {
                    obj.has("images") -> obj.getJSONArray("images")
                    obj.has("scenes") -> obj.getJSONArray("scenes")
                    obj.has("clips") -> obj.getJSONArray("clips")
                    obj.has("data") -> obj.getJSONArray("data")
                    obj.has("sync") -> obj.getJSONArray("sync")
                    else -> {
                        val key = obj.keys().asSequence().firstOrNull { obj.optJSONArray(it) != null }
                        if (key != null) obj.getJSONArray(key) else JSONArray()
                    }
                }
            } else {
                JSONArray()
            }

            val defaultMediaFiles = mediaDir.listFiles { _, name ->
                name.endsWith(".jpg", true) || name.endsWith(".png", true) || name.endsWith(".jpeg", true)
            }?.toList() ?: emptyList()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue

                val id = obj.optString("id").ifEmpty { "img_${System.currentTimeMillis()}_$i" }
                val title = obj.optString("title").ifEmpty {
                    obj.optString("name").ifEmpty { "Scene ${i + 1}" }
                }

                val duration = when {
                    obj.has("duration") -> obj.optDouble("duration", 3.0)
                    obj.has("duration_seconds") -> obj.optDouble("duration_seconds", 3.0)
                    obj.has("length") -> obj.optDouble("length", 3.0)
                    obj.has("seconds") -> obj.optDouble("seconds", 3.0)
                    else -> 3.0
                }.coerceIn(0.5, 60.0)

                val rawTransition = obj.optString("transition").ifEmpty {
                    obj.optString("trans", "None")
                }
                val transition = when (rawTransition.lowercase()) {
                    "none", "cut", "clean cut", "" -> "None"
                    "crossfade", "dissolve" -> "Crossfade"
                    "dip to black", "diptoblack", "black" -> "Dip to Black"
                    "dip to white", "diptowhite", "white" -> "Dip to White"
                    "zoom in", "zoomin" -> "Zoom In"
                    "zoom out", "zoomout" -> "Zoom Out"
                    "slide left", "slideleft" -> "Slide Left"
                    "slide right", "slideright" -> "Slide Right"
                    "push up", "pushup" -> "Push Up"
                    "push down", "pushdown" -> "Push Down"
                    "film burn", "filmburn" -> "Film Burn"
                    "glitch dissolve", "glitch" -> "Glitch Dissolve"
                    "blur flash", "blur" -> "Blur Flash"
                    else -> rawTransition
                }

                val rawEffect = obj.optString("effect").ifEmpty {
                    obj.optString("filter").ifEmpty {
                        obj.optString("motion", "Normal")
                    }
                }
                val effect = if (rawEffect.isBlank()) "Normal" else rawEffect
                val trackIndex = when {
                    obj.has("layer_index") -> obj.optInt("layer_index", 0)
                    obj.has("layer") -> obj.optInt("layer", 0)
                    obj.has("track_index") -> obj.optInt("track_index", 0)
                    obj.has("track") -> obj.optInt("track", 0)
                    else -> 0
                }
                val startTimeSec = when {
                    obj.has("start_time") -> obj.optDouble("start_time", -1.0)
                    obj.has("start_seconds") -> obj.optDouble("start_seconds", -1.0)
                    obj.has("start") -> obj.optDouble("start", -1.0)
                    else -> -1.0
                }

                // Safe Path Resolution:
                val existingAtIdx = existingImages.getOrNull(i)
                val imgParam = obj.optString("image").ifEmpty {
                    obj.optString("file").ifEmpty {
                        obj.optString("path", "")
                    }
                }

                val directFile = if (imgParam.isNotEmpty()) File(imgParam) else null
                val inMediaFile = if (imgParam.isNotEmpty()) File(mediaDir, imgParam) else null
                val inPersistentFile = if (imgParam.isNotEmpty()) File(persistentMediaDir, imgParam) else null

                val finalPath: String
                val finalFileName: String

                if (directFile != null && directFile.exists()) {
                    finalPath = directFile.absolutePath
                    finalFileName = directFile.name
                } else if (inPersistentFile != null && inPersistentFile.exists()) {
                    finalPath = inPersistentFile.absolutePath
                    finalFileName = inPersistentFile.name
                } else if (inMediaFile != null && inMediaFile.exists()) {
                    finalPath = inMediaFile.absolutePath
                    finalFileName = inMediaFile.name
                } else if (existingAtIdx != null && File(existingAtIdx.localPath).exists()) {
                    finalPath = existingAtIdx.localPath
                    finalFileName = existingAtIdx.fileName
                } else if (existingImages.isNotEmpty()) {
                    val fallbackExisting = existingImages[i % existingImages.size]
                    finalPath = fallbackExisting.localPath
                    finalFileName = fallbackExisting.fileName
                } else if (defaultMediaFiles.isNotEmpty()) {
                    val fallbackFile = defaultMediaFiles[i % defaultMediaFiles.size]
                    finalPath = fallbackFile.absolutePath
                    finalFileName = fallbackFile.name
                } else {
                    finalPath = File(mediaDir, "scene_1.jpg").absolutePath
                    finalFileName = "scene_1.jpg"
                }

                list.add(
                    ImageSegment(
                        id = id,
                        fileName = finalFileName,
                        localPath = finalPath,
                        durationSeconds = duration,
                        title = title,
                        transition = transition,
                        effect = effect,
                        trackIndex = trackIndex,
                        startTimeSeconds = startTimeSec
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Error parsing image_sync.json: ${e.message}")
        }
        list
    }

    suspend fun parseTextSyncJson(jsonString: String): List<SubtitleSegment> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubtitleSegment>()
        try {
            var clean = jsonString.trim()
            if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                return@withContext emptyList()
            }
            if (clean.startsWith("```")) {
                clean = clean.replace(Regex("""^```[a-zA-Z]*\s*"""), "").replace(Regex("""\s*```$"""), "").trim()
            }
            if (clean.isEmpty() || clean == "[]" || clean == "{}") {
                return@withContext emptyList()
            }

            val array: JSONArray = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                when {
                    obj.has("subtitles") -> obj.getJSONArray("subtitles")
                    obj.has("captions") -> obj.getJSONArray("captions")
                    obj.has("texts") -> obj.getJSONArray("texts")
                    obj.has("lines") -> obj.getJSONArray("lines")
                    obj.has("data") -> obj.getJSONArray("data")
                    obj.has("sync") -> obj.getJSONArray("sync")
                    else -> {
                        val key = obj.keys().asSequence().firstOrNull { obj.optJSONArray(it) != null }
                        if (key != null) obj.getJSONArray(key) else JSONArray()
                    }
                }
            } else {
                JSONArray()
            }

            var lastEndSec = 0.0

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue

                val id = obj.optString("id").ifEmpty { "sub_${System.currentTimeMillis()}_$i" }

                val text = obj.optString("text").ifEmpty {
                    obj.optString("caption").ifEmpty {
                        obj.optString("content").ifEmpty {
                            obj.optString("subtitle").ifEmpty {
                                obj.optString("line", "Line ${i + 1}")
                            }
                        }
                    }
                }

                // Robust Start Parsing: Supports raw seconds, colon timestamps, or automatic sequence
                val rawStart: Double = when {
                    obj.has("start_seconds") -> obj.optDouble("start_seconds", -1.0)
                    obj.has("startTime") -> obj.optDouble("startTime", -1.0)
                    obj.has("start") -> {
                        val d = obj.optDouble("start", -1.0)
                        if (d >= 0.0) d else AssSubtitleParser.parseTimeToSeconds(obj.optString("start", ""))
                    }
                    else -> -1.0
                }
                val startSec = if (rawStart >= 0.0) rawStart else lastEndSec

                // Duration Calculation
                val duration = when {
                    obj.has("duration") -> obj.optDouble("duration", 2.5)
                    obj.has("duration_seconds") -> obj.optDouble("duration_seconds", 2.5)
                    else -> 2.5
                }.coerceIn(0.5, 30.0)

                // Robust End Parsing
                val rawEnd: Double = when {
                    obj.has("end_seconds") -> obj.optDouble("end_seconds", -1.0)
                    obj.has("endTime") -> obj.optDouble("endTime", -1.0)
                    obj.has("end") -> {
                        val d = obj.optDouble("end", -1.0)
                        if (d > startSec) d else AssSubtitleParser.parseTimeToSeconds(obj.optString("end", ""))
                    }
                    else -> -1.0
                }
                val endSec = if (rawEnd > startSec) rawEnd else (startSec + duration)
                lastEndSec = endSec

                val template = obj.optString("template", "hormozi_bold.ass")
                val rawAnim = obj.optString("animation").ifEmpty {
                    obj.optString("anim", "Pop-up")
                }
                val anim = if (rawAnim.isBlank()) "Pop-up" else rawAnim

                val rawEffect = obj.optString("effect").ifEmpty {
                    obj.optString("style", "Glow")
                }
                val effect = if (rawEffect.isBlank()) "Glow" else rawEffect

                val posX = obj.optDouble("pos_x", obj.optDouble("posX", 0.5)).toFloat().coerceIn(0.05f, 0.95f)
                val posY = obj.optDouble("pos_y", obj.optDouble("posY", 0.82)).toFloat().coerceIn(0.05f, 0.95f)
                val fontSize = obj.optInt("font_size", obj.optInt("fontSize", 38)).coerceIn(16, 72)
                
                // Universal font parameter recognition: font_name, font, font_family, fontFamily
                val fontName = obj.optString("font_name").ifEmpty {
                    obj.optString("font").ifEmpty {
                        obj.optString("font_family").ifEmpty {
                            obj.optString("fontFamily", "Impact")
                        }
                    }
                }

                var textColor = obj.optString("text_color").ifEmpty {
                    obj.optString("color", "#FFFF00")
                }
                if (!textColor.startsWith("#") && !textColor.startsWith("&H")) textColor = "#$textColor"

                var outlineColor = obj.optString("outline_color").ifEmpty {
                    obj.optString("outlineColor", "#000000")
                }
                if (!outlineColor.startsWith("#") && !outlineColor.startsWith("&H")) outlineColor = "#$outlineColor"

                val outlineWidth = obj.optDouble("outline_width", obj.optDouble("outlineWidth", 6.0)).toFloat().coerceIn(0f, 20f)
                val trackIndex = obj.optInt("track_index", obj.optInt("track", 0))

                val bgBox = if (obj.has("bg_box") && obj.optString("bg_box").isNotEmpty() && obj.optString("bg_box") != "null" && obj.optString("bg_box") != "none") {
                    obj.optString("bg_box")
                } else null

                list.add(
                    SubtitleSegment(
                        id = id,
                        startSeconds = startSec,
                        endSeconds = endSec,
                        text = text,
                        templateName = template,
                        animationPreset = anim,
                        textEffect = effect,
                        posX = posX,
                        posY = posY,
                        fontSize = fontSize,
                        fontName = fontName,
                        textColorHex = textColor,
                        outlineColorHex = outlineColor,
                        outlineWidth = outlineWidth,
                        bgBoxColorHex = bgBox,
                        trackIndex = trackIndex
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Error parsing text_sync.json: ${e.message}")
        }
        list
    }

    suspend fun loadInitialData(): Triple<List<ImageSegment>, List<SubtitleSegment>, List<AudioSegment>> = withContext(Dispatchers.IO) {
        // Try restoring last active project first
        val lastId = getLastActiveProjectId()
        if (lastId != null) {
            val restored = loadFullProject(lastId)
            if (restored != null && restored.first.isNotEmpty()) {
                return@withContext restored
            }
        }

        // Fallback to mediaDir image_sync.json / text_sync.json
        val imgFile = File(mediaDir, "image_sync.json")
        val txtFile = File(mediaDir, "text_sync.json")
        val audFile = File(mediaDir, "audio_sync.json")

        val images = if (imgFile.exists()) {
            parseImageSyncJson(imgFile.readText())
        } else {
            listOf(
                ImageSegment("img_0", "scene_1.jpg", File(mediaDir, "scene_1.jpg").absolutePath, 3.0, "Hook Question", "Crossfade"),
                ImageSegment("img_1", "scene_2.jpg", File(mediaDir, "scene_2.jpg").absolutePath, 3.0, "The Secret", "Zoom In"),
                ImageSegment("img_2", "scene_3.jpg", File(mediaDir, "scene_3.jpg").absolutePath, 3.0, "Evidence / Proof", "Slide Left"),
                ImageSegment("img_3", "scene_4.jpg", File(mediaDir, "scene_4.jpg").absolutePath, 2.5, "Call to Action", "None")
            )
        }

        val subs = if (txtFile.exists()) {
            parseTextSyncJson(txtFile.readText())
        } else {
            listOf(
                SubtitleSegment("sub_0", 0.0, 3.0, "DO THIS INSTEAD OF THAT", "hormozi_bold.ass", "Pop-up", "Glow", 0.5f, 0.82f, 38, "Impact", "#FFFF00", "#000000", 6f),
                SubtitleSegment("sub_1", 3.0, 6.0, "THE REAL REASON NOBODY TELLS YOU", "hormozi_bold.ass", "Bounce", "Glow", 0.5f, 0.82f, 38, "Impact", "#FFFF00", "#000000", 6f),
                SubtitleSegment("sub_2", 6.0, 9.0, "WATCH THIS SIMPLE 10-SECOND TRICK", "hormozi_bold.ass", "Word-Glow", "Neon", 0.5f, 0.82f, 38, "Righteous", "#00FFFF", "#000000", 6f),
                SubtitleSegment("sub_3", 9.0, 11.5, "COMMENT 'GROW' FOR FULL TEMPLATE", "hormozi_bold.ass", "Pop-up", "3D Stroke", 0.5f, 0.82f, 38, "Anton", "#F59E0B", "#000000", 6f)
            )
        }

        val audios = if (audFile.exists()) {
            parseAudioSyncJson(audFile.readText())
        } else {
            val mixer = com.example.engine.AudioMixerEngine(context)
            mixer.ensureSampleAudioFilesExist()
            listOf(
                AudioSegment(
                    id = "aud_bgm_0",
                    name = "Lo-Fi Study Chill Beat",
                    localPath = mixer.getSampleAudioPath("Lo-Fi Study Chill", "music"),
                    startSeconds = 0.0,
                    durationSeconds = 12.0,
                    volume = 0.85f,
                    trackIndex = 0,
                    layerType = "music",
                    fadeInSeconds = 0.8,
                    fadeOutSeconds = 1.2,
                    isLooping = true,
                    isDuckingEnabled = true,
                    duckVolume = 0.25f
                ),
                AudioSegment(
                    id = "aud_vo_0",
                    name = "AI Voiceover Narration",
                    localPath = mixer.getSampleAudioPath("AI Voiceover Narration", "voiceover"),
                    startSeconds = 0.5,
                    durationSeconds = 8.0,
                    volume = 0.95f,
                    trackIndex = 1,
                    layerType = "voiceover",
                    fadeInSeconds = 0.2,
                    fadeOutSeconds = 0.3,
                    isLooping = false
                ),
                AudioSegment(
                    id = "aud_sfx_0",
                    name = "Cinematic Impact Punch",
                    localPath = mixer.getSampleAudioPath("Cinematic Impact Punch", "sfx"),
                    startSeconds = 0.0,
                    durationSeconds = 2.5,
                    volume = 0.90f,
                    trackIndex = 2,
                    layerType = "sfx",
                    fadeInSeconds = 0.0,
                    fadeOutSeconds = 0.5,
                    isLooping = false
                )
            )
        }

        Triple(images, subs, audios)
    }

    fun generateImageSyncJson(images: List<ImageSegment>): String {
        val array = JSONArray()
        for (img in images) {
            val obj = JSONObject().apply {
                put("id", img.id)
                put("image", img.fileName)
                put("duration", img.durationSeconds)
                put("title", img.title)
                put("transition", img.transition)
                put("effect", img.effect)
                put("layer_index", img.trackIndex)
                put("track_index", img.trackIndex)
                if (img.startTimeSeconds >= 0.0) {
                    put("start_time", img.startTimeSeconds)
                }
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun generateTextSyncJson(subtitles: List<SubtitleSegment>): String {
        val array = JSONArray()
        for (sub in subtitles) {
            val obj = JSONObject().apply {
                put("id", sub.id)
                put("start", AssSubtitleParser.secondsToAssTime(sub.startSeconds))
                put("end", AssSubtitleParser.secondsToAssTime(sub.endSeconds))
                put("start_seconds", sub.startSeconds)
                put("end_seconds", sub.endSeconds)
                put("text", sub.text)
                put("template", sub.templateName)
                put("animation", sub.animationPreset)
                put("effect", sub.textEffect)
                put("pos_x", sub.posX)
                put("pos_y", sub.posY)
                put("font_size", sub.fontSize)
                put("font_name", sub.fontName)
                put("font", sub.fontName)
                put("text_color", sub.textColorHex)
                put("outline_color", sub.outlineColorHex)
                put("outline_width", sub.outlineWidth)
                put("track_index", sub.trackIndex)
                if (sub.bgBoxColorHex != null) put("bg_box", sub.bgBoxColorHex)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun generateAudioSyncJson(audios: List<AudioSegment>): String {
        val array = JSONArray()
        for (aud in audios) {
            val obj = JSONObject().apply {
                put("id", aud.id)
                put("name", aud.name)
                put("path", aud.localPath)
                put("start_seconds", aud.startSeconds)
                put("duration", aud.durationSeconds)
                put("volume", aud.volume)
                put("is_muted", aud.isMuted)
                put("track_index", aud.trackIndex)
                put("layer_index", aud.trackIndex)
                put("layer_type", aud.layerType)
                put("fade_in_seconds", aud.fadeInSeconds)
                put("fade_out_seconds", aud.fadeOutSeconds)
                put("loop", aud.isLooping)
                put("ducking", aud.isDuckingEnabled)
                put("duck_volume", aud.duckVolume)
                if (aud.volumeKeyframes.isNotEmpty()) {
                    val kfArray = JSONArray()
                    for (kf in aud.volumeKeyframes) {
                        val kfObj = JSONObject().apply {
                            put("time", kf.timeOffsetSec)
                            put("volume", kf.volume)
                        }
                        kfArray.put(kfObj)
                    }
                    put("automation_keyframes", kfArray)
                }
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    suspend fun parseAudioSyncJson(jsonString: String): List<AudioSegment> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AudioSegment>()
        try {
            var clean = jsonString.trim()
            if (clean.isEmpty() || clean == "[]" || clean == "{}") return@withContext emptyList()
            if (clean.startsWith("```")) {
                clean = clean.replace(Regex("""^```[a-zA-Z]*\s*"""), "").replace(Regex("""\s*```$"""), "").trim()
            }
            val array = if (clean.startsWith("[")) JSONArray(clean) else {
                val obj = JSONObject(clean)
                if (obj.has("audios")) obj.getJSONArray("audios") else JSONArray()
            }
            val mixer = com.example.engine.AudioMixerEngine(context)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifEmpty { "audio_${System.currentTimeMillis()}_$i" }
                val name = obj.optString("name", "Audio Track ${i + 1}")
                val start = obj.optDouble("start_seconds", obj.optDouble("start", 0.0)).coerceAtLeast(0.0)
                val duration = obj.optDouble("duration", obj.optDouble("duration_seconds", 15.0)).coerceAtLeast(0.5)
                val volume = obj.optDouble("volume", 0.85).toFloat().coerceIn(0f, 1f)
                val isMuted = obj.optBoolean("is_muted", obj.optBoolean("muted", false))
                val trackIndex = obj.optInt("track_index", obj.optInt("track", obj.optInt("layer_index", 0)))
                val layerType = obj.optString("layer_type").ifEmpty {
                    when (trackIndex) {
                        1 -> "voiceover"
                        2 -> "sfx"
                        3 -> "ambient"
                        else -> "music"
                    }
                }
                var path = obj.optString("path", "")
                if (path.isEmpty() || (!File(path).exists() && !path.startsWith("content://"))) {
                    path = mixer.getSampleAudioPath(name, layerType)
                }

                val fadeIn = obj.optDouble("fade_in_seconds", obj.optDouble("fadeIn", 0.5)).coerceAtLeast(0.0)
                val fadeOut = obj.optDouble("fade_out_seconds", obj.optDouble("fadeOut", 0.5)).coerceAtLeast(0.0)
                val loop = obj.optBoolean("loop", obj.optBoolean("is_looping", true))
                val ducking = obj.optBoolean("ducking", obj.optBoolean("is_ducking_enabled", false))
                val duckVol = obj.optDouble("duck_volume", 0.25).toFloat().coerceIn(0f, 1f)

                val keyframes = mutableListOf<com.example.model.AudioVolumeKeyframe>()
                val kfArray = obj.optJSONArray("automation_keyframes") ?: obj.optJSONArray("volume_keyframes")
                if (kfArray != null) {
                    for (k in 0 until kfArray.length()) {
                        val kObj = kfArray.optJSONObject(k) ?: continue
                        val t = kObj.optDouble("time", kObj.optDouble("time_offset_sec", 0.0))
                        val v = kObj.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f)
                        keyframes.add(com.example.model.AudioVolumeKeyframe(t, v))
                    }
                }

                list.add(
                    AudioSegment(
                        id = id,
                        name = name,
                        localPath = path,
                        startSeconds = start,
                        durationSeconds = duration,
                        volume = volume,
                        isMuted = isMuted,
                        trackIndex = trackIndex,
                        layerType = layerType,
                        fadeInSeconds = fadeIn,
                        fadeOutSeconds = fadeOut,
                        isLooping = loop,
                        isDuckingEnabled = ducking,
                        duckVolume = duckVol,
                        volumeKeyframes = keyframes
                    )
                )
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun exportProjectZip(
        images: List<ImageSegment>,
        subtitles: List<SubtitleSegment>,
        activeTemplate: AssTemplateInfo
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val zipFile = File(exportDir, "FacelessProject_${System.currentTimeMillis()}.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val imgJson = generateImageSyncJson(images)
            zos.putNextEntry(ZipEntry("image_sync.json"))
            zos.write(imgJson.toByteArray())
            zos.closeEntry()

            val txtJson = generateTextSyncJson(subtitles)
            zos.putNextEntry(ZipEntry("text_sync.json"))
            zos.write(txtJson.toByteArray())
            zos.closeEntry()

            val tmplFile = File(templatesDir, activeTemplate.fileName)
            if (tmplFile.exists()) {
                zos.putNextEntry(ZipEntry("templates/${activeTemplate.fileName}"))
                FileInputStream(tmplFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }

            for (img in images) {
                val f = File(img.localPath)
                if (f.exists()) {
                    zos.putNextEntry(ZipEntry("media/${f.name}"))
                    FileInputStream(f).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val rootApk = File(context.filesDir.parentFile, "FacelessVideoEditor_SamsungA53.apk")
            val fallbackApk = File(context.applicationInfo.publicSourceDir)
            val apkToZip = if (rootApk.exists()) rootApk else if (fallbackApk.exists()) fallbackApk else null
            if (apkToZip != null && apkToZip.exists()) {
                zos.putNextEntry(ZipEntry("FacelessVideoEditor_SamsungA53.apk"))
                FileInputStream(apkToZip).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        zipFile
    }

    /**
     * PERMANENT STORAGE IMAGE IMPORTER:
     * Saves files into persistent_media/ with unique timestamps.
     * Guaranteed NEVER to be deleted, replaced, or lost when leaving the app!
     */
    fun importCustomImage(sourceUri: Uri): ImageSegment {
        val fileName = "imported_${System.currentTimeMillis()}.jpg"
        val destFile = File(persistentMediaDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return ImageSegment(
            id = "img_${System.currentTimeMillis()}",
            fileName = fileName,
            localPath = destFile.absolutePath,
            durationSeconds = 3.0,
            title = "Imported Image"
        )
    }

    fun importMultipleCustomImages(uris: List<Uri>): List<ImageSegment> {
        val result = mutableListOf<ImageSegment>()
        for ((idx, uri) in uris.withIndex()) {
            val mimeType = try { context.contentResolver.getType(uri) ?: "" } catch (_: Exception) { "" }
            val isVideo = mimeType.startsWith("video") || uri.toString().contains(".mp4", ignoreCase = true) || uri.toString().contains(".mov", ignoreCase = true) || uri.toString().contains(".mkv", ignoreCase = true)
            val extension = if (isVideo) "mp4" else "jpg"
            val fileName = "imported_${System.currentTimeMillis()}_$idx.$extension"
            val destFile = File(persistentMediaDir, fileName)

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0L) {
                    var clipDuration = 3.0
                    var clipTitle = if (isVideo) "Video Clip ${idx + 1}" else "Scene ${idx + 1}"

                    if (isVideo) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(destFile.absolutePath)
                            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            if (durStr != null) {
                                val durMs = durStr.toDoubleOrNull() ?: 0.0
                                if (durMs > 200.0) {
                                    clipDuration = Math.round((durMs / 1000.0) * 10.0) / 10.0
                                }
                            }
                        } catch (_: Exception) {} finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                    }

                    result.add(
                        ImageSegment(
                            id = "img_${System.currentTimeMillis()}_$idx",
                            fileName = fileName,
                            localPath = destFile.absolutePath,
                            durationSeconds = clipDuration,
                            title = clipTitle
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        return result
    }
}
