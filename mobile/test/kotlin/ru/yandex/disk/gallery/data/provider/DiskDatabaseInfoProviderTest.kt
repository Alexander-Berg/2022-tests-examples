package ru.yandex.disk.gallery.data.provider

import org.mockito.kotlin.anyArray
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.domain.gallery.PhotosliceServerFileContentSource
import ru.yandex.disk.domain.albums.AlbumSet
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskTableCursor
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.util.Path

private val TEST_PATH = Path("dist/test/path")
private val TEST_SOURCE = PhotosliceServerFileContentSource(TEST_PATH.path, "image", "etag", "image")

class DiskDatabaseInfoProviderTest : AndroidTestCase2() {

    private val diskDb = mock<DiskDatabase>()

    private val diskDataInfoProvider = DiskDatabaseInfoProvider(diskDb)

    @Test
    fun `should provide MediaItemInformation with size from disk db`() {
        val testSize = 12345L
        val diskFile = mock<DiskTableCursor> {
            on { size } doReturn testSize
            on { albums } doReturn AlbumSet(0)
            on { moveToFirst() } doReturn true
        }
        whenever(diskDb.queryByPathFromTable(eq(TEST_PATH), anyArray())).thenReturn(diskFile)

        val information = diskDataInfoProvider.getInformation(TEST_SOURCE)

        assertThat(information.size, equalTo(testSize))
    }

    @Test
    fun `should provide MediaItemInformation without size if item not found in disk db`() {
        val diskFile = mock<DiskTableCursor> {
            on { moveToFirst() } doReturn false
        }
        whenever(diskDb.queryByPathFromTable(eq(TEST_PATH), anyArray())).thenReturn(diskFile)

        val information = diskDataInfoProvider.getInformation(TEST_SOURCE)

        assertThat(information.path, equalTo(TEST_PATH.path))
        assertThat(information.name, equalTo(TEST_PATH.name))
        assertThat(information.size, equalTo(0L))
    }
}
