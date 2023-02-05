package ru.yandex.disk.gallery.data.sync

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.gallery.data.database.MediaItemModel
import ru.yandex.disk.photoslice.MomentItemMapping
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.util.MediaTypes

class PhotosliceSyncTest: GalleryDataTestCase() {

    private lateinit var headersProcessor: GalleryHeadersProcessor
    private lateinit var syncer: GallerySyncerHelper<*>

    @Before
    override fun setUp() {
        super.setUp()

        headersProcessor = GalleryHeadersProcessor(dataProvider, galleryDao)

        syncer = GallerySyncerHelper(
                GallerySyncerHelper.Handler(), databaseTransactions, dataProvider,
                PhotosliceSyncerProcessor(galleryDao, dataProvider), headersProcessor,
                Unit, "test")
    }

    @Test
    fun `should create headers for moments`() {
        val moment1 = createMoment(52..52)
        createMomentItem(moment1, 52, "1")

        val moment2 = createMoment(12..13)
        createMomentItem(moment2, 13, "2")
        createMomentItem(moment2, 12, "3")

        syncer.sync()

        assertThat(loadItemsETags(), equalTo(listOf(listOf<String?>("1"), listOf("2", "3"))))
    }

    @Test
    fun `should merge moments with local`() {
        val match = createMediaItem(12, "match")
        val miss = createMediaItem(36, "miss")

        syncer.sync()

        assertThat(loadHeaderCounts(), equalTo(listOf(1, 1)))

        val moment1 = createMoment(38..38)
        createMomentItem(moment1, 38, "tag")

        val moment2 = createMoment(12..16)
        createMomentItem(moment2, 16, match.md5!!)
        createMomentItem(moment2, 12, match.md5!!)

        syncer.sync()

        assertThat(loadItemsETags(), equalTo(listOf(listOf("tag", null), listOf("match"))))
        assertThat(loadItemIds(), equalTo(listOf(listOf(match.id, miss.id), listOf(match.id))))
    }

    @Test
    fun `should delete orphaned headers`() {
        val match = createMediaItem(52, "match")

        val moment1 = createMoment(52..52)
        val item1 = createMomentItem(moment1, 52, match.md5!!)

        val moment2 = createMoment(38..38)
        val item2 = createMomentItem(moment2, 38, "item2")

        val moment3 = createMoment(12..12)
        val item3 = createMomentItem(moment3, 12, "item3")
        val miss = createMediaItem(12, "miss")

        syncer.sync()

        assertThat(loadHeaderCounts(), equalTo(listOf(1, 1, 2)))

        momentsDatabase.deleteMomentItem(moment1, item1)
        momentsDatabase.deleteMomentItem(moment2, item2)
        momentsDatabase.deleteMomentItem(moment3, item3)

        syncer.sync()

        assertThat(loadHeaderCounts(), equalTo(listOf(1, 1)))
        assertThat(loadItemIds(), equalTo(listOf(listOf(match.id), listOf(miss.id))))
    }

    @Test
    fun `should survive headers deletion`() {
        createMediaItem(12, "md5")
        createMomentItem(createMoment(36..36), 36, "tag")

        syncer.sync()

        assertThat(loadHeaderCounts(), equalTo(listOf(1, 1)))

        galleryDao.deleteAllHeaders()

        syncer.sync()

        assertThat(loadHeaderCounts(), equalTo(listOf(1, 1)))
    }

    @Test
    fun `should not lose items while iterating`() {
        createMomentItem("moment", 12, "x")

        val pairs = listOf(Pair(12, 2), Pair(12, 1), Pair(11, 4), Pair(11, 3), Pair(10, 3))

        val items = pairs.map { createMediaItem(it.first.toLong(), "x", it.second.toLong()) }

        syncer.sync(1)

        assertThat(loadItemIds(), equalTo(listOf(items.map { it.id })))
        assertThat(loadItemsETags().flatten(), everyItem(equalTo("x")))
    }

    private fun loadHeaderCounts(): List<Int> = loadPhotoslice().map { it.header.count }

    private fun loadItemsETags(): List<List<String?>> = itemsETags(loadPhotoslice())

    //TODO remove it.diskItemId if need
    private fun loadItemIds(): List<List<Long?>> = loadPhotoslice().map { s -> s.items.map { it.id ?: it.diskItemId } }

    private fun itemsETags(sections: List<PhotosliceSection>): List<List<String?>> =
            sections.map { s -> s.items.map { it.serverETag } }

    private fun createMediaItem(eHours: Long, md5: String, mediaId: Long = 0): MediaItemModel {
        val eTime = hoursMs(eHours)

        val data = consMediaItem(
                path = "/$eTime", mediaStoreId = mediaId,
                eTime = eTime, photosliceTime = eTime, md5 = md5)

        return data.copy(id = galleryDao.insertMediaItem(data))
    }

    private fun createMomentItem(momentId: String, eHours: Long, eTag: String): String {
        val eTime = hoursMs(eHours)

        val syncId = "$eTag/$eTime"
        val path = "/$momentId/$eTime"

        momentsDatabase.insertOrReplace(momentId, MomentItemMapping(syncId, path))

        insertDiskItem(DiskItemBuilder().setMediaType(MediaTypes.IMAGE)
                .setPath(path).setEtag(eTag)
                .setMimeType("*/*").setSize(0L).build(), eTime)

        return syncId
    }

    private fun createMoment(intervalHours: IntRange): String {
        return createMoment(hoursMs(intervalHours.start)..hoursMs(intervalHours.endInclusive))
    }
}
