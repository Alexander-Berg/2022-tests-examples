package ru.yandex.disk.gallery.data.database

import androidx.room.Room
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.*
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.gallery.data.GalleryDataTestCase.Companion.consMediaItem
import ru.yandex.disk.gallery.utils.TestGalleryDatabase
import ru.yandex.disk.domain.albums.InnerAlbumId
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.util.MediaTypes

private const val MOCK_PATH = "/storage/emulated/0/pictures/image"
private const val MOCK_BUCKET_ID = "Mock bucket Id"
private const val MOCK_TIME = 1524873600000L
private const val IMAGE_MIME_TYPE = "image/*"
private const val VIDEO_MIME_TYPE = "video/*"

class GalleryDatabaseTest : TestCase2() {

    private val context = RuntimeEnvironment.application

    private val database = Room.databaseBuilder(context,
            TestGalleryDatabase::class.java, "galleryTest")
            .allowMainThreadQueries()
            .build()

    private val dao = database.galleryDao()

    private var rawDatabase = database.openHelper.readableDatabase

    @Test
    fun `should add new media item`() {
        insertMediaItem()

        assertHasElement("MediaItems")
    }

    @Test
    fun `should add new header`() {
        insertHeader(0, 0)

        assertHasElement("MediaHeaders")
    }

    @Test
    fun `should add image`() {
        dao.insertMediaItem(consData(0, 0))

        val data = findMediaItems(MOCK_BUCKET_ID)

        assertThat(data, hasSize(1))
        assertThat(data[0].mimeType, equalTo(IMAGE_MIME_TYPE))
    }

    @Test
    fun `should find headers by media type`() {
        insertHeader(0, 1, HeaderCounts(1, 0, 1))
        insertHeader(1, 2, HeaderCounts(0, 2, 2))
        insertHeader(2, 3, HeaderCounts(2, 1, 3))

        fun query(type: String?) = dao.queryAlbumHeaders(PhotosliceAlbumId, type, -1)
                .map { it.startTime.toInt() to it.count }

        assertThat(query(null), equalTo(listOf(2 to 3, 1 to 2, 0 to 1)))
        assertThat(query(null), equalTo(query(MediaTypes.UNKNOWN)))

        assertThat(query(MediaTypes.VIDEO), equalTo(listOf(2 to 1, 1 to 2)))
        assertThat(query(MediaTypes.IMAGE), equalTo(listOf(2 to 2, 0 to 1)))
    }

    @Test
    fun `should not return items from different album`() {
        dao.insertMediaItem(consData(0, 0, MOCK_BUCKET_ID, VIDEO_MIME_TYPE))

        assertThat(findMediaItems("Wrong album"), empty())
    }

    @Test
    fun `should query item position in bucket`() {
        dao.insertMediaItem(consData(2L, 20, MOCK_BUCKET_ID))
        dao.insertMediaItem(consData(3L, 10, MOCK_BUCKET_ID))
        dao.insertMediaItem(consData(1L, 10, MOCK_BUCKET_ID))
        dao.insertMediaItem(consData(3L, 0, "Another"))

        val find: (Long, String) -> Int? = dao::queryItemPositionInBucket

        assertThat(find(4L, MOCK_BUCKET_ID), nullValue())
        assertThat(find(1L, MOCK_BUCKET_ID), equalTo(2))
        assertThat(find(3L, MOCK_BUCKET_ID), equalTo(1))
        assertThat(find(3L, "Another"), equalTo(0))
    }

    @Test
    fun `should find media between`() {
        dao.insertMediaItem(consData(1L, 10))
        dao.insertMediaItem(consData(2L, 50))
        dao.insertMediaItem(consData(3L, 50))
        dao.insertMediaItem(consData(4L, 100))

        val findMediaIds: (Long, Long) -> List<Long> = { from, to ->
            dao.queryItemsBetweenKeys(
                    MediaStoreKey(from, Long.MIN_VALUE), MediaStoreKey(to, Long.MAX_VALUE), -1).map { it.mediaStoreId }
        }

        assertThat(findMediaIds(50, 100), equalTo(listOf(4L, 3L, 2L)))
        assertThat(findMediaIds(10, 50), equalTo(listOf(3L, 2L, 1L)))
        assertThat(findMediaIds(0, 20), equalTo(listOf(1L)))
        assertThat(findMediaIds(200, 0), empty())
    }

    @Test
    fun `should find items by media type`() {
        dao.insertMediaItems(listOf(
                consData(1L, eTime = 10, bucketId = "B", mimeType = IMAGE_MIME_TYPE),
                consData(2L, eTime = 20, bucketId = "B", mimeType = VIDEO_MIME_TYPE),
                consData(3L, eTime = 30, bucketId = "B", mimeType = "Unknown"),
                consData(4L, eTime = 30, isPhotoslice = true)))

        fun find(type: String?) = dao.queryBucketItemsBetween(
                "B", Long.MIN_VALUE, Long.MAX_VALUE, type, -1, 0)
                .map { it.mediaStoreId }

        assertThat(find(null), equalTo(listOf(3L, 2L, 1L)))
        assertThat(find(null), equalTo(find(MediaTypes.UNKNOWN)))

        assertThat(find(MediaTypes.IMAGE), equalTo(listOf(1L)))
        assertThat(find(MediaTypes.VIDEO), equalTo(listOf(2L)))
    }

    @Test
    fun `should count items by media type`() {
        dao.insertMediaItems(listOf(
                consData(1L, eTime = 10, bucketId = "B", mimeType = IMAGE_MIME_TYPE),
                consData(2L, eTime = 20, bucketId = "B", mimeType = VIDEO_MIME_TYPE),
                consData(3L, eTime = 30, bucketId = "B", mimeType = "Unknown"),
                consData(4L, eTime = 40, bucketId = "B", mimeType = VIDEO_MIME_TYPE),
                consData(5L, eTime = 50, isPhotoslice = true, mimeType = VIDEO_MIME_TYPE),
                consData(6L, eTime = 60, isPhotoslice = true, mimeType = IMAGE_MIME_TYPE),
                consData(7L, eTime = 70, isPhotoslice = true, mimeType = IMAGE_MIME_TYPE)))

        assertThat(dao.countBucketItemsBetween("B", Long.MIN_VALUE, Long.MAX_VALUE),
                equalTo(HeaderCounts(images = 1, videos = 2, total = 4)))

        assertThat(dao.countSliceItemsBetween(PhotosliceAlbumId.bitMask, Long.MIN_VALUE, Long.MAX_VALUE),
                equalTo(HeaderCounts(images = 2, videos = 1, total = 3)))
    }

    private fun insertHeader(
            startTime: Long, endTime: Long,
            counts: HeaderCounts = HeaderCounts.ZERO,
            albumId: InnerAlbumId = PhotosliceAlbumId): Long {

        val header = Header(null, albumId, startTime, endTime, 0, 0, 0)
        return dao.insertHeader(counts.applyTo(header))
    }

    private fun insertMediaItem(): Long {
        return dao.insertMediaItem(consMediaItem(
                path = MOCK_PATH, eTime = MOCK_TIME, bucketId = MOCK_BUCKET_ID, mimeType = IMAGE_MIME_TYPE))
    }

    private fun consData(
            mediaId: Long, eTime: Long, bucketId: String = MOCK_BUCKET_ID,
            mimeType: String = IMAGE_MIME_TYPE, isPhotoslice: Boolean = false): MediaItemModel {

        return consMediaItem(
                path = MOCK_PATH, mediaStoreId = mediaId, eTime = eTime,
                photosliceTime = eTime.takeIf { isPhotoslice }, bucketId = bucketId, mimeType = mimeType)
    }

    private fun findMediaItems(bucketId: String): List<MediaItemModel> =
            dao.queryBucketItemsBetween(bucketId, Long.MIN_VALUE, Long.MAX_VALUE, null, -1,0)

    private fun assertHasElement(table: String) {
        val cursor = rawDatabase.query("SELECT * FROM $table")
        assertThat(cursor.count, equalTo(1))
    }
}
