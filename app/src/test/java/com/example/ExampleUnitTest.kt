package com.example

import com.example.engine.AssSubtitleParser
import com.example.engine.FFmpegCommandBuilder
import com.example.model.AssTemplateInfo
import com.example.model.ImageSegment
import com.example.model.SubtitleSegment
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun testConcatBugFix_repeatsLastImagePath() {
        val images = listOf(
            ImageSegment("1", "img1.jpg", "/path/to/img1.jpg", 3.0, "Scene 1"),
            ImageSegment("2", "img2.jpg", "/path/to/img2.jpg", 2.5, "Scene 2")
        )
        val concatContent = FFmpegCommandBuilder.buildConcatFileContent(images)

        val lines = concatContent.trim().lines()
        // Format should be:
        // file '/path/to/img1.jpg'
        // duration 3.000
        // file '/path/to/img2.jpg'
        // duration 2.500
        // file '/path/to/img2.jpg'
        assertEquals(5, lines.size)
        assertEquals("file '/path/to/img1.jpg'", lines[0])
        assertEquals("duration 3.000", lines[1])
        assertEquals("file '/path/to/img2.jpg'", lines[2])
        assertEquals("duration 2.500", lines[3])
        // Verify critical bug fix: last image repeated at end
        assertEquals("file '/path/to/img2.jpg'", lines[4])
    }

    @Test
    fun testPass1Arguments_containEvenDimensionsAndBaselineProfile() {
        val concatFile = File("/tmp/concat.txt")
        val pass1Out = File("/tmp/pass1.mp4")
        val args = FFmpegCommandBuilder.buildPass1Arguments(concatFile, pass1Out)

        val fullCommand = args.joinToString(" ")
        assertTrue("Must force even dimensions", fullCommand.contains("scale=trunc(iw/2)*2:trunc(ih/2)*2"))
        assertTrue("Must specify libx264", fullCommand.contains("-c:v libx264"))
        assertTrue("Must specify yuv420p", fullCommand.contains("-pix_fmt yuv420p"))
        assertTrue("Must specify baseline profile", fullCommand.contains("-profile:v baseline"))
        assertTrue("Must specify level 3.0", fullCommand.contains("-level 3.0"))
        assertTrue("Must specify +faststart", fullCommand.contains("-movflags +faststart"))
    }

    @Test
    fun testPass2Arguments_burnsAssSubtitles() {
        val pass1Out = File("/tmp/pass1.mp4")
        val assFile = File("/tmp/subtitles.ass")
        val finalOut = File("/tmp/final.mp4")
        val args = FFmpegCommandBuilder.buildPass2Arguments(pass1Out, assFile, finalOut)

        val fullCommand = args.joinToString(" ")
        assertTrue("Must use subtitles filter with ASS path", fullCommand.contains("-vf subtitles=${assFile.absolutePath}"))
        assertTrue("Must specify baseline profile", fullCommand.contains("-profile:v baseline"))
        assertTrue("Must specify +faststart", fullCommand.contains("-movflags +faststart"))
    }

    @Test
    fun testAssTimestampFormatting() {
        val formatted = AssSubtitleParser.secondsToAssTime(75.42)
        assertEquals("0:01:15.42", formatted)

        val parsed = AssSubtitleParser.parseTimeToSeconds("0:01:15.42")
        assertEquals(75.42, parsed, 0.01)
    }

    @Test
    fun testStripAssTags() {
        val raw = "{\\pos(540,1500)\\fad(100,100)}THE SECRET NOBODY TALKS ABOUT"
        val clean = AssSubtitleParser.stripAssTags(raw)
        assertEquals("THE SECRET NOBODY TALKS ABOUT", clean)
    }
}
