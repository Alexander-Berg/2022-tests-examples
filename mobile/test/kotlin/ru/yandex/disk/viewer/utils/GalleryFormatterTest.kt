package ru.yandex.disk.viewer.utils

import android.text.style.ImageSpan
import junit.framework.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.disk.util.UITools.NON_BREAKING_SPACE
import ru.yandex.disk.viewer.util.GalleryFormatter
import kotlin.test.assertNotNull

@Ignore("MOBDISK-20597")
@Config(qualifiers = "ru")
@RunWith(RobolectricTestRunner::class)
class GalleryFormatterTest {

    @Test
    fun `check photounlim`() {
        val (path, _) = GalleryFormatter.formatFilePath(RuntimeEnvironment.application, "/photounlim/blabla")
        val divider = path!!.getSpans(0, path.length, ImageSpan::class.java)
        assertNotNull(divider)
        assertEquals("Фото$NON_BREAKING_SPACE/ Безлимит", path.toString())
    }

    @Test
    fun `check disk ru`() {
        val (path, _) = GalleryFormatter.formatFilePath(RuntimeEnvironment.application, "/disk/Фотокамера/2018-09-12 17-16-49.JPG")
        val divider = path!!.getSpans(0, path.length, ImageSpan::class.java)
        assertNotNull(divider)
        assertEquals("Фотокамера", path.toString())
    }

    @Test
    fun `check disk en`() {
        val (path, _) = GalleryFormatter.formatFilePath(RuntimeEnvironment.application, "/disk/Camera/2018-09-12 17-16-49.JPG")
        val divider = path!!.getSpans(0, path.length, ImageSpan::class.java)
        assertNotNull(divider)
        assertEquals("Camera", path.toString())
    }
}
