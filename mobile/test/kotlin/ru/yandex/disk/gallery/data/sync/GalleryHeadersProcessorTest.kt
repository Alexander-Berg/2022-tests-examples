package ru.yandex.disk.gallery.data.sync

import org.mockito.kotlin.mock
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.DiskItem
import ru.yandex.disk.gallery.data.GalleryDataTestCase
import ru.yandex.disk.gallery.data.database.GalleryDataProvider
import ru.yandex.disk.gallery.data.database.MediaItemModel
import ru.yandex.disk.gallery.data.database.TimeHeader
import ru.yandex.disk.domain.albums.BucketAlbumId
import ru.yandex.disk.domain.albums.InnerAlbumId
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.photoslice.MomentItemMapping
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.util.AndroidCalendar
import ru.yandex.disk.util.MediaTypes
import ru.yandex.disk.util.makeDayIntervalsSplitByTzOffsets
import ru.yandex.disk.util.upTo
import ru.yandex.util.Path
import java.util.*
import java.util.concurrent.TimeUnit

class GalleryHeadersProcessorTest: GalleryDataTestCase() {

    private lateinit var processor: GalleryHeadersProcessor

    @Before
    override fun setUp() {
        super.setUp()

        dataProvider = GalleryDataProvider(mockContext, databaseTransactions, galleryDao, photosliceDao, photosliceMerger, mock(), CommandLogger())
        processor = GalleryHeadersProcessor(dataProvider, galleryDao)

        processor.setTz(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun `should create photoslice headers`() {
        createMediaItem(12)
        createMomentWithItems(23, 25)

        createMediaItem(36)
        createMomentWithItems(36)

        createMomentWithItems(50)

        createMediaItem(72)

        processHeaders(listOf(12, 36, 72), listOf(), listOf(), 0..100)

        assertThat(queryHeadersCounts(), equalTo(listOf(2, 3, 1, 1)))
        assertThat(queryHeadersStartHours(), equalTo(listOf(0, 24, 48, 72)))
    }

    @Test
    fun `should recount local headers update in memory`() {
        val album = BucketAlbumId("")

        processHeaders(listOf(), listOf(12, 16, 20, 36, 40, 72), listOf(), -1..100, album)

        assertThat(queryHeadersCounts(album), equalTo(listOf(3, 2, 1)))

        processHeaders(listOf(12, 36, 40, 72), listOf(76), listOf(16, 20), -1..100, album)

        assertThat(queryHeadersCounts(album), equalTo(listOf(1, 2, 2)))

        processHeaders(listOf(12, 36, 76), listOf(), listOf(40, 72), -1..100, album)

        assertThat(queryHeadersCounts(album), equalTo(listOf(1, 1, 1)))

        processHeaders(listOf(), listOf(), listOf(12, 36, 76), -1..100, album)

        assertThat(queryHeadersCounts(album), equalTo(emptyList()))
    }

    @Test
    fun `should keep header for recently uploaded moment item`() {
        val server = createDiskItem("some", 69)
        momentsDatabase.insertRecentlyUploadedItem(Path.asPath(server.path)!!, 0L)

        processHeaders(listOf(), listOf(), listOf(), -1..100)

        assertThat(queryHeadersCounts(), equalTo(listOf(1)))

        val local = createMediaItem(69)

        processHeaders(listOf(), listOf(local.eTime), listOf(), -1..100)

        assertThat(queryHeadersCounts(), equalTo(listOf(2)))

        galleryDao.deleteMediaItems(listOf(local))

        processHeaders(listOf(), listOf(), listOf(local.eTime), -1..100)

        assertThat(queryHeadersCounts(), equalTo(listOf(1)))
    }

    @Test
    fun `should remove orphaned headers`() {
        val moment1 = createMomentWithItems(12)

        val item1 = createMediaItem(36)

        val moment2 = createMomentWithItems(40, 50)

        val item2 = createMediaItem(72)

        processHeaders(listOf(36, 72), listOf(), listOf(), 0..100)
        assertThat(queryHeadersCounts(), equalTo(listOf(1, 2, 1, 1)))

        galleryDao.deleteMediaItems(listOf(item1, item2))

        momentsDatabase.deleteMomentItem(moment1.momentId, moment1.syncIds.single())
        momentsDatabase.deleteMomentItem(moment2.momentId, moment2.syncIds.last())

        processHeaders(listOf(), listOf(), listOf(36, 72), 0..100)

        assertThat(queryHeadersCounts(), equalTo(listOf(1)))
        assertThat(queryHeadersStartHours(), equalTo(listOf(24)))
    }

    @Test
    fun `should crop tz mismatched headers`() {
        createMediaItem(20)
        createMediaItem(30)
        createMomentWithItems(40, 50)

        processHeaders(listOf(20, 30), listOf(), listOf(), 0..50)
        assertThat(galleryDao.queryAlbumHeaders(PhotosliceAlbumId, null, -1).size, equalTo(3))

        try {
            processor.setTz(SimpleTimeZone(hoursMs(1).toInt(), "+1"))
            processHeaders(listOf(30), listOf(), listOf(), 30..30)

            assertThat(queryHeadersCounts(), equalTo(listOf(1, 2, 1)))
            assertThat(queryHeadersStartHours(), equalTo(listOf(0, 23, 48)))
            assertThat(queryHeadersEndHours(), equalTo(listOf(23, 47, 72)))

            processor.setTz(SimpleTimeZone(hoursMs(-1).toInt(), "-1"))
            processHeaders(listOf(30), listOf(), listOf(), 30..30)

            assertThat(queryHeadersCounts(), equalTo(listOf(1, 2, 1)))
            assertThat(queryHeadersStartHours(), equalTo(listOf(0, 25, 49)))
            assertThat(queryHeadersEndHours(), equalTo(listOf(23, 49, 72)))

        } finally {
            processor.setTz(TimeZone.getTimeZone("UTC"))
        }
    }

    private fun processHeaders(
        seenHours: List<Long>, addedHours: List<Long>, goneHours: List<Long>,
        visitedHours: IntRange, albumId: InnerAlbumId = PhotosliceAlbumId) {

        val eTimes = SortedETimesChanges(
                seenHours.map { hoursMs(it) },
                addedHours.map { hoursMs(it) },
                goneHours.map { hoursMs(it) })

        val visited = hoursMs(visitedHours.first.toLong()) upTo hoursMs(visitedHours.last.toLong())

        val changes = SortedMediaETimesChanges(MediaTypeMap(null, null, eTimes))
        val data = HeadersData(visited, mapOf(albumId to changes))

        val moments = dataProvider.getMomentPhotosliceTimesNotAfter(Long.MAX_VALUE, -1).asReversed()
        val groups = AndroidCalendar(processor.getTz()).makeDayIntervalsSplitByTzOffsets(moments)

        val overlapping = groups.mapNotNull { group ->
            group.filter { it.start <= visited.end && visited.start <= it.end }.takeUnless { it.isEmpty() }
        }
        processor.processHeaders(data, MomentDaysIntervals(overlapping))
    }

    private fun queryHeaders(albumId: InnerAlbumId = PhotosliceAlbumId): List<TimeHeader> {
        return dataProvider.getAlbumHeaders(albumId, null, -1).sortedBy { it.startTime }
    }

    private fun queryHeadersCounts(albumId: InnerAlbumId = PhotosliceAlbumId): List<Int> {
        return queryHeaders(albumId).map { it.count }
    }

    private fun queryHeadersStartHours(): List<Int> {
        return queryHeaders().map { TimeUnit.MILLISECONDS.toHours(it.startTime).toInt() }
    }

    private fun queryHeadersEndHours(): List<Int> {
        return queryHeaders().map { TimeUnit.MILLISECONDS.toHours(it.endTime + 1).toInt() }
    }

    private fun createMediaItem(eHours: Long): MediaItemModel {
        val eTime = hoursMs(eHours)

        val data = consMediaItem(path = "/$eTime", eTime = eTime, photosliceTime = eTime)

        return data.copy(id = galleryDao.insertMediaItem(data))
    }

    private fun createMomentWithItems(vararg eHours: Long): MomentWithItems {
        val momentId = createMoment(hoursMs(eHours.minOrNull()!!)..hoursMs(eHours.maxOrNull()!!))

        return MomentWithItems(momentId, eHours.map { createMomentItem(momentId, it) })
    }

    private fun createMomentItem(momentId: String, eHours: Long): String {
        val eTime = hoursMs(eHours)

        val (syncId, path) = generateSyncIdAndPath(momentId, eTime)

        momentsDatabase.insertOrReplace(momentId, MomentItemMapping(syncId, path))

        createDiskItem(momentId, eHours)

        return syncId
    }

    private fun createDiskItem(momentId: String, eHours: Long): DiskItem {
        val eTime = hoursMs(eHours)

        val (syncId, path) = generateSyncIdAndPath(momentId, eTime)

        val item = DiskItemBuilder().setMediaType(MediaTypes.IMAGE)
                .setPath(path).setEtag(syncId)
                .setMimeType("*/*").setSize(0L).build()

        insertDiskItem(item, eTime)

        return item
    }

    private fun generateSyncIdAndPath(momentId: String, eTime: Long) = "$eTime" to "/$momentId/$eTime"

    private data class MomentWithItems(val momentId: String, val syncIds: List<String>)
}
