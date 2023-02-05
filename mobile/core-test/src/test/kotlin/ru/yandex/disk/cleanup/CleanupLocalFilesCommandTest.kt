package ru.yandex.disk.cleanup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import ru.yandex.disk.autoupload.observer.MediaStoreClient
import ru.yandex.disk.cleanup.command.CleanupLocalFilesCommand
import ru.yandex.disk.cleanup.command.CleanupLocalFilesCommandRequest
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.provider.DiskContentProviderTest
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.remote.CleanupApi
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.service.CommandScheduler
import ru.yandex.disk.settings.ApplicationSettings
import ru.yandex.disk.storage.DocumentsTreeManager
import ru.yandex.disk.storage.FileDeleteProcessor
import ru.yandex.disk.upload.DiskUploaderTestHelper
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.SystemClock
import rx.Single
import rx.plugins.RxJavaHooks
import rx.schedulers.Schedulers
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

private const val TEST_FILE_SIZE: Int = 200

class CleanupLocalFilesCommandTest : DiskContentProviderTest() {

    private lateinit var cleanupCommand: CleanupLocalFilesCommand
    private val eventSender = mock(EventSender::class.java)
    private val commandScheduler = mock(CommandScheduler::class.java)
    private val remoteRepo = mock(RemoteRepo::class.java)
    private lateinit var uploadQueue: UploadQueue
    private lateinit var uploadHelper : DiskUploaderTestHelper
    private val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
    private val mediaStoreClient = mock(MediaStoreClient::class.java)
    private val appSettings = mock(ApplicationSettings::class.java)
    private val documentsTreeManager = mock(DocumentsTreeManager::class.java)
    private val fileDeleteProcessor = mock(FileDeleteProcessor::class.java)

    override fun setUp() {
        super.setUp()

        uploadHelper = DiskUploaderTestHelper(mContext)
        uploadQueue = spy(uploadHelper.uploadQueue)
        cleanupCommand = createCommand(true)
        whenever(fileDeleteProcessor.delete(any<File>())).then { invocation ->
            (invocation?.arguments?.get(0) as? File)?.delete()
            true
        }

        RxJavaHooks.setOnIOScheduler({Schedulers.immediate()})
    }

    private fun createCommand(hasPermission : Boolean) : CleanupLocalFilesCommand {
        val context = mock(Context::class.java).apply {
            whenever(checkPermission(anyString(), anyInt(), anyInt())).thenReturn(
                    PackageManager.PERMISSION_GRANTED)
            whenever(checkPermission(eq(Manifest.permission.WRITE_EXTERNAL_STORAGE), anyInt(), anyInt())).thenReturn(
                     if (hasPermission) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED)
        }
        return CleanupLocalFilesCommand(context, eventSender, commandScheduler, uploadQueue, remoteRepo, cleanupPolicy, mediaStoreClient, appSettings,
                DiskUploaderTestHelper.JavaSEHashCalculator(), documentsTreeManager, fileDeleteProcessor)
    }

    override fun tearDown() {
        RxJavaHooks.reset()
        super.tearDown()
    }

    @Test
    fun `should request storage permission`() {
        val command = createCommand(false)
        command.execute(CleanupLocalFilesCommandRequest())
        verify(eventSender).send(any<DiskEvents.CleanupNoPermissionEvent>())
        verify(appSettings).isCleanupInProgress = true
    }

    @Test
    fun `should not perform network request`() {
        val command = createCommand(true)
        command.execute(CleanupLocalFilesCommandRequest())
        verify(remoteRepo, never()).canCleanupLocalFiles(any())
    }


    @Test
    fun `should delete five local files`() {
        val filePaths  = startAutoupload(5)
        startCleanup(true)
        verify(uploadQueue).queryCleanupFiles(0, 10)
        verify(remoteRepo).canCleanupLocalFiles(any<CleanupApi.LocalFilesMeta>())
        verify(uploadQueue, times(5)).updateCleanupState(eq(DiskContract.QueueExt.CleanupState.DELETED), anyLong())
        verifyFilesDeleted(filePaths, true)
    }

    @Test
    fun `should not cleanup skipped files`() {
        val filePaths  = addSkippedFilesToAutoupload(5)
        startCleanup(true)
        verify(uploadQueue).queryCleanupFiles(0, 10)
        verify(remoteRepo, never()).canCleanupLocalFiles(any<CleanupApi.LocalFilesMeta>())
        verify(uploadQueue, never()).updateCleanupState(eq(DiskContract.QueueExt.CleanupState.DELETED), anyLong())
        verifyFilesDeleted(filePaths, false)
    }

    @Test
    fun `should update cleanup progress`() {
        val inOrder = inOrder(appSettings)
        startAutoupload(1)
        startCleanup(true)
        inOrder.verify(appSettings).isCleanupInProgress = eq(true)
        inOrder.verify(appSettings).isCleanupInProgress = eq(false)
    }

    @Test
    fun `should delete many local files`() {
        val filePaths  = startAutoupload(25)

        startCleanup(true)

        verify(uploadQueue, times(3)).queryCleanupFiles(0, 10)
        verify(remoteRepo, times(3)).canCleanupLocalFiles(any<CleanupApi.LocalFilesMeta>())
        verify(uploadQueue, times(25)).updateCleanupState(eq(DiskContract.QueueExt.CleanupState.DELETED), anyLong())
        verifyFilesDeleted(filePaths, true)
    }

    @Test
    fun `should exclude deleted files`() {
        val filePaths  = startAutoupload(25)
        for (path : String? in filePaths) {
            val file = File(path)
            assertThat(file.delete(), equalTo(true))
        }
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())

        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(25))

        val command = createCommand(true)
        command.execute(CleanupLocalFilesCommandRequest())

        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(0))
        verify(uploadQueue, times(3)).queryCleanupFiles(0, 10)
        verify(remoteRepo, never()).canCleanupLocalFiles(any<CleanupApi.LocalFilesMeta>())
    }


    @Test
    fun `should mark deleted files as checked`() {
        val filePaths  = startAutoupload(17)
        for (path : String? in filePaths) {
            val file = File(path)
            assertThat(file.delete(), equalTo(true))
        }

        startCleanup(false)

        verify(uploadQueue, times(2)).queryCleanupFiles(0, 10)
        verify(uploadQueue, times(17)).updateCleanupState(eq(DiskContract.QueueExt.CleanupState.CHECKED), anyLong())
    }

    @Test
    fun `should mark changed files as checked`() {
        val filePaths  = startAutoupload(17)
        for (path : String? in filePaths) {
            modifyFile(path!!, TEST_FILE_SIZE * 2)
        }
        startCleanup(false)
        verify(uploadQueue, times(17)).updateCleanupState(eq(DiskContract.QueueExt.CleanupState.CHECKED), anyLong())
    }

    @Test
    fun `should not delete changed files`() {
        val filePaths  = startAutoupload(29)
        for (path : String? in filePaths) {
            modifyFile(path!!, TEST_FILE_SIZE * 2)
        }

        startCleanup(true)

        verify(remoteRepo, never()).canCleanupLocalFiles(any<CleanupApi.LocalFilesMeta>())
        verifyFilesDeleted(filePaths, false)
    }

    @Test
    fun `should send result notification`() {
        startAutoupload(7)
        val argumentCaptor = argumentCaptor<DiskEvents.CleanupFinishedEvent>()
        doNothing().`when`(eventSender).send(argumentCaptor.capture())
        startCleanup(true)
        verify(eventSender, atLeastOnce()).send(any<DiskEvents.CleanupUpdateProgressEvent>())
        val event = argumentCaptor.lastValue
        assertThat(event.freeSpace, equalTo(7L * TEST_FILE_SIZE))
    }

    @Test
    fun `should send progress notification`() {
        val filePaths = startAutoupload(50)
        for (i in filePaths.indices) {
            val path = filePaths[i]!!
            if (i < 3) {
                File(path).delete()
            }
        }
        val argumentCaptor = argumentCaptor<DiskEvents.CleanupUpdateProgressEvent>()
        doNothing().whenever(eventSender).send(argumentCaptor.capture())
        startCleanup(false)
        val events = argumentCaptor.allValues
        for (event : Any in events) {
            if (event is DiskEvents.CleanupUpdateProgressEvent) {
                assertThat(event.totalFiles, greaterThanOrEqualTo(event.checkedFiles))
            }
        }
    }

    private fun startCleanup(canDelete: Boolean) {
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())
        mockRemoteRepoResponse(canDelete)
        val command = createCommand(true)
        command.execute(CleanupLocalFilesCommandRequest())
    }

    private fun modifyFile(path : String, bytesToWrite : Int) {
        val file = File(path)
        assertThat(file.exists(), equalTo(true))
        BufferedOutputStream(FileOutputStream(file)).use {
            it.write((ByteArray(bytesToWrite)))
        }
    }

    private fun mockRemoteRepoResponse(canDelete : Boolean) {
        whenever(remoteRepo.canCleanupLocalFiles(any<CleanupApi.LocalFilesMeta>())).thenAnswer {
            val arg = it.getArgument<CleanupApi.LocalFilesMeta>(0)
            Single.just(prepareMockRespose(arg.items.size, canDelete))
        }
    }

    private fun verifyFilesDeleted(files : Array<String>, deleted : Boolean) {
        for (path in files) {
            val file = File(path)
            assertThat(file.exists(), equalTo(!deleted))
        }
    }

    private fun startAutoupload(filesCount : Int) : Array<String> {
        return Array(filesCount) { i ->
            uploadHelper.queueImageToAutoUpload("file_$i")
                .also { modifyFile(it, TEST_FILE_SIZE) } // create not empty file
        }.also {
            uploadHelper.startUpload()
        }
    }

    private fun addSkippedFilesToAutoupload(filesCount : Int) : Array<String> {
        return Array(filesCount) { i ->
            uploadHelper.queueSkippedFileToAutoUpload(
                "file_$i",
                DiskContract.Queue.MediaTypeCode.IMAGE
            ).also {
                modifyFile(it, TEST_FILE_SIZE) // create not empty file
            }
        }.also {
            uploadHelper.startUpload()
        }
    }

    private fun prepareMockRespose(filesCount : Int, canDelete : Boolean) : CleanupApi.FilesStatus {
        val items = ArrayList<CleanupApi.FileStatus>(filesCount)
        for(i in 0 until filesCount) {
            items.add(CleanupApi.FileStatus(canDelete, "", 0))
        }
        return CleanupApi.FilesStatus(items)
    }

}
