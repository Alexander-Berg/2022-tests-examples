package ru.yandex.disk.gallery.data.command

import org.mockito.kotlin.mock
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.gallery.data.database.ItemAlbumsSyncData
import ru.yandex.disk.gallery.data.database.ItemSyncData
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.domain.albums.AlbumSet
import ru.yandex.disk.photoslice.MomentItemMapping
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.util.MediaTypes

class ProcessDeletedFilesCommandTest : GalleryDataTestCase() {

    private lateinit var command: ProcessDeletedFilesCommand

    @Before
    override fun setUp() {
        super.setUp()

        command = ProcessDeletedFilesCommand(galleryDao, dataProvider, mock(), mock())
    }

    @Test
    fun `should become missed at server`() {
        val momentItemId = insertMomentItem("/1", "tag", "moment")
        val albumsSync = ItemAlbumsSyncData(null, AlbumSet())
        val itemId = insertMediaItem(ItemSyncData.synced(SyncStatuses.SYNCED_STRONGLY, "tag", albumsSync))

        momentsDatabase.deleteMomentItem("moment", momentItemId)

        command.processMediaItems(emptyList(), listOf("tag"))

        assertThat(galleryDao.queryItemById(itemId)?.syncStatus, equalTo(SyncStatuses.MISSED_AT_SERVER))
    }

    @Test
    fun `should lose uploaded status`() {
        val itemId = insertMediaItem(ItemSyncData.uploaded("tag", 0, "/"))

        command.processMediaItems(emptyList(), listOf("tag"))

        assertThat(galleryDao.queryItemById(itemId)?.syncStatus, equalTo(SyncStatuses.MISSED_AT_SERVER))
    }

    @Test
    fun `should merge to survived file`() {
        insertMomentItem("/1", "tag", "moment")
        val momentItemId = insertMomentItem("/2", "tag", "moment")

        val itemId = insertMediaItem(ItemSyncData.uploaded("tag", 0, "/1"))

        momentsDatabase.deleteMomentItem("moment", momentItemId)

        command.processMediaItems(emptyList(), listOf("tag"))

        assertThat(galleryDao.queryItemById(itemId)?.syncStatus, equalTo(SyncStatuses.SYNCED_STRONGLY))
    }


    private fun insertMediaItem(sync: ItemSyncData): Long {
        val data = sync.applyTo(consMediaItem(photosliceTime = 0, md5 = sync.serverETag))

        return galleryDao.insertMediaItem(data)
    }

    private fun insertMomentItem(path: String, eTag: String, momentId: String = "momentId"): String {
        val itemId = listOf(path, eTag).joinToString("_") { it }

        momentsDatabase.insertOrReplace(momentId, MomentItemMapping(itemId, path))

        diskDatabase.updateOrInsert(DiskItemBuilder().setMediaType(MediaTypes.IMAGE)
                .setPath(path).setEtag(eTag).setEtime(0).setSize(0).build())

        return itemId
    }
}
