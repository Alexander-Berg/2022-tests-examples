package ru.yandex.disk.provider

import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.hamcrest.Matchers.*
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.Credentials
import ru.yandex.disk.Mocks.*
import ru.yandex.disk.asyncbitmap.LegacyPreviewsDatabase
import ru.yandex.disk.asyncbitmap.PreviewsDatabaseContract
import ru.yandex.disk.database.BaseDatabaseUpgradeTest
import ru.yandex.disk.feed.FeedDatabase
import ru.yandex.disk.photoslice.MomentColumns
import ru.yandex.disk.photoslice.MomentsDatabase
import ru.yandex.disk.sql.TableSuffix
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.test.TestObjectsFactory.*
import ru.yandex.disk.upload.StorageListProviderStub
import ru.yandex.disk.upload.Upload
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.util.Path

private const val LEGACY_GOLDEN_PREVIEW_ETAG_COLUMN_NAME = "goldenPreviewEtag"

@Config(manifest = Config.NONE)
class DatabaseUpgradeTest : BaseDatabaseUpgradeTest() {

    private lateinit var diskDatabase: DiskDatabase
    private lateinit var momentsDatabase: MomentsDatabase
    private lateinit var feedDatabase: FeedDatabase
    private lateinit var uploadQueue: UploadQueue
    private lateinit var sqlite: DH
    private lateinit var keyValueStore: SharedPreferences

    private fun setupOldDatabase(filename: String) {
        setupOldDatabase(filename, "disk")
        initDatabasesObjects()
    }

    private fun initDatabasesObjects() {
        sqlite = DH(context)
        keyValueStore = PreferenceManager.getDefaultSharedPreferences(mockContext)

        diskDatabase = createDiskDatabase(sqlite, null, null)
        momentsDatabase = TestObjectsFactory.createMomentsDatabase(sqlite, keyValueStore)
        feedDatabase = TestObjectsFactory.createFeedDatabase(sqlite, createSettings(mockContext, sqlite), diskDatabase)

        val cp = createDiskContentProvider(context, sqlite)
        val authority = DiskContentProvider.getAuthority(context)
        addContentProvider(context, cp, authority)
        context.setActivityManager(mockActivityManager())
        val cr = context.contentResolver
        val selfClient = createSelfContentProviderClient(context, sqlite)
        val neighborsClient = createNeighborsProviderClient(context)

        uploadQueue = createUploadQueueWithoutListeners(neighborsClient, selfClient, sqlite, cr,
            StorageListProviderStub(), Credentials("test", 0L))
    }

    @Test
    fun `should upgrade default order then upgrade from 281`() {
        setupOldDatabase("DISK_2.81")

        val path = Path("/disk/0")
        diskDatabase.updateOrInsert(DiskItemBuilder().setPath(path).build())

        val blockId = feedDatabase.insertBlock(createContentBlock())
        feedDatabase.upsertBlockToFileBinder(blockId, 0, 2, path)
        feedDatabase.upsertBlockToFileBinder(blockId, 1, 1, path)
        feedDatabase.upsertBlockToFileBinder(blockId, 2, 0, path)

        feedDatabase.queryLoadedBlockItems(blockId).use { blockItems ->
            assertThat(blockItems[0].fraction, equalTo(0))
            assertThat(blockItems[1].fraction, equalTo(1))
            assertThat(blockItems[2].fraction, equalTo(2))
        }
    }

    @Test
    fun `should delete id column from 282`() {
        setupOldDatabase("DISK_2.82")
        assertColumnsNotExist(DiskDatabase.TABLE, "_id")
        assertColumnsExist(DiskDatabase.VIEW_DISK_ITEMS, "_id")
    }

    @Test
    fun `should add data source column to 303`() {
        setupOldDatabase("DISK_2.82")
        assertColumnsExist(FeedDatabase.TABLE_FEED_BLOCKS, "data_source")
    }

    @Test
    fun `should update report sent status from 3_03`() {
        setupOldDatabase("DISK_3.03_UPLOAD_QUEUE_WITH_REPORT_SENT")

        val oldReportSentItems = uploadQueue.queryAutouploadedFiles(Upload.State.UPLOADED)
        assertThat(oldReportSentItems.count, equalTo(1))

        val anotherItems = uploadQueue.queryAutouploadedFiles(Upload.State.IN_QUEUE)
        assertThat(anotherItems.count, equalTo(1))
    }

    @Test
    fun `should drop folder type column from 3_05`() {
        setupOldDatabase("DISK_3.05_PUBLIC_LINKS")
        assertColumnsNotExist(DiskDatabase.TABLE, "FOLDER_TYPE")
    }

    @Test
    fun `should add resource ids and years ago from 3_08`() {
        setupOldDatabase("DISK_3.08_RUNTIME_PERMISSIONS")
        assertColumnsExist(FeedDatabase.TABLE_FEED_BLOCKS, "resource_ids", "years_ago")
    }

    @Test
    fun `should add collection id from 3_14`() {
        setupOldDatabase("DISK_3.14")
        assertColumnsExist(FeedDatabase.TABLE_FEED_BLOCKS, "remote_collection_id")
    }

    @Test
    fun `should add update trigger on moment to moment items table from 3_20`() {
        setupOldDatabase("DISK_3.20")

        val triggerNames = getDbTriggersNames(sqlite)

        assertTrue(triggerNames.contains("update_moment_to_moment_item1"))
        assertTrue(triggerNames.contains("update_moment_to_moment_item2"))
    }

    @Test
    fun `should drop recent table from 2_77`() {
        setupOldDatabase("DISK_2.77")
        assertNoTable("recent_event_group1")
        assertNoTable("recent_event_group2")
        assertNoTable("recent_event1")
        assertNoTable("recent_event2")
        assertNoTable("view_events1")
        assertNoTable("view_events2")
        assertNoTable("view_disk_items1")
        assertNoTable("view_disk_items2")
    }

    @Test
    fun `should add action column to feed blocks from 3_22`() {
        setupOldDatabase("DISK_3.22")
        assertColumnsExist(FeedDatabase.TABLE_FEED_BLOCKS, "area")
    }

    @Test
    fun `should add visible_for_user column to feed blocks from 3_25`() {
        setupOldDatabase("DISK_3.25")
        assertColumnsExist(FeedDatabase.TABLE_FEED_BLOCKS, "visible_for_user")
    }

    @Test
    fun `should add sync status column to moments from 3_25`() {
        setupOldDatabase("DISK_3.25")
        TableSuffix.values().forEach {
            assertColumnsExist(it.getTableName(MomentsDatabase.TABLE_MOMENTS), MomentColumns.IS_INITED)
        }
    }

    @Test
    fun `should remove column for golden cache on demand`() {
        setupOldDatabase("DISK_3.35")
        LegacyPreviewsDatabase(momentsDatabase, sqlite).apply {
            LegacyPreviewsDatabase.removeGoldenCacheEtagColumn(sqlite)
        }

        assertColumnsNotExist(PreviewsDatabaseContract.TABLE, LEGACY_GOLDEN_PREVIEW_ETAG_COLUMN_NAME)
        TableSuffix.values().forEach {
            assertColumnsNotExist(it.getTableName(PreviewsDatabaseContract.VIEW),
                    LEGACY_GOLDEN_PREVIEW_ETAG_COLUMN_NAME)
        }
    }

    @Test
    fun `should create indexes for gallery from 3_35`() {
        setupOldDatabase("DISK_3.35")

        assertThat(getDbIndexesNames(sqlite), hasItems(
                "DISK_ETAG_INDEX", "DISK_ETIME_INDEX",
                "MOMENTS1_INTERVAL_INDEX", "MOMENTS2_INTERVAL_INDEX"))
    }

    @Test
    fun `should create index for upload queue src_parent`() {
        setupOldDatabase("DISK_3.35")

        assertThat(getDbIndexesNames(sqlite), hasItems("SRC_PARENT_INDEX"))
    }

    @Test
    fun `should create triggers to manipulate src_parent column`() {
        setupOldDatabase("DISK_3.35")

        assertThat(getDbTriggersNames(sqlite), hasItems(
            "src_parent_update_trigger", "src_parent_insert_trigger"))
    }

    @Test
    fun `should add new columns on 4_26 upgrade`() {
        setupOldDatabase("DISK_4.23")

        assertColumnsExist(DiskContract.QueueExt.TABLE_EXT, DiskContract.QueueExt.ADDED_TO_QUEUE_TIME)
        assertColumnsExist(DiskContract.QueueExt.TABLE_EXT, DiskContract.QueueExt.UPLOAD_STARTED_TIME)
    }


    @Test
    fun `should add new columns on 4_29 upgrade`() {
        setupOldDatabase("DISK_4.23")

        assertColumnsExist(DiskContract.QueueExt.TABLE_EXT, DiskContract.QueueExt.ENQUEUED_NETWORK)
        assertColumnsExist(DiskContract.QueueExt.TABLE_EXT, DiskContract.QueueExt.ENQUEUED_ON_APPROPRIATE_NETWORK)
    }

    private fun assertColumnsExist(tableName: String, vararg columns: String) {
        assertColumnsExist(true, tableName, *columns)
    }

    private fun assertColumnsNotExist(tableName: String, vararg columns: String) {
        assertColumnsExist(false, tableName, *columns)
    }

    private fun assertColumnsExist(shouldExist: Boolean, tableName: String, vararg columns: String) {
        getTableInfo(tableName).use { cursor ->
            columns
                    .map { cursor.getColumnIndex(it) }
                    .forEach { assertThat(it, if (shouldExist) COLUMN_EXISTS else COLUMN_NOT_EXISTS) }
        }
    }

    private fun assertNoTable(tableName: String) {
        assertNoTable(sqlite, tableName)
    }

    private fun getTableInfo(tableName: String) = getTableInfo(sqlite, tableName)

    companion object {
        private val COLUMN_NOT_EXISTS = equalTo(-1)
        private val COLUMN_EXISTS = not(COLUMN_NOT_EXISTS)
    }

}
