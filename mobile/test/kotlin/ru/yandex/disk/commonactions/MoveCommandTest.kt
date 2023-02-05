package ru.yandex.disk.commonactions

import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.yandex.disk.rest.json.Link
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import ru.yandex.disk.Credentials
import ru.yandex.disk.FileItem
import ru.yandex.disk.Mocks
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.provider.DiskDatabaseMethodTest
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.remote.exceptions.PermanentException
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.upload.StorageListProviderStub
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.FileContentAccessor
import ru.yandex.disk.util.Signal
import ru.yandex.util.Path.asPath
import rx.schedulers.Schedulers
import java.util.*

class MoveCommandTest : DiskDatabaseMethodTest() {

    private lateinit var storageTestHelper: FileContentAccessor
    private val uploadQueue = mock<UploadQueue>()
    private val remoteRepo = mock<RemoteRepo>()
    private val commandLogger = CommandLogger()

    private lateinit var command: MoveCommand
    private val eventLogger = EventLogger()
    private var requestCreateDir: Boolean = false

    override fun setUp() {
        super.setUp()
        val context = SeclusiveContext(mContext)
        Mocks.addContentProviders(context)
        val downloadQueue = TestObjectsFactory.createDownloadQueue(context)
        val applicationStorage = TestObjectsFactory.createApplicationStorage(context,
            mock(), mock(), mock(), StorageListProviderStub(), mock())
        val storage = TestObjectsFactory.createStorage(Credentials("test", 0L),
            applicationStorage, mock(), mock(), mock())
        storageTestHelper = FileContentAccessor(storage.storagePath)
        command = MoveCommand(storage, diskDb, downloadQueue, uploadQueue,
            eventLogger, remoteRepo, commandLogger, Schedulers.trampoline())
        storageTestHelper.clear()
    }

    @Test
    fun `should move file in local cache`() {
        storageTestHelper.write("/disk/A/a", "DATA")

        command.execute(createRequest(listOf("/disk/A/a"), DEST_DIR))

        assertThat(storageTestHelper.read("/disk/B/a"), equalTo("DATA"))
    }

    @Test
    fun `should move directory in local cache`() {
        storageTestHelper.write("/disk/A/a", "DATA")

        executeCommand()

        assertThat(storageTestHelper.read("/disk/B/A/a"), equalTo("DATA"))
    }

    @Test
    fun `should clean up upload queue`() {
        executeCommand()

        verify(uploadQueue).deleteUploadsByDestDir(asPath("/disk/A")!!)
    }

    @Test
    fun `should start track remote operation command if get link`() {
        whenever(remoteRepo.move(any(), any(), any())).thenReturn(Link())

        executeCommand()

        val request = commandLogger.findByClass(TrackDirectoryOperationProgressCommandRequest::class.java)
        assertThat(request, `is`(notNullValue()))
        assertThat(request.affectedPaths, equalTo(listOf("/disk", DEST_DIR)))
    }

    @Test
    fun `should not notify remote dir changed if get link`() {
        whenever(remoteRepo.move(any(), any(), any())).thenReturn(Link())

        executeCommand()

        val eventLoggerByClass = eventLogger.findByClass(DiskEvents.RemoteDirectoryChanged::class.java)
        assertThat(eventLoggerByClass, `is`(nullValue()))
    }

    @Test
    fun `should notify error if get remote execution exception`() {
        whenever(remoteRepo.move(any(), any(), any())).thenThrow(PermanentException("test"))

        executeCommand()

        val event = eventLogger.first as DiskEvents.CopyMoveFilesFinished
        assertThat(event.result, equalTo(DiskEvents.CopyMoveFilesFinished.RESULT_OPERATION_FAILED))
    }

    @Test
    fun `should move remotely all files`() {
        whenever(remoteRepo.move(any(), any(), any())).thenReturn(Link())
        val fileA = "/disk/A"
        val fileB = "/disk/B"

        command.execute(createRequest(listOf(fileA, fileB), DEST_DIR))

        verify(remoteRepo).move(eq(asPath(fileA)!!), eq(asPath(DEST_DIR)!!), any())
        verify(remoteRepo).move(eq(asPath(fileB)!!), eq(asPath(DEST_DIR)!!), any())
    }

    @Test
    fun `should notify common parent once`() {
        val fileA = "/disk/A/a"
        val fileB = "/disk/A/b"

        command.execute(createRequest(listOf(fileA, fileB), DEST_DIR))

        val events = eventLogger.findAllByClass(DiskEvents.LocalCachedFileListChanged::class.java)
        assertThat(events.size, equalTo(2))
        assertThat(events[0].dirPath, equalTo("/disk/A"))
        assertThat(events[1].dirPath, equalTo(DEST_DIR))
    }

    @Test
    fun `should create dir if required`() {
        requestCreateDir = true

        executeCommand()

        verify(remoteRepo).makeFolder(asPath(DEST_DIR)!!)
    }

    private fun executeCommand() {
        command.execute(createRequest(listOf(SRC), DEST_DIR))
    }

    private fun createRequest(paths: List<String>, dir: String): MoveCommandRequest {
        val items = ArrayList<FileItem>(paths.size)
        for (path in paths) {
            items.add(DiskItemBuilder().setPath(path).build())
        }
        return MoveCommandRequest(items, dir, requestCreateDir, Signal())
    }

    companion object {
        private const val SRC = "/disk/A"
        private const val DEST_DIR = "/disk/B"
    }
}
