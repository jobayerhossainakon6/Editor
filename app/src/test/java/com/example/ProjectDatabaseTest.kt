package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.ProjectDao
import com.example.data.db.VideoProjectEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProjectDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.projectDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveProject() = runBlocking {
        val project = VideoProjectEntity(
            id = "test_proj_1",
            name = "My Test Video",
            lastModified = 1700000000L,
            thumbnailPath = "/path/to/thumb.jpg",
            totalDurationSeconds = 12.5,
            sceneCount = 4,
            subtitleCount = 3,
            imageJsonPath = "/path/to/image_sync.json",
            textJsonPath = "/path/to/text_sync.json",
            audioJsonPath = "/path/to/audio_sync.json",
            exportStatus = "DRAFT"
        )

        dao.insertProject(project)

        val retrieved = dao.getProjectById("test_proj_1")
        assertNotNull(retrieved)
        assertEquals("My Test Video", retrieved?.name)
        assertEquals(12.5, retrieved?.totalDurationSeconds ?: 0.0, 0.01)
        assertEquals("DRAFT", retrieved?.exportStatus)

        // Test updating export status
        dao.updateExportStatus("test_proj_1", "EXPORTED", "/path/to/output.mp4")
        val updated = dao.getProjectById("test_proj_1")
        assertEquals("EXPORTED", updated?.exportStatus)
        assertEquals("/path/to/output.mp4", updated?.lastExportedPath)

        // Test deletion
        dao.deleteProjectById("test_proj_1")
        assertNull(dao.getProjectById("test_proj_1"))
    }
}
