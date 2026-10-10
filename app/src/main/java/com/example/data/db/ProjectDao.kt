package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM video_projects ORDER BY lastModified DESC")
    fun getAllProjectsFlow(): Flow<List<VideoProjectEntity>>

    @Query("SELECT * FROM video_projects ORDER BY lastModified DESC")
    suspend fun getAllProjects(): List<VideoProjectEntity>

    @Query("SELECT * FROM video_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): VideoProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<VideoProjectEntity>)

    @Update
    suspend fun updateProject(project: VideoProjectEntity)

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("UPDATE video_projects SET exportStatus = :status, lastExportedPath = :path WHERE id = :id")
    suspend fun updateExportStatus(id: String, status: String, path: String?)

    @Query("SELECT COUNT(*) FROM video_projects")
    suspend fun getProjectCount(): Int
}
