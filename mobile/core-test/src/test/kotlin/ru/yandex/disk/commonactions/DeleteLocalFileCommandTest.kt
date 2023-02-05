package ru.yandex.disk.commonactions

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.FileItem
import ru.yandex.disk.Storage
import ru.yandex.disk.download.DownloadCommandRequest
import ru.yandex.disk.download.DownloadQueueItem
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.asPath
import ru.yandex.util.Path

private const val PATH = "/disk/A"

@RunWith(RobolectricTestRunner::class)
class DeleteFileLocallyCommandTest {
    private val storage = Mockito.mock(Storage::class.java)
    private val diskDatabase = Mockito.mock(DiskDatabase::class.java)
    private val eventLogger = EventLogger()
    private val downloadQueue = TestObjectsFactory.createDownloadQueue(RuntimeEnvironment.application)
    private val uploadQueue = Mockito.mock(UploadQueue::class.java)
    private val commandLogger = CommandLogger()

    val command = DeleteFileLocallyCommand(
        commandLogger,
        eventLogger,
        diskDatabase,
        downloadQueue,
        storage,
        uploadQueue
    )

    @Test
    fun `should delete from disk database`() {
        val dirA = DiskItemBuilder().setPath(PATH).setIsDir(true).build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(dirA)))

        Mockito.verify(diskDatabase).deleteByPath(PATH.asPath())
    }

    @Test
    fun `should clean up upload queue`() {
        val dirA = DiskItemBuilder().setPath(PATH).setIsDir(true).build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(dirA)))

        Mockito.verify(uploadQueue).deleteUploadsByDestDir(PATH.asPath())
    }

    @Test
    fun `should clean up Storage`() {
        val dirA = DiskItemBuilder().setPath(PATH).setIsDir(true).build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(dirA)))

        Mockito.verify(storage).deleteFileOrFolder(PATH.asPath())
    }

    @Test
    fun `should clean up download queue from dir`() {
        val fileInDirPath = Path(PATH, "file")
        downloadQueue.add(DownloadQueueItem.Type.SYNC,
            null, fileInDirPath, "/test".asPath(), 0, 0)
        val dirA = DiskItemBuilder().setPath(PATH).setIsDir(true).build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(dirA)))

        MatcherAssert.assertThat(downloadQueue.isEmpty, Matchers.equalTo(true))
        MatcherAssert.assertThat(commandLogger.allClasses, Matchers.hasItem(DownloadCommandRequest::class.java))
    }

    @Test
    fun `should clean up download queue from offline-marked file`() {
        downloadQueue.add(DownloadQueueItem.Type.SYNC,
            null, PATH.asPath(), "/test".asPath(), 0, 0)
        val offlineFile = DiskItemBuilder().setPath(PATH)
            .setOffline(FileItem.OfflineMark.MARKED)
            .build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(offlineFile)))

        MatcherAssert.assertThat(downloadQueue.isEmpty, Matchers.equalTo(true))
        MatcherAssert.assertThat(commandLogger.allClasses, Matchers.hasItem(DownloadCommandRequest::class.java))
    }

    @Test
    fun `should not start download command if file not in presented in queue`() {
        val offlineFile = DiskItemBuilder().setPath(PATH)
            .setOffline(FileItem.OfflineMark.MARKED)
            .build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(offlineFile)))

        MatcherAssert.assertThat(downloadQueue.isEmpty, Matchers.equalTo(true))
        MatcherAssert.assertThat(commandLogger.allClasses, Matchers.not(Matchers.hasItem(DownloadCommandRequest::class.java)))
    }

    @Test
    fun `should send events`() {
        val dir = DiskItemBuilder()
            .setPath("/disk/dir")
            .setIsDir(true)
            .build()
        val file = DiskItemBuilder()
            .setPath("/disk/second-dir/file")
            .build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(dir, file)))

        val event0 = eventLogger[0] as DiskEvents.LocalCachedFileListChanged
        MatcherAssert.assertThat(event0.dirPath, Matchers.equalTo("/disk"))

        val event1 = eventLogger[1] as DiskEvents.LocalCachedFileListChanged
        MatcherAssert.assertThat(event1.dirPath, Matchers.equalTo("/disk/second-dir"))

        MatcherAssert.assertThat(eventLogger[2], Matchers.instanceOf(DiskEvents.DeleteFilesLocallyCompleted::class.java))
    }

    @Test
    fun `should send unique events`() {
        val file1 = DiskItemBuilder()
            .setPath("/disk/dir/1")
            .build()
        val file2 = DiskItemBuilder()
            .setPath("/disk/dir/2")
            .build()

        command.execute(DeleteFileLocallyCommandRequest(listOf(file1, file2)))

        val events = eventLogger.findAllByClass(DiskEvents.LocalCachedFileListChanged::class.java)
        MatcherAssert.assertThat(events, Matchers.hasSize(1))
    }
}