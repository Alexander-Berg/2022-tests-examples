package ru.yandex.disk.gallery.data.sync

import android.provider.MediaStore
import org.mockito.kotlin.*
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.gallery.data.database.*
import ru.yandex.disk.gallery.data.MediaStoreDeleteInProgressRegistry
import ru.yandex.disk.domain.albums.AlbumId
import ru.yandex.disk.gallery.data.provider.*
import ru.yandex.disk.domain.albums.AlbumSet
import ru.yandex.disk.util.MapCursorWrapper
import ru.yandex.disk.util.MediaTypes
import ru.yandex.disk.util.withTz
import java.util.*
import java.util.concurrent.TimeUnit

class MediaStoreSyncTest : GalleryDataTestCase() {

    private val images: MutableList<MediaStoreItem> = arrayListOf()
    private val videos: MutableList<MediaStoreItem> = arrayListOf()

    private val mediaStoreProvider = mock<MediaStoreProvider> { _ ->
        val comparator = compareBy<MediaStoreItem> { it.dateTaken }.thenBy { it.mediaId }.reversed()

        val prepare: (List<MediaStoreItem>, (MediaStoreItem) -> Map<String, Any?>) -> MapCursorWrapper = {
            items, mapper -> MapCursorWrapper(items.sortedWith(comparator).map(mapper).toTypedArray())
        }
        on { getImages() } doAnswer { MediaStoreImageCursor(prepare(images, ::imageToCursorMap)) }
        on { getVideos() } doAnswer { MediaStoreVideoCursor(prepare(videos, ::videoToCursorMap)) }
    }

    private val photosliceItemsHelper = mock<AlbumsBasedPhotosliceItemsHelper> {
        on { getAlbumSet(any(), anyOrNull()) } doAnswer { AlbumSet() }
    }
    private lateinit var dao: GalleryDao
    private lateinit var syncer: GallerySyncerHelper<*>

    @Before
    override fun setUp() {
        super.setUp()

        images.clear()
        videos.clear()

        dao = galleryDao

        syncer = GallerySyncerHelper(GallerySyncerHelper.Handler(), databaseTransactions, dataProvider,
                MediaStoreSyncerProcessor(
                    mediaStoreProvider, dao, dataProvider, photosliceMerger,
                    photosliceItemsHelper),
                GalleryHeadersProcessor(dataProvider, dao),
                MediaStoreDeleteInProgressRegistry(), "test")
    }

    @Test
    fun `should create daily`() {
        images.add(generateMedia(1, TimeUnit.DAYS.toMillis(0)))
        videos.add(generateMedia(2, TimeUnit.DAYS.toMillis(1)))
        images.add(generateMedia(3, TimeUnit.DAYS.toMillis(40)))

        syncer.sync()

        val daily = loadGallery()

        assertThat(daily, hasSize(3))
        assertThat(daily.map { it.items.single().mediaStoreId }, equalTo(listOf(3L, 2L, 1L)))
    }

    @Test
    fun `should delete media before and after`() {
        videos.add(generateMedia(1, TimeUnit.DAYS.toMillis(0)))
        videos.add(generateMedia(2, TimeUnit.DAYS.toMillis(1)))
        images.add(generateMedia(3, TimeUnit.DAYS.toMillis(2)))

        syncer.sync()

        assertThat(loadGallery(), hasSize(3))

        videos.removeAt(0)
        images.removeAt(0)

        syncer.sync()

        loadGallery().let {
            assertThat(it, hasSize(1))
            assertThat(it.single().items.single().mediaStoreId, equalTo(2L))
        }
    }

    @Test
    fun `should delete data for gone days`() {
        videos.add(generateMedia(1, TimeUnit.DAYS.toMillis(0)))
        images.add(generateMedia(2, TimeUnit.DAYS.toMillis(1)))
        videos.add(generateMedia(3, TimeUnit.DAYS.toMillis(2)))

        syncer.sync()

        assertThat(loadGallery(), hasSize(3))

        images.clear()
        syncer.sync()

        loadGallery().let { ss ->
            assertThat(ss, hasSize(2))
            assertThat(ss.flatMap { s -> s.items.map { it.mediaStoreId } }, equalTo(listOf(3L, 1L)))
        }
    }

    @Test
    fun `should keep headers until orphaned`() {
        val first = generateMedia(1, TimeUnit.DAYS.toMillis(0), 0)
        val gone = generateMedia(2, TimeUnit.DAYS.toMillis(1), 0)
        val last = generateMedia(3, TimeUnit.DAYS.toMillis(2), 0)

        images.addAll(listOf(first, gone, last))
        syncer.sync()

        val before = loadGallery()
        assertThat(before, hasSize(3))

        images.removeAt(1)
        syncer.sync()

        val after = loadGallery()
        assertThat(after, hasSize(2))

        assertThat(after.first().header.id, equalTo(before.first().header.id))
        assertThat(after.last().header.id, equalTo(before.last().header.id))
    }

    @Test
    fun `should maintain media type counters`() {
        images.add(generateMedia(1, TimeUnit.DAYS.toMillis(0)))

        videos.add(generateMedia(2, TimeUnit.DAYS.toMillis(1)))
        images.add(generateMedia(3, TimeUnit.DAYS.toMillis(1)))

        syncer.sync()

        assertThat(loadHeaders().map { HeaderCounts.of(it) }, equalTo(listOf(
                HeaderCounts(images = 1, videos = 0, total = 1),
                HeaderCounts(images = 1, videos = 1, total = 2))))

        videos.add(generateMedia(4, TimeUnit.DAYS.toMillis(0)))

        images.removeAt(1)
        videos.add(generateMedia(5, TimeUnit.DAYS.toMillis(1)))

        syncer.sync()

        assertThat(loadHeaders().map { HeaderCounts.of(it) }, equalTo(listOf(
                HeaderCounts(images = 1, videos = 1, total = 2),
                HeaderCounts(images = 0, videos = 2, total = 2))))
    }

    @Test
    fun `should survive headers deletion`() {
        videos.add(generateMedia(1, TimeUnit.DAYS.toMillis(0)))
        images.add(generateMedia(2, TimeUnit.DAYS.toMillis(1)))

        syncer.sync()

        assertThat(loadGallery().map { it.items.size }, equalTo(listOf(1, 1)))

        dao.deleteAllHeaders()

        syncer.sync()

        assertThat(loadGallery().map { it.items.size }, equalTo(listOf(1, 1)))
    }

    @Test
    fun `should keep media item id during update`() {
        val medias = listOf(
                generateMedia(0, TimeUnit.HOURS.toMillis(8)),
                generateMedia(1, TimeUnit.HOURS.toMillis(12)),
                generateMedia(2, TimeUnit.HOURS.toMillis(12), 120),
                generateMedia(3, TimeUnit.HOURS.toMillis(12)),
                generateMedia(4, TimeUnit.HOURS.toMillis(16)))
        videos.addAll(medias)

        syncer.sync()

        val items = loadGallery().single().items
        assertThat(items, hasSize(5))

        videos.clear()
        videos.addAll(listOf(
                generateMedia(medias[0].mediaId, medias[0].dateTaken + 120),
                generateMedia(medias[2].mediaId, medias[2].dateTaken, medias[2].duration + 120),
                generateMedia(medias[4].mediaId + 120, medias[4].dateTaken)))

        syncer.sync()

        val updated = loadGallery().single().items
        assertThat(updated, hasSize(3))

        assertThat(updated[0].id, not(equalTo(items[0].id)))

        assertThat(updated[1].id, equalTo(items[2].id))
        assertThat(updated[1].duration, not(equalTo(items[2].duration)))

        assertThat(updated[2].id, not(equalTo(items[4].id)))
    }

    @Test
    fun `should deal with duplicates during update`() {
        val loadItemIds: () -> List<Long> = { loadGallery().single().items.map { it.id!! } }
        val media = generateMedia(0, 0, 0)

        repeat(2) { images.add(media) }
        videos.add(media)

        syncer.sync()

        val itemIds = loadItemIds()
        assertThat(itemIds.toSet(), hasSize(2))

        images.removeAt(0)
        syncer.sync()

        val updatedIds = loadItemIds()
        assertThat(updatedIds, hasSize(2))
        assertThat(updatedIds, hasItems(*itemIds.toTypedArray()))

        videos.clear()
        syncer.sync()

        val nextIds = loadItemIds()
        assertThat(nextIds, hasSize(1))
        assertThat(nextIds.single(), isIn(updatedIds))
    }

    @Test
    fun `should deal with timezone changes`() {
        images.addAll((0L..4).map { generateMedia(it, TimeUnit.HOURS.toMillis(it * 5), 0) })
        images.addAll((0L..4).map { generateMedia(it, TimeUnit.HOURS.toMillis(70 - it * 5), 0) })

        fun sync(tzOffset: Long, batch: Int): List<Int> {
            withTz(SimpleTimeZone(TimeUnit.HOURS.toMillis(tzOffset).toInt(), tzOffset.toString())) {
                syncer.sync(batch)
            }
            return loadGallery().map { it.items.size }
        }

        assertThat(sync(-12, 3), equalTo(listOf(3, 2, 2, 3)))
        assertThat(sync(12, 3), equalTo(listOf(3, 2, 2, 3)))

        assertThat(sync(-5, 3), equalTo(listOf(4, 1, 4, 1)))
        assertThat(sync(0, 3), equalTo(listOf(5, 5)))
        assertThat(sync(5, 3), equalTo(listOf(1, 4, 1, 4)))

        assertThat(sync(-5, 50), equalTo(listOf(4, 1, 4, 1)))
        assertThat(sync(3, 50), equalTo(listOf(1, 4, 5)))
        assertThat(sync(5, 50), equalTo(listOf(1, 4, 1, 4)))
    }

    @Test
    fun `should not lose items while iterating`() {
        images.addAll((0L..14).map { generateMedia(it, hoursMs(it * 5 + 1)) })

        fun moveTo(fromIndex: Int, toIndex: Int) {
            val (from, to) = images[fromIndex] to images[toIndex]

            dao.replaceMediaDownload(MediaDownload(
                from.path!!, to.dateTaken - hoursMs(1), from.size, null, AlbumSet()))
        }

        moveTo(14, 1)
        moveTo(11, 11)
        moveTo(10, 6)
        moveTo(9, 8)
        moveTo(5, 2)
        moveTo(1, 0)

        fun sync(batchSize: Int) {
            withTz(SimpleTimeZone(0, "UTC")) { syncer.sync(batchSize) }

            assertThat(
                loadGallery().map { s -> s.items.map { it.mediaStoreId.toInt() } },
                equalTo(listOf(
                    listOf(13, 12, 11),
                    listOf(8, 9, 7, 6, 10),
                    listOf(4, 3, 2, 5, 14, 0, 1))))
        }

        sync(1)
        sync(3)
        sync(11)
        sync(17)
        sync(50)
    }

    private fun generateMedia(mediaStoreId: Long, dateTaken: Long, duration: Long = 0) =
            MediaStoreItem(mediaStoreId, "/path/$mediaStoreId", "album", "Album",
                    dateTaken, 0, 0, "unknown", 4000, 3000, MediaTypes.IMAGE, duration)

    private fun imageToCursorMap(item: MediaStoreItem) = mapOf(
            MediaStore.Images.ImageColumns.DATA to item.path,
            MediaStore.Images.ImageColumns.BUCKET_ID to item.bucketId,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME to item.bucketName,
            MediaStore.Images.ImageColumns.DATE_TAKEN to item.dateTaken,
            MediaStore.MediaColumns.DATE_ADDED to 0,
            MediaStore.Images.ImageColumns.DATE_MODIFIED to item.dateModified,
            MediaStore.Images.ImageColumns.SIZE to item.size,
            MediaStore.Images.ImageColumns.MIME_TYPE to "image/*",
            MediaStore.Images.ImageColumns._ID to item.mediaId,
            MediaStore.Images.ImageColumns.WIDTH to item.width,
            MediaStore.Images.ImageColumns.HEIGHT to item.height)

    private fun videoToCursorMap(item: MediaStoreItem) = mapOf(
            MediaStore.Video.VideoColumns.DATA to item.path,
            MediaStore.Video.VideoColumns.BUCKET_ID to item.bucketId,
            MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME to item.bucketName,
            MediaStore.Video.VideoColumns.DATE_TAKEN to item.dateTaken,
            MediaStore.MediaColumns.DATE_ADDED to 0,
            MediaStore.Video.VideoColumns.DATE_MODIFIED to item.dateModified,
            MediaStore.Video.VideoColumns.SIZE to item.size,
            MediaStore.Video.VideoColumns.DURATION to item.duration,
            MediaStore.Video.VideoColumns.MIME_TYPE to "video/*",
            MediaStore.Video.VideoColumns._ID to item.mediaId,
            MediaStore.Video.VideoColumns.WIDTH to item.width,
            MediaStore.Video.VideoColumns.HEIGHT to item.height)

    private fun loadGallery(): List<GallerySection> {

        val headers = loadHeaders()

        val sections = headers.sortedByDescending { it.startTime }.map {
            val items = dao.queryBucketItemsBetween("album", it.startTime, it.endTime, null, -1, 0)

            GallerySection(it, items)
        }

        sections.zipWithNext().forEach {
            assertThat(it.first.header.endTime, greaterThan(it.second.header.startTime))
        }
        assertThat(sections.map { it.header.count }, equalTo(sections.map { it.items.size }))

        return sections
    }

    private fun loadHeaders() = dao.queryOverlappingAlbumsHeaders(
            setOf(AlbumId.bucket("album")), Long.MIN_VALUE, Long.MAX_VALUE)

    private data class GallerySection(val header: Header, val items: List<MediaItemModel>)

}
