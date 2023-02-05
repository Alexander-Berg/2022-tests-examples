package ru.yandex.disk.upload

import android.content.ContentValues
import android.database.Cursor
import android.os.Environment
import android.os.RemoteException
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Ignore
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.Credentials
import ru.yandex.disk.TypedPreviewable
import ru.yandex.disk.autoupload.observer.StorageListProvider
import ru.yandex.disk.cleanup.CleanupPolicy
import ru.yandex.disk.provider.DiskContentProviderTest
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.provider.DiskContract.Queue.MediaTypeCode
import ru.yandex.disk.provider.UploadInfo
import ru.yandex.disk.remote.ServerConstants
import ru.yandex.disk.replication.NeighborsContentProviderClient
import ru.yandex.disk.replication.SelfContentProviderClient
import ru.yandex.disk.sql.SQLVocabulary.SELECT_ALL_FROM
import ru.yandex.disk.sql.SQLiteDatabase2
import ru.yandex.disk.test.TestEnvironment.getTestRootDirectory
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.test.TestSQLiteOpenHelper2
import ru.yandex.disk.util.Arrays2.asStringArray
import ru.yandex.disk.util.MediaTypes
import ru.yandex.disk.util.SystemClock
import ru.yandex.disk.util.asPath
import ru.yandex.util.Path
import java.io.File
import java.io.FileOutputStream

private const val IMG1_JPG = "IMG1.jpg"
private const val IMG2_JPG = "IMG2.jpg"
private const val IMG3_JPG = "IMG3.jpg"
private const val VIDEO1_MOVIE = "VIDEO1.movie"
private const val VIDEO2_MOVIE = "VIDEO2.movie"
private const val VIDEO3_MOVIE = "VIDEO3.movie"
private const val VIDEO4_MOVIE = "VIDEO4.movie"
private const val FORCE_UPLOAD_VIDEO_MOVIE = "forceUploadVIDEO.movie"
private const val FORCE_UPLOAD_IMG1_JPG = "forceUploadIMG1.jpg"
private const val FORCE_UPLOAD_IMG2_JPG = "forceUploadIMG2.jpg"
private const val TEST_DEST_DIR = "CONSTANT_TEST"
private const val TEST_ETIME = 1111L
private const val TEST_SHA_256_SUM = "TEST_SHA256_SUM"
private const val TEST_MD5_SUM = "TEST_MD5_SUM"
private const val TEST_FILE_SIZE: Long = 200
private const val TEST_QUEUED_TIME = 100L
private const val TEST_UPLOAD_START_TIME = 200L
private const val TEST_UPLOADED_TIME = 300L


@Config(manifest = Config.NONE)
class UploadQueueTest : DiskContentProviderTest() {

    private val storageListProvider = mock<StorageListProvider>()

    private lateinit var uploadQueue: UploadQueue
    private lateinit var sqlite: TestSQLiteOpenHelper2
    private lateinit var neighborsClient: NeighborsContentProviderClient
    private lateinit var selfClient: SelfContentProviderClient
    private lateinit var db: SQLiteDatabase2

    public override fun setUp() {
        super.setUp()
        val mockCreds = TestObjectsFactory.createCredentials()
        sqlite = TestObjectsFactory.createSqlite(mockContext) as TestSQLiteOpenHelper2
        selfClient = TestObjectsFactory.createSelfContentProviderClient(mockContext, sqlite)
        neighborsClient = TestObjectsFactory.createNeighborsProviderClient(mockContext)
        uploadQueue = TestObjectsFactory.createUploadQueue(neighborsClient, selfClient,
            sqlite, mockContentResolver, storageListProvider, mockCreds)
        db = sqlite.writableDatabase
    }

    public override fun tearDown() {
        sqlite.close()
        super.tearDown()
    }

    @Test
    fun shouldSortPhotoBeforeVideoForAutoupload() {
        queueImageToAutoUpload(IMG1_JPG, 1000)
        queueVideoToAutoUpload(VIDEO1_MOVIE, 2000)
        queueImageToAutoUpload(IMG2_JPG, 3000)
        queueVideoToAutoUpload(VIDEO2_MOVIE, 4000)
        queueImageToAutoUpload(IMG3_JPG, 5000)

        val fileQueueItems = uploadQueue.queryFileListToUpload()

        assertThat(fileQueueItems[0].displayName, equalTo(IMG3_JPG))
        assertThat(fileQueueItems[1].displayName, equalTo(IMG2_JPG))
        assertThat(fileQueueItems[2].displayName, equalTo(IMG1_JPG))
        assertThat(fileQueueItems[3].displayName, equalTo(VIDEO2_MOVIE))
        assertThat(fileQueueItems[4].displayName, equalTo(VIDEO1_MOVIE))
    }

    @Test
    fun shouldIgnoreDirectories() {
        queueImageToAutoUpload(IMG1_JPG, 1000)

        uploadQueue.addToDiskQueue(createDirItemToQueue())

        assertThat(uploadQueue.queryFileListToUpload(), hasSize(1))
        assertThat(uploadQueue.queryUploadItemsCount(true), equalTo(1))

        assertThat(uploadQueue.queryFolderListToUpload(), hasSize(1))
    }

    @Test
    fun shouldSortUserUploadsBeforeAutoupload() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        queueVideoToAutoUpload(VIDEO1_MOVIE, 1000)
        queueImageToAutoUpload(IMG2_JPG, 2000)

        queueImageToUpload(FORCE_UPLOAD_IMG1_JPG, 3000)

        queueVideoToAutoUpload(VIDEO2_MOVIE, 4000)
        queueImageToUpload(FORCE_UPLOAD_IMG2_JPG, 5000)
        queueImageToAutoUpload(IMG3_JPG, 6000)

        queueVideoToUpload(FORCE_UPLOAD_VIDEO_MOVIE, 7000)

        queueVideoToAutoUpload(VIDEO3_MOVIE, 8000)
        queueVideoToAutoUpload(VIDEO4_MOVIE, 9000)

        val fileQueueItems = uploadQueue.queryFileListToUpload()

        assertThat(fileQueueItems[0].displayName, equalTo(FORCE_UPLOAD_VIDEO_MOVIE))
        assertThat(fileQueueItems[1].displayName, equalTo(FORCE_UPLOAD_IMG2_JPG))
        assertThat(fileQueueItems[2].displayName, equalTo(FORCE_UPLOAD_IMG1_JPG))
        assertThat(fileQueueItems[3].displayName, equalTo(IMG3_JPG))
        assertThat(fileQueueItems[4].displayName, equalTo(IMG2_JPG))
        assertThat(fileQueueItems[5].displayName, equalTo(IMG1_JPG))
        assertThat(fileQueueItems[6].displayName, equalTo(VIDEO4_MOVIE))
        assertThat(fileQueueItems[7].displayName, equalTo(VIDEO3_MOVIE))
        assertThat(fileQueueItems[8].displayName, equalTo(VIDEO2_MOVIE))
        assertThat(fileQueueItems[9].displayName, equalTo(VIDEO1_MOVIE))
    }

    @Test
    @Ignore //todo fix me
    fun shouldUploadUsersFilesIgnoreMimeType() {
        queueImageToUpload(IMG1_JPG, 0)
        queueImageToUpload(IMG2_JPG, 1000)
        queueVideoToUpload(VIDEO1_MOVIE, 2000)
        queueImageToUpload(IMG3_JPG, 3000)
        queueVideoToUpload(VIDEO2_MOVIE, 4000)

        val fileQueueItems = uploadQueue.queryFileListToUpload()

        assertThat(fileQueueItems[0].displayName, equalTo(VIDEO2_MOVIE))
        assertThat(fileQueueItems[1].displayName, equalTo(IMG3_JPG))
        assertThat(fileQueueItems[2].displayName, equalTo(VIDEO1_MOVIE))
        assertThat(fileQueueItems[3].displayName, equalTo(IMG2_JPG))
        assertThat(fileQueueItems[4].displayName, equalTo(IMG1_JPG))
    }

    @Test
    fun shouldQueryUploadItemByIdWithProperType() {
        queueImageToUpload(IMG1_JPG, 0)

        val fileQueueItem = pickFirstFileInQueue()
        assertThat(fileQueueItem.uploadItemType, equalTo(DiskContract.Queue.UploadItemType.DEFAULT))
    }

    @Test
    fun shouldQueryAutouploadItemByIdWithProperType() {
        queueImageToAutoUpload(IMG1_JPG, 0)

        val fileQueueItem = pickFirstFileInQueue()
        assertThat(fileQueueItem.uploadItemType, equalTo(DiskContract.Queue.UploadItemType.AUTOUPLOAD))
    }

    @Test
    fun shouldDeleteUploadByDir() {
        queueImageToUpload(IMG1_JPG, 0)
        val items = uploadQueue.queryFileListToUpload()
        assertThat(items.size, equalTo(1))

        uploadQueue.deleteUploadsByDestDir(TEST_DEST_DIR.asPath())

        val afterDeleteItems = uploadQueue.queryFileListToUpload()
        assertThat(afterDeleteItems.size, equalTo(0))
    }

    @Test
    fun shouldClearDoneQueueItems() {
        queueImageToUpload(IMG1_JPG, 0)
        val items = uploadQueue.queryFileListToUpload()
        val item = items[0]
        val id = CompositeQueueId(DiskContract.Queue.UploadItemType.DEFAULT, item.id)
        uploadQueue.updateState(DiskContract.Queue.State.UPLOADED, item)

        assertNotNull(uploadQueue.queryItemById(id))

        uploadQueue.cleanDoneQueueItems()

        assertNull(uploadQueue.queryItemById(id))
    }

    @Test
    fun shouldResumePausedAutouploads() {
        queueImageToAutoUpload(IMG1_JPG, 0)

        val items = uploadQueue.queryFileListToUpload()
        val item = items[0]
        uploadQueue.updateState(DiskContract.Queue.State.PAUSED, item)

        val id = CompositeQueueId(DiskContract.Queue.UploadItemType.AUTOUPLOAD, item.id)
        assertThat(uploadQueue.queryItemById(id)?.transferState, equalTo(DiskContract.Queue.State.PAUSED))

        uploadQueue.resumePausedAutouploadsIfNeeded()

        assertThat(uploadQueue.queryItemById(id)?.transferState, equalTo(DiskContract.Queue.State.IN_QUEUE))
    }

    @Test
    fun shouldResumePausedByPermissionDenialUploads() {
        queueImageToUpload(IMG1_JPG, 0)
        val items = uploadQueue.queryFileListToUpload()
        val item = items[0]

        uploadQueue.pauseFileInQueue(item, DiskContract.Queue.ErrorReason.PERMISSION_DENIAL)

        val id = CompositeQueueId(DiskContract.Queue.UploadItemType.AUTOUPLOAD,
            item.id)

        val itemBeforeResume = uploadQueue.queryItemById(id)
        assertThat(itemBeforeResume?.transferState, equalTo(DiskContract.Queue.State.PAUSED))
        assertThat(itemBeforeResume?.errorReason, equalTo(DiskContract.Queue.ErrorReason.PERMISSION_DENIAL))

        uploadQueue.resumePausedByPermissionDenialUploads()

        assertThat(uploadQueue.queryItemById(id)?.transferState, equalTo(DiskContract.Queue.State.IN_QUEUE))
    }

    @Test
    fun shouldDeleteFileItemFromQueue() {
        queueImageToUpload(IMG1_JPG, 0)
        val items = uploadQueue.queryFileListToUpload()

        uploadQueue.delete(items[0])

        assertThat(uploadQueue.queryFileListToUpload().size, equalTo(0))
    }

    @Test
    fun shouldUpdateItemProgress() {
        queueImageToUpload(IMG1_JPG, 0)
        val item = pickFirstFileInQueue()

        val loaded: Long = 75
        uploadQueue.updateProgress(loaded, item)

        val itemAfterUpdate = uploadQueue.queryFileListToUpload()[0]
        assertThat(itemAfterUpdate.transferProgress, equalTo(loaded))
    }

    @Test
    fun shouldUpdateItemInfo() {
        queueImageToAutoUpload(IMG1_JPG, 0)

        val fileQueueItem = pickFirstFileInQueue()
        fileQueueItem.md5sum = TEST_MD5_SUM
        fileQueueItem.sha256 = TEST_SHA_256_SUM
        fileQueueItem.etime = TEST_ETIME
        fileQueueItem.md5time = TEST_ETIME

        uploadQueue.update(fileQueueItem)

        val itemAfterUpdate = pickFirstFileInQueue()

        assertThat(itemAfterUpdate.md5sum, equalTo(TEST_MD5_SUM))
        assertThat(itemAfterUpdate.sha256, equalTo(TEST_SHA_256_SUM))
        assertThat(itemAfterUpdate.etime, equalTo(TEST_ETIME))
        assertThat(itemAfterUpdate.md5time, equalTo(TEST_ETIME))
    }

    @Test
    fun shouldMarkItemAsDone() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        val item = pickFirstFileInQueue()

        uploadQueue.markAsDone(item, item.destPath, TEST_UPLOADED_TIME)

        val fileAfterUpdate = uploadQueue.queryItemById(CompositeQueueId(DiskContract.Queue.UploadItemType.AUTOUPLOAD, item.id))

        assertThat(fileAfterUpdate?.transferState, equalTo(DiskContract.Queue.State.UPLOADED))
    }

    @Test
    fun shouldPauseAllFilesInQueue() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        queueImageToUpload(IMG2_JPG, 0)

        uploadQueue.pauseAllFilesInQueue()

        for (item in uploadQueue.queryFileListToUpload()) {
            assertThat(item.transferState, equalTo(DiskContract.Queue.State.PAUSED))
        }
    }

    @Test
    fun shouldResumeAllFilesInQueue() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        queueImageToUpload(IMG2_JPG, 0)
        val itemIds = uploadQueue.queryFileListToUpload()
            .map { CompositeQueueId(it.uploadItemType, it.id) }
        uploadQueue.pauseAllFilesInQueue()

        uploadQueue.resumeAllPausedUploads()

        itemIds.forEach {
            assertThat(uploadQueue.queryItemById(it)?.transferState, equalTo(DiskContract.Queue.State.IN_QUEUE))
        }
    }

    @Test
    fun shouldRemoveAllNonAutouploads() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        queueImageToUpload(IMG2_JPG, 0)

        uploadQueue.removeNonAutouploads()

        val items = uploadQueue.queryFileListToUpload()
        assertThat(items.size, equalTo(1))
    }

    @Test
    fun shouldQueryFirsFileToUpload() {
        queueImageToAutoUpload(IMG1_JPG, 1000)
        queueVideoToAutoUpload(VIDEO1_MOVIE, 2000)

        val fileQueueItem = uploadQueue.queryFirstFileListToUpload(listOf())
        assertThat(fileQueueItem?.displayName, equalTo(IMG1_JPG))
    }

    @Test
    fun shouldCheckIfQueueContainsUploadedFile() {
        queueImageToUpload(IMG1_JPG, 0)
        val item = pickFirstFileInQueue()

        assertTrue(uploadQueue.contains(item))
    }

    @Test
    fun autouploadedFileMustNotBeInQueue() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        val item = pickFirstFileInQueue()
        assertFalse(uploadQueue.isInQueue(CompositeQueueId.from(item)))
    }

    @Test
    fun shouldQueryNextUploadItem() {
        queueImageToUpload(IMG2_JPG, 0)

        val uploadItem = uploadQueue.queryNextUploadItem(false)
        assertThat(uploadItem?.displayName, equalTo(IMG2_JPG))
        assertThat(uploadItem?.uploadItemType, equalTo(DiskContract.Queue.UploadItemType.DEFAULT))
    }

    @Test
    fun shouldQueryNextAutouploadItem() {
        queueImageToAutoUpload(IMG1_JPG, 0)

        val autouploadItem = uploadQueue.queryNextUploadItem(true)
        assertThat(autouploadItem?.displayName, equalTo(IMG1_JPG))
        assertThat(autouploadItem?.uploadItemType, equalTo(DiskContract.Queue.UploadItemType.AUTOUPLOAD))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun shouldThrowOnUnsupportedItemRequested() {
        uploadQueue.queryItemById(CompositeQueueId(10, 1))
    }

    @Test
    fun mustQueryTypedPreviewablesRight() {
        val imagePath = queueImageToAutoUpload(IMG1_JPG, 0).srcName
        val videoPath = queueVideoToUpload(VIDEO1_MOVIE, 2000).srcName

        val typedPreviewables = uploadQueue.queryFileListToUploadPreviews()
        assertThat(typedPreviewables.size, equalTo(2))

        assertPreviewable(videoPath, typedPreviewables[0],
            DiskContract.Queue.UploadItemType.DEFAULT, MediaTypes.VIDEO)
        assertPreviewable(imagePath, typedPreviewables[1],
            DiskContract.Queue.UploadItemType.AUTOUPLOAD, MediaTypes.IMAGE)

    }

    @Test
    fun shouldNotQueryUploadedFilesToPreviews() {
        val uploadedImagePath = queueImageToAutoUpload(IMG1_JPG, 0).srcName
        val pausedImagePath = queueImageToAutoUpload(IMG2_JPG, 1000).srcName
        queueVideoToUpload(VIDEO1_MOVIE, 2000)

        val items = uploadQueue.queryFileListToUpload()
        assertThat(items, hasSize(3))
        uploadQueue.markAsDone(items[0], items[0].destPath, TEST_UPLOADED_TIME)
        uploadQueue.pauseFileInQueue(items[1], DiskContract.Queue.ErrorReason.NONE)

        val previewables = uploadQueue.queryFileListToUploadPreviews()
        assertThat(previewables, hasSize(2))

        assertPreviewable(pausedImagePath, previewables[0],
            DiskContract.Queue.UploadItemType.AUTOUPLOAD, MediaTypes.IMAGE)

        assertPreviewable(uploadedImagePath, previewables[1],
            DiskContract.Queue.UploadItemType.AUTOUPLOAD, MediaTypes.IMAGE)
    }

    private fun assertPreviewable(path: String, previewable: TypedPreviewable,
                                  type: Int, mediaType: String) {
        assertThat(previewable.path, equalTo(path))
        assertThat(previewable.uploadItemType, equalTo(type))
        assertThat(previewable.mediaType, equalTo(mediaType))

        assertNull(previewable.eTag)
        assertFalse(previewable.hasThumbnail)
    }

    @Test
    fun shouldFilterFilesInUnmountedCards() {
        val path = queueImageToAutoUpload(IMG1_JPG, 0).srcName
        val cadrPath = path.substring(0, path.lastIndexOf('/'))
        val storageInfo = StorageListProvider.StorageInfo(File(cadrPath), Environment.MEDIA_UNMOUNTED, true)

        whenever(storageListProvider.storageList).thenReturn(listOf(storageInfo))

        assertFalse(uploadQueue.checkUploadsRemain(true, false))
    }

    @Test
    fun shouldAddCleanupStateAndUploadPath() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        val item = pickFirstFileInQueue()

        uploadQueue.markAsDone(item, item.destPath, TEST_UPLOADED_TIME)
        val cursor = sqlite.readableDatabase.query(DiskContract.QueueExt.TABLE_EXT, null, DiskContract.QueueExt.UPLOAD_ID + " = ?",
            asStringArray(item.id), null, null, null)

        assertTrue(cursor.moveToFirst())

        val uploadIdIndex = cursor.getColumnIndex(DiskContract.QueueExt.UPLOAD_ID)
        val uploadId = cursor.getLong(uploadIdIndex)
        assertThat(uploadId, equalTo(item.id))

        val stateIndex = cursor.getColumnIndex(DiskContract.QueueExt.CLEANUP_STATE)
        val cleanUpState = cursor.getInt(stateIndex)
        assertThat(cleanUpState, equalTo(DiskContract.QueueExt.CleanupState.DEFAULT))

        val pathIndex = cursor.getColumnIndex(DiskContract.QueueExt.UPLOADED_PATH)
        val uploadedPath = cursor.getString(pathIndex)
        assertThat(uploadedPath, equalTo(item.destPath))

        cursor.close()
    }

    @Test
    fun shouldCalculateFilesSize() {
        queueImageToAutoUpload(IMG1_JPG, 0)
        queueImageToAutoUpload(IMG2_JPG, 0)
        queueImageToAutoUpload(IMG3_JPG, 0)

        val cleanupPolicy = CleanupPolicy(SystemClock.REAL)

        var allFilesSize = uploadQueue.queryAutouploadedFilesSize(cleanupPolicy.all())
        assertThat(allFilesSize, equalTo(0L))

        val oldFilesSize = uploadQueue.queryAutouploadedFilesSize(cleanupPolicy.excludeRecents())
        assertThat(oldFilesSize, equalTo(0L))

        val cursor = uploadQueue.queryAutouploadedFiles(Upload.State.IN_QUEUE)
        val items = ArrayList<FileQueueItem>()
        while (cursor.moveToNext()) {
            val fileItem = cursor.get(cursor.position)
            uploadQueue.markAsDone(fileItem, fileItem.destPath, TEST_UPLOADED_TIME)
            items.add(fileItem)
        }

        allFilesSize = uploadQueue.queryAutouploadedFilesSize(cleanupPolicy.all())
        assertThat(allFilesSize, equalTo(TEST_FILE_SIZE * 3))

        updateCleanupState(DiskContract.QueueExt.CleanupState.CHECKED, items[0])
        updateCleanupState(DiskContract.QueueExt.CleanupState.DELETED, items[1])

        allFilesSize = uploadQueue.queryAutouploadedFilesSize(cleanupPolicy.all())
        assertThat(allFilesSize, equalTo(TEST_FILE_SIZE))

        cursor.close()
    }

    @Test
    fun shouldMarkAsDeletedInvalidUploads() {
        val file1 = queueImageToAutoUpload(IMG1_JPG, 0).srcName
        val file2 = queueImageToAutoUpload(IMG2_JPG, 0).srcName
        val file3 = queueImageToAutoUpload(IMG3_JPG, 0).srcName

        val items = ArrayList<FileQueueItem>()
        items.add(findQueueItem(uploadQueue, file1))
        items.add(findQueueItem(uploadQueue, file2))
        items.add(findQueueItem(uploadQueue, file3))

        var newItem: FileQueueItem? = null

        for (item: FileQueueItem in items) {
            val createdFile = File(item.path)
            // all files with same hash
            item.sha256 = TEST_SHA_256_SUM
            uploadQueue.update(item)

            if (createdFile.name != IMG1_JPG) {
                // IMG2_JPG & IMG3_JPG uploaded but deleted from file system
                uploadQueue.markAsDone(item, item.destPath, TEST_UPLOADED_TIME)
            } else {
                newItem = item
            }
        }
        val dups = uploadQueue.findUploadedDuplicates(newItem!!)
        assertThat(dups.size, equalTo(2))
        for (item in dups) {
            assertThat(item.displayName, !equals(IMG1_JPG))
        }

    }

    @Test
    fun shouldMarkDefaultCleanupAsChecking() {
        val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
        addToQueueAndMarkAsDone("file_")
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(0))
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(100))
    }

    @Test
    fun shouldMarkCheckingCleanupAsDefault() {
        val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
        addToQueueAndMarkAsDone("file_")
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(0))
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(100))
        uploadQueue.markCheckingCleanupAsDefault()
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(0))
    }

    @Test
    fun shouldHandleNewCleanupState() {
        val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
        val list = addToQueueAndMarkAsDone("file_")
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(100))
        for (i in 0..49) {
            uploadQueue.updateCleanupState(DiskContract.QueueExt.CleanupState.CHECKED, list[i].id)
        }
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(50))

        for (i in 50..59) {
            uploadQueue.updateCleanupState(DiskContract.QueueExt.CleanupState.DELETED, list[i].id)
        }
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(40))
    }

    @Test
    fun shouldIgnoreNewUploadDuringCleanup() {
        val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
        addToQueueAndMarkAsDone("file_")
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(100))
        addToQueueAndMarkAsDone("some_file_")
        assertThat(uploadQueue.queryCleanupFilesCount(), equalTo(100))
    }

    @Test
    fun shouldQueryCleanupFiles() {
        val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
        val files = addToQueueAndMarkAsDone("file_")
        uploadQueue.markDefaultCleanupAsChecking(cleanupPolicy.all())
        uploadQueue.queryCleanupFiles(0, 10).use {
            assertNotNull(it)
            assertThat(it.count, equalTo(10))
            for (i in 0..98) {
                uploadQueue.updateCleanupState(DiskContract.QueueExt.CleanupState.DELETED, files[i].id)
            }
        }
        uploadQueue.queryCleanupFiles(0, 10).use {
            assertNotNull(it)
            assertThat(it.count, equalTo(1))
        }
    }

    private fun addToQueueAndMarkAsDone(prefix: String): List<FileQueueItem> {
        val list = ArrayList<FileQueueItem>()
        for (i in 1..100) {
            val path = queueImageToAutoUpload(prefix + i, 0).srcName
            val item = findQueueItem(uploadQueue, path)
            uploadQueue.markAsDone(item, item.destPath, TEST_UPLOADED_TIME)
            list.add(item)
        }
        return list
    }

    private fun findQueueItem(queue: UploadQueue, path: String): FileQueueItem {
        queue.findFile(path).use {
            if (it.moveToFirst()) {
                return it.makeItemForRow()!!
            } else {
                throw IllegalStateException("Failed to call makeItemForRow ")
            }
        }
    }

    @Test
    fun `should handle files with quote in name`() {
        val filePath = queueImageToAutoUpload("with'quotes'in'name").srcName
        val diskUploadQueueCursor = uploadQueue.findFile(filePath)
        diskUploadQueueCursor.moveToFirst()
        assertThat(diskUploadQueueCursor.count, equalTo(1))
        assertThat(diskUploadQueueCursor.srcName, equalTo(filePath))
    }

    @Test
    fun `should ignore uploaded items for another user`() {
        queueImageToAutoUpload(IMG1_JPG)
        assertPausedItemsCount(1)
        relogin()
        assertPausedItemsCount(0)
    }

    @Test
    fun `should ignore paused with error items in upload items count except SHARED_FOLDER_LIMIT_EXCEEDED`() {
        queueImageToUpload(IMG1_JPG, 0)
        queueImageToUpload(IMG2_JPG, 0)
        queueImageToUpload(IMG3_JPG, 0)

        assertThat(uploadQueue.queryUploadItemsCount(false), equalTo(3))

        val listToUpload = uploadQueue.queryFileListToUpload()

        uploadQueue.pauseFileInQueue(listToUpload[0], DiskContract.Queue.ErrorReason.PERMISSION_DENIAL)
        uploadQueue.resumePausedByPermissionDenialUploads()
        uploadQueue.pauseFileInQueue(listToUpload[1], DiskContract.Queue.ErrorReason.SHARED_FOLDER_LIMIT_EXCEEDED)

        assertThat(uploadQueue.queryUploadItemsCount(false), equalTo(3))
    }

    @Test
    fun `should handler single quotes in accountName`() {
        uploadQueue = TestObjectsFactory.createUploadQueueWithoutListeners(neighborsClient,
            selfClient, sqlite, mockContentResolver, storageListProvider,
            Credentials("test'_'new", 0L))

        uploadQueue.queryFileListToUploadPreviews()
    }

    @Test
    fun `should return null if info not founded`() {
        assertThat(uploadQueue.queryUploadInfo(1L), equalTo(null as UploadInfo?))
    }

    @Test
    fun `should return null uploaded time if upload not ended`() {
         queueImageToAutoUpload(IMG1_JPG)
        val cvForExt = ContentValues().apply {
            put(DiskContract.QueueExt.UPLOAD_ID, 1)
            put(DiskContract.QueueExt.ADDED_TO_QUEUE_TIME, TEST_QUEUED_TIME)
            put(DiskContract.QueueExt.UPLOAD_STARTED_TIME, TEST_UPLOAD_START_TIME)
        }
        sqlite.writableDatabase.insert(DiskContract.QueueExt.TABLE_EXT, null, cvForExt)

        val info = uploadQueue.queryUploadInfo(1L)!!
        assertThat(info.queuedTime, equalTo(TEST_QUEUED_TIME))
        assertThat(info.uploadStartedTime, equalTo(TEST_UPLOAD_START_TIME))
        assertThat(info.uploadedTime, equalTo(null as Long?))
    }

    @Test
    fun `should save item queued date`() {
        val path = queueImageToAutoUpload(IMG1_JPG).srcName

        uploadQueue.updateOrInsertQueuedDate(path, TEST_QUEUED_TIME, "wifi", true)

        assertThat(uploadQueue.queryUploadInfo(1L)!!.queuedTime, equalTo(TEST_QUEUED_TIME))
    }

    @Test
    fun `should update item queued date if record exists`() {
        val path = queueImageToAutoUpload(IMG1_JPG).srcName
        val cvForExt = ContentValues().apply {
            put(DiskContract.QueueExt.UPLOAD_ID, 1)
        }
        sqlite.writableDatabase.insert(DiskContract.QueueExt.TABLE_EXT, null, cvForExt)

        assertThat(uploadQueue.queryUploadInfo(1L)!!.queuedTime, equalTo(null as Long?))

        uploadQueue.updateOrInsertQueuedDate(path, TEST_QUEUED_TIME, "wifi", true)

        assertThat(uploadQueue.queryUploadInfo(1L)!!.queuedTime, equalTo(TEST_QUEUED_TIME))
    }

    @Test
    fun `should update existing record while markAsDone`() {
        queueImageToAutoUpload(IMG1_JPG)
        val cvForExt = ContentValues().apply {
            put(DiskContract.QueueExt.UPLOAD_ID, 1)
        }
        sqlite.writableDatabase.insert(DiskContract.QueueExt.TABLE_EXT, null, cvForExt)

        val uploadedPath = "test/uploaded/path"
        uploadQueue.markAsDone(pickFirstFileInQueue(), uploadedPath, TEST_UPLOADED_TIME)

        selectAllFromExtTable().use {
            it.moveToFirst()
            assertThat(it.count, equalTo(1))
            val uploadedPathIndex = it.getColumnIndex(DiskContract.QueueExt.UPLOADED_PATH)
            assertThat(it.getString(uploadedPathIndex), equalTo(uploadedPath))
        }
    }

    @Test
    fun `should do nothing if record in queue not exists`() {
        uploadQueue.updateOrInsertQueuedDate("test/path", TEST_QUEUED_TIME, "wifi", true)

        selectAllFromExtTable().use {
            assertThat(it.count, equalTo(0))
        }
    }

    @Test
    fun `should update upload start time`() {
        val path = queueImageToAutoUpload(IMG1_JPG).srcName

        uploadQueue.updateOrInsertQueuedDate(path, TEST_QUEUED_TIME, "wifi", true)

        uploadQueue.updateUploadStartTime(1L, TEST_UPLOAD_START_TIME)

        selectAllFromExtTable().use {
            it.moveToFirst()
            assertThat(it.count, equalTo(1))

            val uploadStartTimeIndex = it.getColumnIndex(DiskContract.QueueExt.UPLOAD_STARTED_TIME)
            assertThat(it.getLong(uploadStartTimeIndex), equalTo(TEST_UPLOAD_START_TIME))
        }

    }

    @Test
    fun `should not add ext record if record in queue not exists`() {
        queueImageToAutoUpload(IMG1_JPG)

        uploadQueue.updateUploadStartTime(1L, TEST_UPLOAD_START_TIME)

        selectAllFromExtTable().use {
            it.moveToFirst()
            assertThat(it.count, equalTo(0))
        }
    }

    @Test
    fun `should not update upload start time if it already presented`() {
        val path = queueImageToAutoUpload(IMG1_JPG).srcName
        uploadQueue.updateOrInsertQueuedDate(path, TEST_QUEUED_TIME, "wifi", true)

        uploadQueue.updateUploadStartTime(1L, TEST_UPLOAD_START_TIME)
        uploadQueue.updateUploadStartTime(1L, 300L)

        selectAllFromExtTable().use {
            it.moveToFirst()
            assertThat(it.count, equalTo(1))

            val uploadStartTimeIndex = it.getColumnIndex(DiskContract.QueueExt.UPLOAD_STARTED_TIME)
            assertThat(it.getLong(uploadStartTimeIndex), equalTo(TEST_UPLOAD_START_TIME))
        }
    }

    @Test
    fun `should query first upload`() {
        queueImageToAutoUpload("no upload time")
        val newerUploadedItem = queueImageToAutoUpload("newer upload time")
        val firsUploadedItem = queueImageToAutoUpload("first upload time")
        val badUploadedItem = queueImageToAutoUpload("upload time comes from Yandex.Search")

        uploadQueue.markAsDone(firsUploadedItem, "test", TEST_UPLOADED_TIME)
        uploadQueue.markAsDone(newerUploadedItem, "test", TEST_UPLOADED_TIME + 1)
        uploadQueue.markAsDone(badUploadedItem, "test", 0)

        assertThat(uploadQueue.queryFirstUploadTime(), equalTo(TEST_UPLOADED_TIME))
    }

    @Test
    fun `should query banned space`() {
        val queueItem = ContentValues().apply {
            put(DiskContract.Queue.SRC_NAME, "/storage/1810-0E0F/DCIM/Camera/IMG_20180714_101943.jpg")
            put(DiskContract.Queue.DATE, 1531552783861)
            put(DiskContract.Queue.STATE, DiskContract.Queue.State.UPLOADED)
            put(DiskContract.Queue.UPLOAD_ITEM_TYPE, DiskContract.Queue.UploadItemType.AUTOUPLOAD)
            put(DiskContract.Queue.USER, "test")
        }
        val queueItemId = db.insert(DiskContract.Queue.TABLE, null, queueItem)
        val queueExtItem = ContentValues().apply {
            put(DiskContract.QueueExt.UPLOAD_ID, queueItemId)
            put(DiskContract.QueueExt.CLEANUP_STATE, DiskContract.QueueExt.CleanupState.DEFAULT)
        }
        db.insert(DiskContract.QueueExt.TABLE_EXT, null, queueExtItem)
        assertThat(uploadQueue.containsFilesInBannedSpace(listOf(Path.asPath("/storage/1810-0E0F"))), equalTo("/storage/1810-0E0F"))
    }

    private fun selectAllFromExtTable(): Cursor {
        val query = SELECT_ALL_FROM + DiskContract.QueueExt.TABLE_EXT
        return sqlite.readableDatabase.query(query)
    }

    private fun assertPausedItemsCount(count: Int) {
        val notPausedAutouploads = uploadQueue.queryReuploadsAndAutouploads(null)
        assertThat(notPausedAutouploads.count, equalTo(count))
        notPausedAutouploads.close()
    }

    private fun relogin() {
        val newUserCreds = Credentials("test_new", 0L)
        uploadQueue = TestObjectsFactory.createUploadQueueWithoutListeners(neighborsClient,
            selfClient, sqlite, mockContentResolver, storageListProvider, newUserCreds)
    }

    private fun updateCleanupState(@DiskContract.QueueExt.CleanupState state: Int, item: FileQueueItem) {
        val cv = ContentValues()
        cv.put(DiskContract.QueueExt.CLEANUP_STATE, state)
        sqlite.writableDatabase.update(DiskContract.QueueExt.TABLE_EXT,
            cv, DiskContract.QueueExt.UPLOAD_ID + " = ?", asStringArray(item.id))
    }

    @Throws(RemoteException::class)
    private fun pickFirstFileInQueue(): FileQueueItem {
        val items = uploadQueue.queryFileListToUpload()
        val id = CompositeQueueId(DiskContract.Queue.UploadItemType.AUTOUPLOAD, items[0].id)
        val fileQueueItem = uploadQueue.queryItemById(id)

        assertNotNull(fileQueueItem)
        return fileQueueItem!!
    }

    private fun queueVideoToUpload(fileName: String, creationTimeOffset: Int): FileQueueItem {
        return queueToUpload(fileName, MediaTypeCode.VIDEO, creationTimeOffset.toLong(), DiskContract.Queue.State.IN_QUEUE)
    }

    private fun queueImageToUpload(fileName: String, creationTimeOffset: Int, state: Int = DiskContract.Queue.State.IN_QUEUE) =
        queueToUpload(fileName, MediaTypeCode.IMAGE, creationTimeOffset.toLong(), state)

    private fun queueVideoToAutoUpload(fileName: String, creationTimeOffset: Long): FileQueueItem {
        return queueToAutoUpload(fileName, MediaTypeCode.VIDEO, creationTimeOffset)
    }

    private fun queueImageToAutoUpload(fileName: String, creationTimeOffset: Long = 0): FileQueueItem {
        return queueToAutoUpload(fileName, MediaTypeCode.IMAGE, creationTimeOffset)
    }

    private fun makeFileOnSD(fileName: String, creationTimeOffset: Long): File {
        val testDirectory = getTestRootDirectory()

        val file = File(testDirectory, fileName)
        file.createNewFile()
        val fos = FileOutputStream(file)
        fos.write(ByteArray(TEST_FILE_SIZE.toInt()))
        fos.close()
        file.setLastModified(creationTimeOffset)
        return file
    }

    private fun queueToUpload(fileName: String, mediaType: Int, creationTimeOffset: Long, state: Int): FileQueueItem {
        val file = makeFileOnSD(fileName, creationTimeOffset)
        val srcName = file.absolutePath
        uploadQueue.addToDiskQueue(listOf(TestObjectsFactory.fileToContentValues(srcName, mediaType,
            state, TEST_DEST_DIR)))
        val queue = uploadQueue.queryFileListToUpload()
        return queue.first { it.srcName == srcName }
    }

    private fun queueToAutoUpload(fileName: String, mediaType: Int, creationTimeOffset: Long): FileQueueItem {
        val file = makeFileOnSD(fileName, creationTimeOffset)
        val srcName = file.absolutePath
        sqlite.writableDatabase.insert("MediaItems", null, ContentValues().apply { put("path", srcName) })
        uploadQueue.addToQueue(DiskQueueItem(srcName,
            ServerConstants.AUTOUPLOADING_FOLDER, mediaType, creationTimeOffset,
            DiskContract.Queue.UploadItemType.AUTOUPLOAD))
        val queue = uploadQueue.queryFileListToUpload()
        return queue.first { it.srcName == srcName }
    }

    private fun createDirItemToQueue(): List<ContentValues> {
        val values = ContentValues()
        values.put(DiskContract.Queue.SRC_NAME, "name")
        values.put(DiskContract.Queue.SRC_NAME_TOLOWER_NO_PATH, "name")
        values.put(DiskContract.Queue.DEST_NAME, "destName")
        values.put(DiskContract.Queue.DEST_DIR, "destDir")
        values.put(DiskContract.Queue.DATE, System.currentTimeMillis())
        values.put(DiskContract.Queue.STATE, DiskContract.Queue.State.IN_QUEUE)
        values.put(DiskContract.Queue.IS_DIR, 1)
        values.put(DiskContract.Queue.UPLOAD_ITEM_TYPE, DiskContract.Queue.UploadItemType.AUTOUPLOAD)
        return listOf(values)
    }

}
