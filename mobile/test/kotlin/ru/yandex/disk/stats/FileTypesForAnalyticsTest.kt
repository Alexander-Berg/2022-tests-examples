package ru.yandex.disk.stats

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.DiskItem
import ru.yandex.disk.test.AndroidTestCase2

@Config(manifest = Config.NONE)
class FileTypesForAnalyticsTest : AndroidTestCase2() {
    @Test
    fun `should be folder type`() {
        val directory = createDir()
        assertThat(FileTypesForAnalytics.getType(directory), equalTo(FileTypesForAnalytics.FOLDER))
    }

    @Test
    fun `should be video type`() {
        val types = arrayOf("avi", "mov", "mp4", "mpeg", "3gp", "wmv")
        checkTypes(types, FileTypesForAnalytics.VIDEO)
    }

    @Test
    fun `should be document type`() {
        val types = arrayOf("docx", "eml", "pdf", "ppt", "pptx", "txt", "xls", "xlsx")
        checkTypes(types, FileTypesForAnalytics.DOCUMENT)
    }

    @Test
    fun `should be photo type`() {
        val types = arrayOf("gif", "jpg", "jpeg", "png")
        checkTypes(types, FileTypesForAnalytics.PHOTO)
    }

    @Test
    fun `should be music type`() {
        val types = arrayOf("mp3", "wav", "ogg", "wma")
        checkTypes(types, FileTypesForAnalytics.MUSIC)
    }

    @Test
    fun `should be other type`() {
        val types = arrayOf("icon", "java", "kt", "xml")
        checkTypes(types, FileTypesForAnalytics.OTHER)
    }

    private fun createFile(name: String) = mock<DiskItem> {
        on { displayName } doReturn name
        on { isDir } doReturn false
    }

    private fun createDir() = mock<DiskItem> {
        on { isDir } doReturn true
    }

    private fun checkTypes(types: Array<String>, analyticsType: FileTypesForAnalytics) {
        for (type in types) {
            val file = createFile("file.$type")
            assertThat(FileTypesForAnalytics.getType(file), equalTo(analyticsType))
        }
    }
}
