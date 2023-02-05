package ru.yandex.disk.gallery.data.database

import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.domain.albums.AlbumId
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.provider.AlbumItemsFilter
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.gallery.data.provider.MediaTypeFilter
import ru.yandex.disk.photoslice.MomentItemMapping
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.provider.DiskTableCursor
import ru.yandex.disk.util.MediaTypes

private const val BUCKET = "bucket"

class GalleryDataProviderTest : GalleryDataTestCase() {

    @Test
    fun `should match local and server files`() {
        val doubled = createMediaItem("doubled", 48)
        val matched = createMediaItem("matched", 32)

        createMomentItem("/1", "eTag", 30, mediaType = MediaTypes.IMAGE)

        val mismatched = createMediaItem("mismatched", 24, mediaType = MediaTypes.IMAGE)
        val unresolved = createMediaItem(null, 12, mediaType = MediaTypes.VIDEO)

        createMomentItem("/2", doubled.serverETag!!, 6)
        createMomentItem("/3", doubled.serverETag!!, 6)
        createMomentItem("/4", matched.serverETag!!, 6)

        assertThat(queryPhotosliceCount(10, 50), equalTo(HeaderCounts(2, 1, 5)))
        assertThat(queryPhotosliceItemPaths(10, 50), equalTo(listOf("/2", "/4", "/1", null, null)))
        assertThat(queryPhotosliceItemIds(10, 50), equalTo(listOf(doubled.id, matched.id, doubled.id, mismatched.id, unresolved.id)))

        assertThat(queryPhotosliceCount(0, 50), equalTo(HeaderCounts(2, 1, 5)))
    }

    @Test
    fun `should use uploaded path`() {
        val missed = createMediaItem("tag1", 2, uploadPath = "/1")
        val matched = createMediaItem("tag2", 1, uploadPath = "/2")

        createMomentItem("/3", matched.serverETag!!, 0)

        assertThat(queryPhotosliceItemPaths(0, 2), equalTo(listOf<String?>("/1", "/3")))
        assertThat(queryPhotosliceItemIds(0, 2), equalTo(listOf(missed.id, matched.id)))
    }

    @Test
    fun `should hide local existing server file`() {
        val matched = createMediaItem("matched", 20)

        createMomentItem("/1", matched.serverETag!!, 10)
        createMomentItem("/2", matched.serverETag!!, 30)

        assertThat(queryPhotosliceCount(0, 35), equalTo(HeaderCounts(0, 0, 1)))
        assertThat(queryPhotosliceItemPaths(0, 35), equalTo(listOf<String?>("/1")))

        assertThat(queryPhotosliceCount(25, 35), equalTo(HeaderCounts.ZERO))
        assertThat(queryPhotosliceItemPaths(25, 35), equalTo(listOf()))

        assertThat(queryPhotosliceCount(5, 15), equalTo(HeaderCounts.ZERO))
        assertThat(queryPhotosliceItemPaths(5, 15), equalTo(listOf()))
    }

    @Test
    fun `should deduplicate server files`() {

        createMomentItem("/1", "eTag", 10, mediaType = MediaTypes.IMAGE)
        createMomentItem("/2", "eTag", 20, mediaType = MediaTypes.IMAGE)
        createMomentItem("/3", "eTag", 20, mediaType = MediaTypes.VIDEO)

        assertThat(queryPhotosliceCount(0, 15), equalTo(HeaderCounts.ZERO))
        assertThat(queryPhotosliceItemPaths(0, 15), equalTo(listOf()))

        assertThat(queryPhotosliceCount(15, 25), equalTo(HeaderCounts(0, 1, 1)))
        assertThat(queryPhotosliceItemPaths(15, 25), equalTo(listOf<String?>("/3")))

        assertThat(queryPhotosliceCount(0, 35), equalTo(HeaderCounts(0, 1, 1)))
        assertThat(queryPhotosliceItemPaths(0, 35), equalTo(listOf<String?>("/3")))
    }

    @Test
    fun `should find items by media type`() {

        createMediaItem("1", 10, mediaType = MediaTypes.IMAGE).id
        createMediaItem("2", 20, mediaType = MediaTypes.VIDEO).id
        createMediaItem("3", 30, mediaType = MediaTypes.IMAGE).id
        createMediaItem("4", 40, mediaType = MediaTypes.UNKNOWN).id

        createMomentItem("/5", "5", 10, MediaTypes.UNKNOWN)
        createMomentItem("/6", "6", 20, MediaTypes.IMAGE)
        createMomentItem("/7", "7", 30, MediaTypes.VIDEO)

        assertThat(queryPhotosliceCount(0, 50), equalTo(HeaderCounts(images = 3, videos = 2, total = 7)))

        assertThat(queryPhotosliceItemETags(0, 50, null),
                equalTo(listOf<String?>("4", "3", "7", "2", "6", "1", "5")))

        assertThat(queryPhotosliceItemETags(0, 50, MediaTypes.IMAGE),
                equalTo(listOf<String?>("3", "6", "1")))

        assertThat(queryPhotosliceItemETags(0, 50, MediaTypes.VIDEO),
                equalTo(listOf<String?>("7", "2")))
    }

    @Test
    fun `should return section paginated items`() {
        val doubled = createMediaItem("doubled", 48)
        val matched = createMediaItem("matched", 32)
        val mismatched = createMediaItem("mismatched", 24)

        createMomentItem("/1", "eTag", 20)

        val unsliced = createMediaItem(matched.serverETag, 12, photosliceTime = null)
        val unresolved = createMediaItem(null, 0)

        createMomentItem("/2", doubled.serverETag!!, 0)
        createMomentItem("/3", doubled.serverETag!!, 0)
        createMomentItem("/4", matched.serverETag!!, 0)

        val section1 = consSection(mismatched.eTime, doubled.eTime)
        val section2 = consSection(unresolved.eTime, matched.eTime)
        val section3 = consSection(unresolved.eTime, doubled.eTime)

        val photosliceAlbumFilter = AlbumItemsFilter(PhotosliceAlbumId)

        val slice1 = dataProvider.getAlbumItemsForSection(photosliceAlbumFilter, section1, -1, 0)

        assertThat(slice1.map { it.id }, equalTo(listOf(doubled, matched, mismatched).map { it.id }))
        assertThat(slice1.map { it.placement.server?.path }, equalTo(listOf("/2", "/4", null)))

        val slice2 = dataProvider.getAlbumItemsForSection(photosliceAlbumFilter, section2, 10, 0)

        //TODO remove it.diskItemId if need
        assertThat(slice2.map { it.id ?: it.diskItemId }, equalTo(listOf(matched.id, mismatched.id, doubled.id, unresolved.id)))
        assertThat(slice2.map { it.placement.server?.path }, equalTo(listOf("/4", null, "/1", null)))

        val slice3 = dataProvider.getAlbumItemsForSection(photosliceAlbumFilter, section3, 2, 3)

        val bucketAlbumFilter = AlbumItemsFilter(AlbumId.bucket(BUCKET))
        //TODO remove it.diskItemId if need
        assertThat(slice3.map { it.id ?: it.diskItemId }, equalTo(listOf(doubled.id, unresolved.id)))

        val bucket3 = dataProvider.getAlbumItemsForSection(bucketAlbumFilter, section3, 2, 3)

        assertThat(bucket3.map { it.id }, equalTo(listOf(unsliced, unresolved).map { it.id }))
    }

    @Test
    fun `should recount headers for items deletion`() {
        val bucketItems = listOf(30, 50, 50, 50).map { eTime -> createMediaItem(eTime.toLong()) }

        val bucketFirst = createHeader(20, 40, 1, "bucket")
        val bucketRest = createHeader(40, 60, 3, "bucket")
        val bucketFull = createHeader(0, 70, 4, "bucket")

        createMediaItem(50, "another")
        val anotherSliceItem = createMediaItem(50, "another", photoslice = true)

        val anotherBucket = createHeader(30, 80, 2, "another")
        val anotherSlice = createHeader(30, 80, 1, null)

        val momentLocalItem = createMediaItem(10, "moment", photoslice = true)
        createMomentItem("/path", "", 10)

        val moment = createHeader(0, 30, 2, null)

        dataProvider.deleteItemsAndRecountHeaders(listOf(bucketItems.first(), bucketItems.last()))

        val findCount: (Long) -> Int? = { galleryDao.queryHeaderById(it)?.count }

        assertThat(findCount(bucketFirst), nullValue())
        assertThat(findCount(bucketRest), equalTo(2))
        assertThat(findCount(bucketFull), equalTo(2))
        assertThat(findCount(anotherBucket), equalTo(2))
        assertThat(findCount(anotherSlice), equalTo(1))
        assertThat(findCount(moment), equalTo(2))

        dataProvider.deleteItemsAndRecountHeaders(listOf(anotherSliceItem, momentLocalItem))

        assertThat(findCount(bucketRest), equalTo(2))
        assertThat(findCount(bucketFull), equalTo(2))
        assertThat(findCount(anotherBucket), equalTo(1))
        assertThat(findCount(anotherSlice), nullValue())
        assertThat(findCount(moment), equalTo(1))
    }

    @Test
    fun `should update items and recount photoslice headers counts`() {
        val orphanedHeader = createHeader(30, 40, 100500, null)
        val localHeader = createHeader(20, 30, 100500, null)
        val commonHeader = createHeader(10, 20, 100500, null)

        val item = createMediaItem(25, "bucket", photoslice = true)
        createMediaItem(15, "bucket", photoslice = true)
        createMomentItem("/path", "", 10)

        dataProvider.updateItemsSyncResolvedAndRecountHeaders(listOf(15, 30)) {
            listOf(ItemUpdate(item, item.copy(size = 256)))
        }

        val findCount: (Long) -> Int? = { galleryDao.queryHeaderById(it)?.count }

        assertThat(findCount(orphanedHeader), nullValue())
        assertThat(findCount(localHeader), equalTo(1))
        assertThat(findCount(commonHeader), equalTo(2))

        assertThat(galleryDao.queryItemById(item.id!!)?.size, equalTo(256L))
    }

    @Test
    fun `should propagate photoslice time to server files`() {
        val doubled = createMediaItem("doubled", 48)
        val matched = createMediaItem("matched", 32)

        createMomentItem("/1", doubled.serverETag!!, 100500)
        createMomentItem("/2", doubled.serverETag!!, 100500)
        createMomentItem("/3", matched.serverETag!!, 100500)

        dataProvider.propagatePhotosliceTimesToServerFiles(listOf(doubled, matched))

        val eTimes = diskDatabase.queryAllFromTable().use { c ->
            c.iterator().asSequence().map { (it as DiskTableCursor).photosliceTime ?: 0 }.toList()
        }

        assertThat(eTimes, equalTo(listOf(doubled.eTime, doubled.eTime, matched.eTime)))
    }

    private fun queryPhotosliceCount(start: Long, end: Long): HeaderCounts {
        return dataProvider.countSliceItemsBetween(PhotosliceAlbumId, start, end)
    }

    private fun queryPhotosliceItems(start: Long, end: Long, mediaType: String? = null): List<MediaItem> {
        val filter = AlbumItemsFilter(PhotosliceAlbumId, mediaType)
        return dataProvider.getAlbumItemsForSection(filter, consSection(start, end), -1, 0)
    }

    private fun queryPhotosliceItemIds(start: Long, end: Long, mediaType: String? = null): List<Long?> {
        //TODO remove it.diskItemId if need
        return queryPhotosliceItems(start, end, mediaType).map { it.id ?: it.diskItemId }
    }

    private fun queryPhotosliceItemPaths(start: Long, end: Long, mediaType: String? = null): List<String?> {
        return queryPhotosliceItems(start, end, mediaType).map { it.placement.server?.path }
    }

    private fun queryPhotosliceItemETags(start: Long, end: Long, mediaType: String? = null): List<String?> {
        return queryPhotosliceItems(start, end, mediaType).map { it.serverETag }
    }

    private fun createMomentItem(path: String, eTag: String, eTime: Long, mediaType: String = MediaTypes.UNKNOWN) {
        momentsDatabase.insertOrReplace("momentId", MomentItemMapping(eTag + path, path))

        insertDiskItem(DiskItemBuilder()
                .setPath(path).setEtag(eTag)
                .setMediaType(mediaType)
                .setMimeType("*/*").setSize(0L).build(), eTime)
    }

    private fun createMediaItem(
            eTime: Long, bucketId: String = BUCKET,
            photoslice: Boolean = false, @MediaTypeFilter mediaType: String = MediaTypes.UNKNOWN): MediaItemModel {

        return createMediaItem(null, eTime, bucketId, eTime.takeIf { photoslice }, mediaType = mediaType)
    }

    private fun createMediaItem(
            serverETag: String?, eTime: Long, bucketId: String = BUCKET,
            photosliceTime: Long? = eTime, uploadPath: String? = null,
            mediaType: String = MediaTypes.UNKNOWN): MediaItemModel {

        val data = consMediaItem(
                path = "/$eTime", eTime = eTime, photosliceTime = photosliceTime,
                bucketId = bucketId, md5 = serverETag, serverETag = serverETag,
                uploadPath = uploadPath, mimeType = "$mediaType/*")

        return data.copy(id = galleryDao.insertMediaItem(data))
    }

    private fun createHeader(startTime: Long, endTime: Long, count: Int, bucketId: String? = BUCKET): Long {
        val albumId = bucketId?.let { AlbumId.bucket(it) } ?: PhotosliceAlbumId

        val header = Header(null, albumId, startTime, endTime, count, 0, 0)
        return galleryDao.insertHeader(header)
    }
}
