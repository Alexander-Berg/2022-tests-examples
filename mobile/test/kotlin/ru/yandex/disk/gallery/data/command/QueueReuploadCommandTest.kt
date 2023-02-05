package ru.yandex.disk.gallery.data.command

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.gallery.data.GalleryDataTestCase.Companion.consMediaItem
import ru.yandex.disk.gallery.data.database.GalleryDao
import ru.yandex.disk.gallery.data.database.MediaItemModel
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.provider.DatabaseTransactions
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.provider.DiskUploadQueueCursor
import ru.yandex.disk.remote.ServerConstants
import ru.yandex.disk.test.DiskMatchers
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.upload.DiskUploader
import ru.yandex.disk.upload.FileQueueItem
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.utils.CursorList
import ru.yandex.disk.utils.MimeTypeUtils

class QueueReuploadCommandTest : TestCase2() {

    val eventSender = mock<EventSender>()
    val uploadQueue = mock<UploadQueue>()
    val diskUploader = mock<DiskUploader>()
    val galleryDao = mock<GalleryDao>()
    val databaseTransactions = mock<DatabaseTransactions> {
        on { runInTransaction(any()) } doAnswer { (it.getArgument(0) as () -> Unit).invoke() }
    }
    val diskDatabase = mock<DiskDatabase>()
    val command = QueueReuploadCommand(eventSender, uploadQueue, diskUploader, galleryDao, databaseTransactions)

    @Test
    fun `should reupload item`() {
        val mediaItem = createMediaItem()
        setupMediaItemsInDatabase()
        setupFileInUploadQueue()

        command.execute(QueueReuploadCommandRequest(listOf(mediaItem)))

        verify(uploadQueue).setupItemForReupload(any())
    }

    @Test
    fun `should add new item as reupload`() {
        val mediaItem = createMediaItem()
        setupMediaItemsInDatabase()
        setupFileMissInUploadQueue()

        command.execute(QueueReuploadCommandRequest(listOf(mediaItem)))

        verify(uploadQueue).addToQueue(argThat {
            destDir == ServerConstants.AUTOUPLOADING_FOLDER && isReupload
        })
    }

    @Test
    fun `should add only items in gallery database`() {
        val mediaItem = createMediaItem()
        setupMediaItemsInDatabase(2)
        setupFileInUploadQueue()

        command.execute(QueueReuploadCommandRequest(listOf(mediaItem, mediaItem, mediaItem)))

        verify(uploadQueue, times(2)).setupItemForReupload(any())
    }

    @Test
    fun `should update items status`() {
        val mediaItem = createMediaItem()
        setupMediaItemsInDatabase()
        setupFileInUploadQueue()

        command.execute(QueueReuploadCommandRequest(listOf(mediaItem)))

        verify(galleryDao).updateMediaItem(argThat { syncStatus == SyncStatuses.WAITING_FOR_UPLOAD })
    }

    @Test
    fun `should send events and start upload`() {
        val mediaItem = createMediaItem()
        setupMediaItemsInDatabase()
        setupFileInUploadQueue()

        command.execute(QueueReuploadCommandRequest(listOf(mediaItem)))

        verify(diskUploader).markQueueChanged()
        verify(eventSender).send(DiskMatchers.argOfClass(DiskEvents.AddedToUploadQueue::class.java))
        verify(diskUploader).startUpload()
    }

    @Test
    fun `should not notify and start upload if queue is not changed`() {
        val mediaItem = createMediaItem()
        setupMediaItemsInDatabase(0)
        setupFileInUploadQueue()

        command.execute(QueueReuploadCommandRequest(listOf(mediaItem)))

        verify(diskUploader, never()).markQueueChanged()
        verify(eventSender, never()).send(DiskMatchers.argOfClass(DiskEvents.AddedToUploadQueue::class.java))
        verify(diskUploader, never()).startUpload()
    }

    fun createMediaItem() = MediaItem(0, null, MimeTypeUtils.GENERIC_VIDEO_TYPE, 0, 0,
            null, SyncStatuses.MISSED_AT_SERVER, "", "", 0.0, 0.0)

    fun setupMediaItemsInDatabase(count: Int = 1) {
        val mediaItemModel = consMediaItem(
                mimeType = MimeTypeUtils.GENERIC_VIDEO_TYPE, syncStatus = SyncStatuses.MISSED_AT_SERVER)

        val list = mutableListOf<MediaItemModel>()
        for (i in 0 until count) {
            list.add(mediaItemModel)
        }
        whenever(galleryDao.queryItemsByIds(any<List<Long>>())).thenReturn(list)
    }

    fun setupFileInUploadQueue() {
        val fileItem = mock<FileQueueItem> {
            on { isFromAutoupload } doReturn true
        }
        val fileCursor = mock<DiskUploadQueueCursor> {
            on { makeItemForRow() } doReturn fileItem
            on { count } doReturn 1
        }
        val fileCursorList = CursorList(fileCursor)
        whenever(fileCursor.asCursorList()).thenReturn(fileCursorList)
        whenever(uploadQueue.findFile(any())).thenReturn(fileCursor)
    }

    fun setupFileMissInUploadQueue() {
        val emptyCursor = mock<DiskUploadQueueCursor> {
            on { count } doReturn 0
        }
        val emptyCursorList = CursorList(emptyCursor)
        whenever(emptyCursor.asCursorList()).thenReturn(emptyCursorList)
        whenever(uploadQueue.findFile(any())).thenReturn(emptyCursor)
    }
}
