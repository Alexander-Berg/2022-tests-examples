package ru.yandex.disk.gallery.data

import androidx.sqlite.db.SimpleSQLiteQuery
import org.mockito.kotlin.mock
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.gallery.data.database.Header
import ru.yandex.disk.gallery.data.database.ItemAlbumsSyncData
import ru.yandex.disk.gallery.data.database.ItemSyncData
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.domain.albums.AlbumSet

class GalleryLogoutPerformerTest: GalleryDataTestCase() {

    private lateinit var performer: GalleryLogoutPerformer

    @Before
    override fun setUp() {
        super.setUp()

        performer = GalleryLogoutPerformer(databaseTransactions, galleryDao, rawQueriesDao, credentials, mock())
    }

    @Test
    fun `should clear sync data`() {
        val uploadData = ItemSyncData.uploaded("tag", 1, "/path")
        val albumsSync = ItemAlbumsSyncData(null, AlbumSet())
        val syncedData = ItemSyncData.synced(SyncStatuses.SYNCED_STRONGLY, "tag", albumsSync)
        val missedData = ItemSyncData.unsynced(SyncStatuses.MISSED_AT_SERVER)

        val itemIds = listOf(uploadData, syncedData, missedData).map {
            galleryDao.insertMediaItem(uploadData.applyTo(consMediaItem()))
        }

        val headerId = galleryDao.insertHeader(
                Header(null, PhotosliceAlbumId, 0, 0, 3, 0, 0))

        performer.clear()

        val items = galleryDao.queryItemsByIds(itemIds)
        assertThat(items, hasSize(3))

        assertThat(items.map { it.syncStatus }, everyItem(equalTo(SyncStatuses.UNRESOLVED)))
        assertThat(items.map { it.serverETag }, everyItem(nullValue()))
        assertThat(items.map { it.uploadTime }, everyItem(nullValue()))
        assertThat(items.map { it.uploadPath }, everyItem(nullValue()))

        assertThat(galleryDao.queryHeaderById(headerId), nullValue())
    }

    @Test
    fun `should store missed at server and clear uploaded paths`() {
        galleryDao.insertMediaItems(listOf(
                consMediaItem(path = "/1", size = 20, syncStatus = SyncStatuses.MISSED_AT_SERVER),
                consMediaItem(path = "/2", size = 30, syncStatus = SyncStatuses.MISSED_AT_SERVER),
                consMediaItem(path = "/3", size = 40, syncStatus = SyncStatuses.UPLOADED)
        ))

        insertAutouploadedQueueItem("/1", 20, 1, "/")
        insertAutouploadedQueueItem("/2", 100500, 1, "/")
        insertAutouploadedQueueItem("/3", 40, 1, "/")

        performer.clear()

        val queued = uploadQueue.queryQueuedFilesBySrcNames(setOf("/1", "/2", "/3"))

        assertThat(queued.map { it.srcPath }, equalTo(listOf("/1", "/2", "/3")))
        assertThat(queued.map { it.isMissedAtServer }, equalTo(listOf(true, false, false)))
        assertThat(queued.map { it.uploadPath }, everyItem(nullValue()))
    }

    @Test
    fun `should delete orphaned queue ext`() {
        val item = insertAutouploadedQueueItem("/1", 20, 1, "/")

        fun findExists(id: Long): Boolean {
            return galleryDatabase.rawQueriesDao().queryInt(SimpleSQLiteQuery(
                    "SELECT COUNT(1) FROM DISK_QUEUE_EXT WHERE upload_id = $id")) > 0
        }

        uploadQueue.delete(item)
        assertThat(findExists(item.id), equalTo(true))

        performer.clear()
        assertThat(findExists(item.id), equalTo(false))
    }
}
