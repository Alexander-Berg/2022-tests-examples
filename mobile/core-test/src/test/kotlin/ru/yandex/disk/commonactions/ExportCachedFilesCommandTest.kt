package ru.yandex.disk.commonactions

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.FileItem
import ru.yandex.disk.Storage
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.export.ExportedFileInfo
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.spaceutils.ByteUnit
import ru.yandex.disk.util.assertHasEvent
import java.io.File

private const val TARGET_DIR = "/Downloads"
private const val FILE_PATH_1 = "/disk/code.txt"
private const val FILE_PATH_2 = "/disk/secrets.txt"
private val FILE_SIZE = ByteUnit.MB.toBytes(1)

class ExportCachedFilesCommandTest : TestCase2() {

    private val storagePath = getCacheDir().path
    private val cachedFile1 = File(storagePath, FILE_PATH_1)
    private val cachedFile2 = File(storagePath, FILE_PATH_2)
    private val targetFile1 = File(storagePath + TARGET_DIR, FILE_PATH_1)
    private val targetFile2 = File(storagePath + TARGET_DIR, FILE_PATH_2)

    private val storage = mock<Storage> {
        on { storagePath } doReturn(getCacheDir().absolutePath)
    }
    private val eventLogger = EventLogger()
    private val command = ExportCachedFilesCommand(storage, eventLogger, mock())

    override fun setUp() {
        setFreeSpaceMb(100)
    }

    override fun tearDown() {
        deleteFiles()
        super.tearDown()
    }

    @Test
    fun `should send finish if nothing to export`() {
        deleteFiles()

        command.execute(getSingleFileRequest())

        assertFileNotExported(targetFile1)
        eventLogger.assertHasEvent<DiskEvents.DownloadTaskFinished>()
    }

    @Test
    fun `should export file`() {
        createFile(cachedFile1)

        command.execute(getSingleFileRequest())

        assertFileExported(targetFile1)
    }

    @Test
    fun `should not export if storage is empty`() {
        createFile(cachedFile1)
        setFreeSpaceMb(0)

        command.execute(getSingleFileRequest())

        assertFileNotExported(targetFile1)
        eventLogger.assertHasEvent<DiskEvents.DownloadTaskFailed>()
    }

    @Test
    fun `should send progress`() {
        createFile(cachedFile1)

        command.execute(getSingleFileRequest())

        eventLogger.assertHasEvent<DiskEvents.FileDownloadProgressed>()
    }

    @Test
    fun `should export each file`() {
        createFile(cachedFile1)
        createFile(cachedFile2)

        command.execute(getMultiFileRequest())

        assertFileExported(targetFile1)
        assertFileExported(targetFile2)
        eventLogger.assertHasEvent<DiskEvents.DownloadTaskFinished>()
    }

    @Test
    fun `should export only existing files`() {
        createFile(cachedFile1)

        command.execute(getMultiFileRequest())

        assertFileExported(targetFile1)
        assertFileNotExported(targetFile2)
        eventLogger.assertHasEvent<DiskEvents.DownloadTaskFinished>()
    }

    private fun setFreeSpaceMb(space: Long) {
        whenever(storage.getFreeSpaceLimited(any(), any())).thenReturn(ByteUnit.MB.toBytes(space))
    }
    
    private fun assertFileExported(file: File) {
        assertThat(file.exists(), equalTo(true))
    }

    private fun assertFileNotExported(file: File) {
        assertThat(file.exists(), equalTo(false))
    }

    private fun getCacheDir() = RuntimeEnvironment.application.cacheDir

    private fun createFile(file: File) {
        file.parentFile.mkdirs()
        file.createNewFile()
        val stream = file.outputStream()
        val array = ByteArray(FILE_SIZE.toInt()) { 0 }
        stream.write(array)
        stream.close()
    }

    private fun deleteFiles() {
        cachedFile1.delete()
        targetFile1.delete()
        targetFile2.delete()
    }

    private fun getMultiFileRequest() : ExportCachedFilesCommandRequest {
        val dir = File(getCacheDir().path, TARGET_DIR)
        val item1 = mock<FileItem> {
            on { path } doReturn(FILE_PATH_1)
        }
        val item2 = mock<FileItem> {
            on { path } doReturn(FILE_PATH_2)
        }
        val fileInfo1 = mock<ExportedFileInfo> {
            on { item } doReturn(item1)
            on { destFile } doReturn(targetFile1)
        }
        val fileInfo2 = mock<ExportedFileInfo> {
            on { item } doReturn(item2)
            on { destFile } doReturn(targetFile2)
        }
        return ExportCachedFilesCommandRequest(listOf(fileInfo1, fileInfo2), dir)
    }

    private fun getSingleFileRequest() : ExportCachedFilesCommandRequest {
        val dir = File(getCacheDir().path, TARGET_DIR)
        val item = mock<FileItem> {
            on { path } doReturn(FILE_PATH_1)
        }
        val fileInfo = mock<ExportedFileInfo> {
            on { getItem() } doReturn(item)
            on { destFile } doReturn(targetFile1)
        }
        return ExportCachedFilesCommandRequest(listOf(fileInfo), dir)
    }
}
