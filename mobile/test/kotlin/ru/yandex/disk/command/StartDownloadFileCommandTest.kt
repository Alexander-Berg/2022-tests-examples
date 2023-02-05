package ru.yandex.disk.command

import android.database.Cursor
import android.os.Environment
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.instanceOf
import org.mockito.ArgumentMatchers.anyLong
import ru.yandex.disk.FileItem
import ru.yandex.disk.Storage
import ru.yandex.disk.commonactions.StartDownloadFileCommand
import ru.yandex.disk.commonactions.StartDownloadFileCommandRequest
import ru.yandex.disk.connectivity.NetworkState
import ru.yandex.disk.download.DownloadQueue
import ru.yandex.disk.download.DownloadQueueItem
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.Event
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskFileCursor
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.spaceutils.ByteUnit
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test

private const val MOCK_ETAG = "ETAG"
private const val MOCK_PATH = "/file"
private val MOCK_SIZE = ByteUnit.MB.toBytes(3)
private val MOCK_TYPE = DownloadQueueItem.Type.UI
private const val OFFLINE_MARK_ID = 0
private const val ETAG_ID = 1
private const val LOCAL_ETAG_ID = 2

class StartDownloadFileCommandTest : TestCase2() {

    private val storage = mock<Storage> {
        on { storagePath }.thenReturn(Environment.getExternalStorageDirectory().path)
        on { getFreeSpaceLimited(anyLong()) }.thenReturn(ByteUnit.GB.toBytes(1))
    }
    private val downloadQueue = mock<DownloadQueue>()
    private val eventLogger = EventLogger()
    private val innerCursor = mock<Cursor> {
        on { moveToPosition(0) }.thenReturn(false)
        on { getColumnIndex(DiskContract.DiskFile.ETAG) }.thenReturn(ETAG_ID)
        on { getColumnIndex(DiskContract.DiskFile.ETAG_LOCAL) }.thenReturn(LOCAL_ETAG_ID)
    }
    private val fileCursor = DiskFileCursor(innerCursor)
    private val diskDatabase = mock<DiskDatabase> {
        on { queryFileByPath(any()) }.thenReturn(fileCursor)
    }
    private val networkState = mock<NetworkState> {
        on { isConnected }.doReturn(true)
    }
    private val command = StartDownloadFileCommand(storage, downloadQueue, eventLogger, diskDatabase, networkState)

    @Test
    fun `should download file`() {
        executeCommand()

        verifyDownloadStarted()
    }

    @Test
    fun `should notify about download`() {
        executeCommand()

        verifyEventSent(DiskEvents.FileAddedToDownload::class)
    }

    @Test
    fun `should download if existing file has wrong eTag`() {
        setupFileAlreadyExist()

        executeCommand()

        verifyDownloadStarted()
    }

    @Test
    fun `should notify if already downloaded`() {
        setupFileAlreadyDownloaded()

        executeCommand()

        verifyEventSent(DiskEvents.FileAlreadyDownloaded::class)
    }

    @Test
    fun `should not download file if already downloaded`() {
        setupFileAlreadyDownloaded()

        executeCommand()

        verifyDownloadNotStarted()
    }

    @Test
    fun `should use offline file as already downloaded if sync is not possible`() {
        setupFileAlreadyDownloadedInOffline()
        setupNetworkUnavailable()

        executeCommand()

        verifyEventSent(DiskEvents.FileAlreadyDownloaded::class)
        verifyDownloadNotStarted()
    }

    @Test
    fun `should update offline file if sync is possible`() {
        setupFileAlreadyDownloadedInOffline()

        executeCommand()

        verifyDownloadStarted()
    }

    @Test
    fun `should remove file if not match eTag`() {
        setupFileAlreadyDownloaded()
        setupWrongETag()

        executeCommand()

        verify(storage).deleteFileOrFolder(argWhere<String> { it.endsWith(MOCK_PATH) })
    }

    @Test
    fun `should notify if not enough space`() {
        setupNotEnoughSpace()

        executeCommand()

        verifyEventSent(DiskEvents.NoEnoughSpaceAvailable::class)
    }

    @Test
    fun `should not download if not enough space`() {
        setupNotEnoughSpace()

        executeCommand()

        verifyDownloadNotStarted()
    }

    @Test
    fun `should download only one audio item`() {
        executeCommand(DownloadQueueItem.Type.AUDIO)

        verify(downloadQueue).removeItemsByType(DownloadQueueItem.Type.AUDIO)
        verifyDownloadStarted(DownloadQueueItem.Type.AUDIO)
    }

    private fun setupFileAlreadyDownloaded() {
        setupFileAlreadyExist()
        setupValidLocalEType()
    }

    private fun setupFileAlreadyDownloadedInOffline() {
        setupFileAlreadyExist()
        setupFileInOfflineWithWrongETag()
    }

    private fun setupFileAlreadyExist() {
        val storagePath = storage.storagePath
        val targetFile = File(storagePath + MOCK_PATH)
        targetFile.createNewFile()
    }

    private fun setupValidLocalEType() {
        whenever(innerCursor.moveToPosition(eq(0))).thenReturn(true)
        whenever(innerCursor.getString(ETAG_ID)).thenReturn(MOCK_ETAG)
        whenever(innerCursor.getString(LOCAL_ETAG_ID)).thenReturn(MOCK_ETAG)
    }

    private fun setupWrongETag() {
        setupValidLocalEType()
        whenever(innerCursor.getString(LOCAL_ETAG_ID)).thenReturn(MOCK_ETAG + "_WRONG")
    }

    private fun setupFileInOfflineWithWrongETag() {
        setupWrongETag()
        whenever(innerCursor.getInt(OFFLINE_MARK_ID)).thenReturn(FileItem.OfflineMark.MARKED.code)
    }

    private fun setupNetworkUnavailable() {
        whenever(networkState.isConnected).doReturn(false)
    }

    private fun setupNotEnoughSpace() {
        whenever(storage.getFreeSpaceLimited(anyLong())).doReturn(ByteUnit.MB.toBytes(1))
    }

    private fun executeCommand(downloadType : DownloadQueueItem.Type = MOCK_TYPE) {
        val request = StartDownloadFileCommandRequest(MOCK_PATH, MOCK_SIZE, downloadType)
        command.execute(request)
    }

    private fun verifyDownloadStarted(downloadType : DownloadQueueItem.Type = MOCK_TYPE) {
        verifyDownloadTimes(1, downloadType)
    }

    private fun verifyDownloadNotStarted(downloadType : DownloadQueueItem.Type = MOCK_TYPE) {
        verifyDownloadTimes(0, downloadType)
    }

    private fun verifyDownloadTimes(numTimes : Int, downloadType : DownloadQueueItem.Type = MOCK_TYPE) {
        verify(downloadQueue, times(numTimes)).add(eq(downloadType),
                argWhere { it.path.endsWith(MOCK_PATH) }, anyOrNull(), anyLong(), eq(MOCK_SIZE))
    }

    private fun <T : Any> verifyEventSent(clazz : KClass<T>) {
        val events = eventLogger.findAllByClass(Event::class.java)
        assertThat(events, hasItem<T>(instanceOf(clazz.java)))
    }
}
