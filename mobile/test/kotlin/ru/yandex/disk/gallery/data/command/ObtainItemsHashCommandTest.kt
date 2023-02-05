package ru.yandex.disk.gallery.data.command

import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.gallery.data.DatabasingFileHashesObtainer
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.gallery.data.GalleryFileHashesObtainer
import ru.yandex.disk.gallery.data.database.GalleryDao
import ru.yandex.disk.gallery.data.database.Header
import ru.yandex.disk.gallery.data.database.MediaHashes
import ru.yandex.disk.gallery.data.database.MediaItemModel
import ru.yandex.disk.gallery.data.provider.MediaStoreProvider
import ru.yandex.disk.test.TestEnvironment
import ru.yandex.disk.upload.AccessMediaLocationCoordinator
import ru.yandex.disk.upload.DiskUploaderTestHelper
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.upload.hash.FileHashes
import ru.yandex.disk.upload.hash.FileHashesId
import ru.yandex.disk.utils.DiskBatteryManager
import java.io.File

class ObtainItemsHashCommandTest : GalleryDataTestCase() {

    private lateinit var dao: GalleryDao

    private var queuedItem: FileHashes? = null

    private val uploadQueueMock: UploadQueue = mock { _ ->
        on { findFileHashes(any()) } doAnswer { queuedItem }
    }

    private val mediaStoreMock: MediaStoreProvider = mock()

    private val diskBatteryManager: DiskBatteryManager = mock {
        on { isAppropriateMomentForHeavyWork } doAnswer { true }
    }

    private lateinit var command: ObtainItemsHashCommand

    @Before
    override fun setUp() {
        super.setUp()

        queuedItem = null
        dao = galleryDatabase.galleryDao()

        command = ObtainItemsHashCommand(galleryDao, dataProvider, mediaStoreMock,
            GalleryFileHashesObtainer(uploadQueueMock, DatabasingFileHashesObtainer(
                DiskUploaderTestHelper.JavaSEHashCalculator(), dao)),
            diskBatteryManager, AccessMediaLocationCoordinator.Stub, mock(),
        )
    }

    @Test
    fun `should compute files md5`() {
        val ids = (1L..2L).map {
            val file = createFile("computed_$it")
            insertMediaGetId(file.path, file.lastModified(), file.length())
        }

        command.obtain()

        assertThat(ids.map(::findMd5), not(nullValue()))
    }

    @Test
    fun `should obtain md5 from upload queue`() {
        val file = createFile("uploaded")

        queuedItem = FileHashes(FileHashesId("", file.length(), file.lastModified()), "md5", "sha256")

        val id = insertMediaGetId(file.path, file.lastModified(), file.length())

        command.obtain()

        assertThat(findMd5(id), equalTo("md5"))
    }

    @Test
    fun `should obtain md5 from media hashes`() {
        val file = createFile("cached")

        galleryDao.replaceMediaHashes(MediaHashes(file.path, file.lastModified(), file.length(), "md5", "sha256"))

        val id = insertMediaGetId(file.path, file.lastModified(), file.length())

        command.obtain()

        assertThat(findMd5(id), equalTo("md5"))
    }

    @Test
    fun `should rescan lastModified mismatched file`() {
        val file = createFile("skipped")

        insertMedia(file.path, file.lastModified() - 1, file.length())

        command.obtain()
        verify(mediaStoreMock).scanFile(argThat { absolutePath == file.absolutePath })

        command.obtain()
        verifyNoMoreInteractions(mediaStoreMock)
    }

    @Test
    fun `should rescan size mismatched file`() {
        val file = createFile("size_mismatched")

        insertMedia(file.path, file.lastModified(), file.length() + 1)

        command.obtain()
        verify(mediaStoreMock).scanFile(argThat { absolutePath == file.absolutePath })

        command.obtain()
        verifyNoMoreInteractions(mediaStoreMock)
    }

    @Test
    fun `should skip not existing file`() {
        val item = dao.queryItemById(insertMediaGetId("somewhere", 1, 1))

        assertThat(command.obtainForItem(item!!), nullValue())
    }

    @Test
    fun `should recount photoslice headers`() {
        val file = createFile("recounted")
        val media = insertMedia(file.path, file.lastModified(), file.length())

        val headerId = galleryDao.insertHeader(Header(null, PhotosliceAlbumId,
            media.photosliceTime!!, media.photosliceTime!!, 100500, 0, 0))

        command.obtain()

        assertThat(galleryDao.queryHeaderById(headerId)?.count, equalTo(1))
    }

    private fun findMd5(itemId: Long): String? {
        val item = dao.queryItemById(itemId)
        assertThat(item, not(nullValue()))

        return item!!.md5
    }

    private fun createFile(fileName: String): File {
        val rootDirectory = TestEnvironment.getTestRootDirectory()
        val testDirectory = File(rootDirectory.path, "ObtainItemsHashCommandTest")

        testDirectory.mkdir()

        val file = File(testDirectory, fileName)
        file.createNewFile()
        return file
    }

    private fun insertMediaGetId(path: String, mTime: Long, size: Long): Long =
        insertMedia(path, mTime, size).id!!

    private fun insertMedia(path: String, mTime: Long, size: Long): MediaItemModel {
        val data = consMediaItem(path = path, mTime = mTime, size = size, photosliceTime = 0)

        return data.copy(id = dao.insertMediaItem(data))
    }
}
