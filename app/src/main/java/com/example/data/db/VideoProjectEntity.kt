package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ProjectRecord

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lastModified: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val totalDurationSeconds: Double = 0.0,
    val sceneCount: Int = 0,
    val subtitleCount: Int = 0,
    val imageJsonPath: String = "",
    val textJsonPath: String = "",
    val audioJsonPath: String = "",
    val exportStatus: String = "DRAFT", // "DRAFT", "RENDERING", "EXPORTED"
    val lastExportedPath: String? = null
) {
    fun toProjectRecord(): ProjectRecord {
        return ProjectRecord(
            id = id,
            name = name,
            lastModified = lastModified,
            thumbnailPath = thumbnailPath,
            totalDurationSeconds = totalDurationSeconds,
            sceneCount = sceneCount,
            subtitleCount = subtitleCount
        )
    }

    companion object {
        fun fromProjectRecord(
            record: ProjectRecord,
            imageJsonPath: String = "",
            textJsonPath: String = "",
            audioJsonPath: String = "",
            exportStatus: String = "DRAFT",
            lastExportedPath: String? = null
        ): VideoProjectEntity {
            return VideoProjectEntity(
                id = record.id,
                name = record.name,
                lastModified = record.lastModified,
                thumbnailPath = record.thumbnailPath,
                totalDurationSeconds = record.totalDurationSeconds,
                sceneCount = record.sceneCount,
                subtitleCount = record.subtitleCount,
                imageJsonPath = imageJsonPath,
                textJsonPath = textJsonPath,
                audioJsonPath = audioJsonPath,
                exportStatus = exportStatus,
                lastExportedPath = lastExportedPath
            )
        }
    }
}
