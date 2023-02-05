package ru.yandex.disk.gallery.data

import androidx.room.Room
import android.content.ContentValues
import android.preference.PreferenceManager
import androidx.annotation.CallSuper
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.hamcrest.Matchers
import org.junit.Before
import org.mockito.Mockito
import ru.yandex.disk.Credentials
import ru.yandex.disk.DiskItem
import ru.yandex.disk.autoupload.observer.StorageListProvider
import ru.yandex.disk.data.RoomDatabaseTransactions
import ru.yandex.disk.gallery.data.database.*
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.model.Section
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.gallery.data.provider.AlbumItemsFilter
import ru.yandex.disk.gallery.data.sync.PhotosliceMergeHandler
import ru.yandex.disk.gallery.utils.TestGalleryDatabase
import ru.yandex.disk.mocks.CredentialsManagerWithUser
import ru.yandex.disk.domain.albums.AlbumSet
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.photoslice.Moment
import ru.yandex.disk.photoslice.MomentsDatabase
import ru.yandex.disk.provider.DatabaseTransactions
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.sql.SQLiteOpenHelper2
import ru.yandex.disk.sync.PhotosliceSyncStateManager
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.toggle.SeparatedAutouploadToggle
import ru.yandex.disk.upload.DiskQueueSerializer
import ru.yandex.disk.upload.FileQueueItem
import ru.yandex.disk.upload.UploadQueue
import java.util.concurrent.TimeUnit

abstract class GalleryDataTestCase: AndroidTestCase2() {

    protected lateinit var momentsDatabase: MomentsDatabase
    protected lateinit var diskDatabase: DiskDatabase
    protected lateinit var dbOpenHelper: SQLiteOpenHelper2

    protected lateinit var galleryDatabase: TestGalleryDatabase
    protected lateinit var databaseTransactions: DatabaseTransactions
    protected lateinit var galleryDao: GalleryDao
    protected lateinit var photosliceDao: PhotosliceDao
    protected lateinit var rawQueriesDao: RawQueriesDao

    protected lateinit var dataProvider: GalleryDataProvider
    protected lateinit var photosliceMerger: PhotosliceMergeHandler

    protected lateinit var credentials: Credentials
    protected lateinit var uploadQueue: UploadQueue
    protected lateinit var diskQueueSerializer: DiskQueueSerializer

    @CallSuper
    @Before
    public override fun setUp() {
        super.setUp()

        dbOpenHelper = TestObjectsFactory.createSqlite(mockContext)

        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpenHelper)
        momentsDatabase = TestObjectsFactory.createMomentsDatabase(dbOpenHelper, PreferenceManager.getDefaultSharedPreferences(mockContext))

        galleryDatabase = Room.databaseBuilder(mockContext, TestGalleryDatabase::class.java, TestObjectsFactory.DB_NAME)
                .openHelperFactory {
                    dbOpenHelper.addOpenHelperCallback(it.callback)
                    dbOpenHelper.supportOpener
                }
                .allowMainThreadQueries()
                .build()

        databaseTransactions = RoomDatabaseTransactions(galleryDatabase)

        val photosliceSyncStateManager: PhotosliceSyncStateManager = mock {
            on { hasReadySnapshot() } doReturn true
            on { updateTime } doReturn 1L
            on { tableSyncerHelper } doReturn momentsDatabase.tableSyncHelper
        }

        galleryDao = galleryDatabase.galleryDao()
        val requestHelper = PhotosliceRequestsHelper(photosliceSyncStateManager)
        rawQueriesDao = galleryDatabase.rawQueriesDao()
        photosliceDao = PhotosliceDao(requestHelper, rawQueriesDao, mock())

        val selfProviderClient = TestObjectsFactory.createSelfContentProviderClient(mockContext, dbOpenHelper)
        val neighborsProviderClient = TestObjectsFactory.createNeighborsProviderClient(mockContext)
        val listProvider: StorageListProvider = Mockito.mock(StorageListProvider::class.java)
        val contentResolver = SeclusiveContext(mContext).contentResolver

        credentials = TestObjectsFactory.createCredentials()

        val serializers = TestObjectsFactory.getSerializers(neighborsProviderClient,
            selfProviderClient, credentials, contentResolver)

        uploadQueue = UploadQueue(dbOpenHelper, listProvider, serializers, credentials)
        diskQueueSerializer = serializers[DiskContract.Queue.UploadItemType.DEFAULT] as DiskQueueSerializer

        val appSettings = TestObjectsFactory.createApplicationSettings(mockContext)
        val userSettings = appSettings.getUserSettings(credentials)!!
        photosliceMerger = PhotosliceMergeHandler(
                photosliceSyncStateManager, photosliceDao, uploadQueue,
                CredentialsManagerWithUser(credentials.user), userSettings, SeparatedAutouploadToggle(false))

        dataProvider = GalleryDataProvider(
            mockContext,
            databaseTransactions,
            galleryDao,
            photosliceDao,
            photosliceMerger,
            mock(),
            CommandLogger()
        )
    }

    protected fun hoursMs(a: Long): Long = TimeUnit.HOURS.toMillis(a)

    protected fun hoursMs(a: Int): Long = hoursMs(a.toLong())

    protected fun createMoment(interval: LongRange): String {
        val momentId = "${interval.first}..${interval.last}"

        momentsDatabase.insertOrReplace(Moment.Builder.newBuilder()
                .setSyncId(momentId)
                .setItemsCount(100500)
                .setFromDate(interval.first)
                .setToDate(interval.last)
                .setLocalityRu("localityRu")
                .setLocalityUk("localityUk")
                .setLocalityEn("localityEn")
                .setLocalityTr("localityTr")
                .setIsInited(true)
                .build())

        return momentId
    }

    protected fun insertAutouploadedQueueItem(
            path: String, size: Long, uploadTime: Long?,
            uploadPath: String? = null, md5: String? = null): FileQueueItem {

        val cv = ContentValues(6)
        cv.put(DiskContract.Queue.SRC_NAME, path)
        cv.put(DiskContract.Queue.DEST_DIR, "/photos")
        cv.put(DiskContract.Queue.SIZE, size)
        cv.put(DiskContract.Queue.UPLOAD_ITEM_TYPE, DiskContract.Queue.UploadItemType.AUTOUPLOAD)
        cv.put(DiskContract.Queue.STATE, DiskContract.Queue.State.UPLOADED)
        cv.put(DiskContract.Queue.UPLOADED_TIME, uploadTime)

        md5?.let { cv.put(DiskContract.Queue.MD5, md5) }

        diskQueueSerializer.bulkInsert(arrayOf(cv))

        val item = uploadQueue.findFile(path).use { it.copyToList().single() }

        if (uploadPath != null) {
            val cve = ContentValues(2)

            cve.put(DiskContract.QueueExt.UPLOAD_ID, item.id)
            cve.put(DiskContract.QueueExt.UPLOADED_PATH, uploadPath)

            dbOpenHelper.writableDatabase.insert(DiskContract.QueueExt.TABLE_EXT, null, cve)
        }
        return item
    }

    protected fun insertDiskItem(item: DiskItem, photosliceTime: Long) {
        diskDatabase.updateOrInsert(DiskDatabase.convertToDiskItemRow(item)
            .setPhotosliceTime(photosliceTime)
            .setAlbums(AlbumSet(PhotosliceAlbumId)))
    }

    protected fun loadPhotoslice(): List<PhotosliceSection> {

        val headers = dataProvider.getAlbumHeaders(PhotosliceAlbumId, null, -1)

        val sections = headers.sortedByDescending { it.startTime }.map {
            val section = consSection(it.startTime, it.endTime, it.count)
            val items = dataProvider.getAlbumItemsForSection(AlbumItemsFilter(PhotosliceAlbumId), section, -1, 0)

            PhotosliceSection(it, items)
        }

        sections.zipWithNext().forEach {
            assertThat(it.first.header.endTime, Matchers.greaterThan(it.second.header.startTime))
        }
        assertThat(sections.map { it.header.count }, Matchers.equalTo(sections.map { it.items.size }))

        return sections
    }

    protected data class PhotosliceSection(val header: TimeHeader, val items: List<MediaItem>)

    companion object {
        fun consMediaItem(
                id: Long? = null, path: String = "",
                mediaStoreId: Long = 0, eTime: Long = 0, photosliceTime: Long? = null,
                bucketId: String = "", mimeType: String = "", duration: Long = 0, mTime: Long = 0,
                size: Long = 0, md5: String? = null,
                syncStatus: Int = SyncStatuses.UNRESOLVED, serverETag: String? = null,
                uploadTime: Long? = null, uploadPath: String? = null, downloadETag: String? = null,
                albumSet: AlbumSet = if (photosliceTime == null) AlbumSet() else AlbumSet(PhotosliceAlbumId)
        ) : MediaItemModel {

            return MediaItemModel(
                    id, path, mediaStoreId, eTime, photosliceTime, bucketId, 4000, 3000, albumSet,
                    mimeType, duration, mTime, size, md5, syncStatus, serverETag,
                    uploadTime, uploadPath, downloadETag, rescanAskedAt = null, beauty = null, mediaType = null)
        }

        fun consSection(
                startTime: Long, endTime: Long, count: Int = 0,
                countBefore: Int = 0, limitlessCountBefore: Int = 0, index: Int = 0): Section {

            return Section(startTime, endTime, count, countBefore, count, limitlessCountBefore, index)
        }
    }
}
