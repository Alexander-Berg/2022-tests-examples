package ru.yandex.disk.gallery.data.provider

import androidx.paging.PagedList
import androidx.room.Room
import org.mockito.kotlin.mock
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.hasSize
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import ru.yandex.disk.data.RoomDatabaseTransactions
import ru.yandex.disk.domain.albums.AlbumId
import ru.yandex.disk.gallery.data.GalleryDataTestCase.Companion.consMediaItem
import ru.yandex.disk.gallery.data.command.RecountHeadersCommandRequest
import ru.yandex.disk.gallery.data.database.*
import ru.yandex.disk.gallery.data.model.*
import ru.yandex.disk.gallery.utils.TestGalleryDatabase
import ru.yandex.disk.provider.DatabaseTransactions
import ru.yandex.disk.service.CommandRequest
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.util.MediaTypes
import java.util.concurrent.TimeUnit

private val ALBUM = AlbumId.bucket("bucket")

class GalleryDataSourceTest: AndroidTestCase2() {

    private lateinit var provider: GalleryDataProvider

    private lateinit var database: TestGalleryDatabase
    private lateinit var transactions: DatabaseTransactions
    private lateinit var galleryDao: GalleryDao
    private lateinit var changedNotifiers: DatabaseChangedNotifiers

    private val capturedCommands = arrayListOf<CommandRequest>()

    @Before
    public override fun setUp() {
        super.setUp()

        database = Room.databaseBuilder(
                mockContext, TestGalleryDatabase::class.java, "galleryDataSourceTest")
                .allowMainThreadQueries()
                .build()
        transactions = RoomDatabaseTransactions(database)
        changedNotifiers = DatabaseChangedNotifiers(mock(), mock())
        galleryDao = database.galleryDao()

        capturedCommands.clear()
        provider = GalleryDataProvider(mockContext, transactions, galleryDao, mock(), mock(), mock()) { capturedCommands.add(it) }
    }

    @Test
    fun `should deal with empty data`() {
        fun check(@ViewType type: Int) {
            fun <T> desc(matcher: Matcher<T>) = describedMore(matcher, "for type $type")

            val ds = consDataSource(type)

            assertThat(ds.totalCount, desc(equalTo(0)))
            assertThat(ds.getData(0, 10), desc(hasSize(0)))
        }

        listOf(ViewTypes.DAILY, ViewTypes.VISTA, ViewTypes.NO_SECTIONS).forEach(::check)
    }

    @Test
    fun `should survive misplaced initial position`() {
        createHeaderWithItems(0, 1)

        val ds = consDataSource(ViewTypes.DAILY)

        val list = PagedList.Builder(ds, 3)
                .setFetchExecutor { it.run() }
                .setNotifyExecutor { it.run() }
                .setInitialKey(100500)
                .build()

        assertThat(list, hasSize(2))
        assertThat(list, everyItem(not(nullValue())))
    }

    @Test
    fun `should respond daily`() {
        createHeaderWithItems(3, 1)
        createHeaderWithItems(2, 2)
        createHeaderWithItems(0, 3)

        val ds = consDataSource(ViewTypes.DAILY)

        val data: List<GalleryItem?> = ds.getData(0, 10)
        assertThat(data, hasSize(ds.totalCount))

        assertThat(data.slice(listOf(0, 2, 5)), everyItem(instanceOf(Section::class.java)))
        assertThat(data.slice(listOf(1, 3, 4, 6, 7, 8)), everyItem(instanceOf(MediaItem::class.java)))

        val sections = data.mapNotNull { it as? Section }

        assertThat(sections.map { it.index }, equalTo(listOf(0, 1, 2)))
        assertThat(sections.map { it.count }, equalTo(listOf(1, 2, 3)))
        assertThat(sections.map { it.countBefore }, equalTo(listOf(0, 1, 3)))
    }

    @Test
    fun `should iterate through daily`() {
        createHeaderWithItems(3, 1)
        createHeaderWithItems(2, 2)
        createHeaderWithItems(0, 3)

        val ds = consDataSource(ViewTypes.DAILY)

        fun identify(item: GalleryItem?): Int? =
                (item as? Section)?.let { 0 } ?: (item as? MediaItem)?.mediaStoreId?.toInt()

        val expected = listOf<Int?>(0, 1, 0, 2, 1, 0, 3, 2, 1)

        fun iterate(limit: Int): List<Int?> =
                (0..expected.size step limit).flatMap { ds.getData(it, limit).map(::identify) }

        (1..expected.size).forEach {
            assertThat(iterate(it), describedMore(equalTo(expected), "for limit $it"))
        }
    }

    @Test
    fun `should respond vista`() {
        createHeaderWithItems(100, 1)

        createHeaderWithItems(72, 1)
        createHeaderWithItems(71, 3)

        createHeaderWithItems(40, 1)

        createHeaderWithItems(1, 2)
        createHeaderWithItems(0, 3)

        val ds = consDataSource(ViewTypes.VISTA, 2)

        val data: List<GalleryItem?> = ds.getData(0, 10)
        assertThat(data, hasSize(ds.totalCount))

        assertThat(data.slice(listOf(0, 2, 5, 7)), everyItem(instanceOf(Section::class.java)))
        assertThat(data.slice(listOf(1, 3, 4, 6, 8, 9)), everyItem(instanceOf(MediaItem::class.java)))

        val sections = data.mapNotNull { it as? Section }

        assertThat(sections.map { it.index }, equalTo(listOf(0, 1, 2, 3)))
        assertThat(sections.map { it.count }, equalTo(listOf(1, 2, 1, 2)))
        assertThat(sections.map { it.countBefore }, equalTo(listOf(0, 1, 3, 4)))
        assertThat(sections.map { it.limitlessCountBefore }, equalTo(listOf(0, 1, 5, 6)))
    }

    @Test
    fun `should iterate through vista`() {
        createHeaderWithItems(100, 1)

        createHeaderWithItems(72, 1)
        createHeaderWithItems(71, 3)

        createHeaderWithItems(40, 1)

        createHeaderWithItems(1, 2)
        createHeaderWithItems(0, 3)

        val ds = consDataSource(ViewTypes.VISTA, 2)

        fun identify(item: GalleryItem?): Int? =
                (item as? Section)?.let { 0 } ?: (item as? MediaItem)?.mediaStoreId?.toInt()

        val expected = listOf<Int?>(0, 1, 0, 1, 3, 0, 1, 0, 2, 1)

        fun iterate(limit: Int): List<Int?> =
                (0..expected.size step limit).flatMap { ds.getData(it, limit).map(::identify) }

        (1..expected.size).forEach {
            assertThat(iterate(it), describedMore(equalTo(expected), "for limit $it"))
        }
    }

    @Test
    fun `should respond no sections`() {
        createHeaderWithItems(3, 1)
        createHeaderWithItems(2, 2)
        createHeaderWithItems(0, 3)

        val ds = consDataSource(ViewTypes.NO_SECTIONS)

        val data: List<GalleryItem?> = ds.getData(0, 10)

        assertThat(data, hasSize(ds.totalCount))
        assertThat(data, everyItem(instanceOf(MediaItem::class.java)))

        assertThat(data.map { (it as MediaItem).mediaStoreId }, equalTo(listOf<Long?>(1, 2, 1, 3, 2, 1)))
    }

    @Test
    fun `should iterate through no sections`() {
        createHeaderWithItems(3, 1)
        createHeaderWithItems(2, 2)
        createHeaderWithItems(0, 3)

        val ds = consDataSource(ViewTypes.NO_SECTIONS)

        fun identify(item: GalleryItem?): Int? = (item as? MediaItem)?.mediaStoreId?.toInt()

        val expected = listOf<Int?>(1, 2, 1, 3, 2, 1)

        fun iterate(limit: Int): List<Int?> =
                (0..expected.size step limit).flatMap { ds.getData(it, limit).map(::identify) }

        (1..expected.size).forEach {
            assertThat(iterate(it), describedMore(equalTo(expected), "for limit $it"))
        }
    }

    @Test
    fun `should append nulls for count mismatched headers`() {
        createHeaderWithItems(3, 0, 1)
        createHeaderWithItems(2, 1, 2)
        createHeaderWithItems(0, 1, 3)

        val ds = consDataSource(ViewTypes.DAILY)

        val data: List<GalleryItem?> = ds.getData(0, 10)
        assertThat(data, hasSize(9))

        assertThat(data.slice(listOf(1, 4, 7, 8)), everyItem(nullValue()))
        assertThat(data.slice(listOf(0, 2, 3, 5, 6)), everyItem(not(nullValue())))
    }

    @Test
    fun `should report count mismatched headers`() {
        createHeaderWithItems(4, 1, 1)
        createHeaderWithItems(3, 0, 2)
        createHeaderWithItems(2, 3, 2)
        createHeaderWithItems(1, 1, 1)

        val daily = consDataSource(ViewTypes.DAILY)

        assertThat(captureRecountRequest(daily, 0, 10), equalTo(2..3))
        assertThat(captureRecountRequest(daily, 2, 4), equalTo(3..3))
        assertThat(captureRecountRequest(daily, 7, 1), equalTo(2..2))
        assertThat(captureRecountRequest(daily, 8, 10), nullValue())

        val noSects = consDataSource(ViewTypes.NO_SECTIONS)

        assertThat(captureRecountRequest(noSects, 0, 10), equalTo(1..4))
        assertThat(captureRecountRequest(noSects, 2, 2), equalTo(2..3))
        assertThat(captureRecountRequest(noSects, 4, 1), equalTo(2..2))
        assertThat(captureRecountRequest(noSects, 5, 10), nullValue())
    }

    @Test
    fun `should not report limit overflown count mismatch`() {
        createHeaderWithItems(41, 1, 2)
        createHeaderWithItems(40, 1, 2)

        createHeaderWithItems(3, 1, 1)
        createHeaderWithItems(2, 0, 1)
        createHeaderWithItems(1, 3, 2)

        val vista = consDataSource(ViewTypes.VISTA, 3)

        assertThat(captureRecountRequest(vista, 0, 10), equalTo(40..41))
        assertThat(captureRecountRequest(vista, 4, 10), nullValue())
    }

    @Test
    fun `should filter by media type`() {
        fun getCounts(ds: GalleryDataSource): List<Int> {
            return ds.sections.sections.map { it.count }
        }

        fun getItems(ds: GalleryDataSource): List<Int> {
            return ds.getData(0, 10).map { item ->
                (item as? Section)?.let { 0 } ?: (item as? MediaItem)?.mediaStoreId!!.toInt()
            }
        }

        createHeader(0, HeaderCounts(images = 1, videos = 0, total = 1))
        createItem(0, 1L, MediaTypes.IMAGE)

        createHeader(1, HeaderCounts(images = 0, videos = 1, total = 2))
        createItem(1, 2L, MediaTypes.UNKNOWN)
        createItem(1, 3L, MediaTypes.VIDEO)

        createHeader(2, HeaderCounts(images = 1, videos = 2, total = 3))
        createItem(2, 4L, MediaTypes.VIDEO)
        createItem(2, 5L, MediaTypes.IMAGE)
        createItem(2, 6L, MediaTypes.VIDEO)

        var ds = consDataSource(ViewTypes.DAILY, mediaType = null)

        assertThat(getCounts(ds), equalTo(listOf(3, 2, 1)))
        assertThat(getItems(ds), equalTo(listOf(0, 6, 5, 4, 0, 3, 2, 0, 1)))

        ds = consDataSource(ViewTypes.DAILY, mediaType = MediaTypes.IMAGE)

        assertThat(getCounts(ds), equalTo(listOf(1, 1)))
        assertThat(getItems(ds), equalTo(listOf(0, 5, 0, 1)))

        ds = consDataSource(ViewTypes.DAILY, mediaType = MediaTypes.VIDEO)

        assertThat(getCounts(ds), equalTo(listOf(2, 1)))
        assertThat(getItems(ds), equalTo(listOf(0, 6, 4, 0, 3)))
    }

    private fun dayMs(a: Long): Long = TimeUnit.DAYS.toMillis(a)

    private fun dayMs(a: Int): Long = dayMs(a.toLong())

    private fun dayOfMs(a: Long): Int = TimeUnit.MILLISECONDS.toDays(a).toInt()

    private fun createHeaderWithItems(day: Int, itemsCount: Int, headerCount: Int = itemsCount) {
        createHeader(day, HeaderCounts(0, 0, headerCount))
        (1..itemsCount).map { createItem(day, it.toLong()) }
    }

    private fun createHeader(day: Int, counts: HeaderCounts) {
        galleryDao.insertHeader(Header(null, ALBUM, dayMs(day), dayMs(day + 1) - 1,
                counts.total, counts.images, counts.videos))
    }

    private fun createItem(day: Int, mediaStoreId: Long, mediaType: String = MediaTypes.UNKNOWN) {
        galleryDao.insertMediaItem(consMediaItem(
                mediaStoreId = mediaStoreId, eTime = dayMs(day),
                bucketId = ALBUM.bucketId, mimeType = "$mediaType/*"))
    }

    private fun consDataSource(
            @ViewType viewType: Int, itemsInSection: Int? = null,
            sections: Int? = null, mediaType: String? = null): GalleryDataSource {

        val filter = AlbumItemsFilter(ALBUM, mediaType)
        val continuousGalleryDataSourceFactory = mock<ContinuousGalleryDataSourceFactory> {
            whenever(it.create(anyOrNull(), anyOrNull(), anyOrNull())).thenAnswer { invocation ->
                ContinuousGalleryDataSource(
                    provider,
                    changedNotifiers,
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                )
            }
        }
        val sectionedFactory = mock<SectionedGalleryDataSourceFactory> {
            whenever(it.create(anyOrNull(), anyOrNull(), anyOrNull())).thenAnswer { invocation ->
                SectionedGalleryDataSource(
                    provider,
                    changedNotifiers,
                    invocation.getArgument(0),
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                )
            }
        }
        val processorFactory = WowGridGenerationProcessorsFactory(mock())
        val factory = GalleryDataSourceFactory(
            sectionedFactory,
            continuousGalleryDataSourceFactory,
            processorFactory
        )
        return factory.create(SectionsRequest(viewType, filter, AlbumItemsLimits(sections, itemsInSection)))
    }

    private fun captureRecountRequest(ds: GalleryDataSource, start: Int, size: Int): IntRange?  {
        capturedCommands.clear()

        ds.getData(start, size)

        return capturedCommands
                .map { (it as RecountHeadersCommandRequest).interval }
                .map { dayOfMs(it.start)..dayOfMs(it.end) }
                .singleOrNull()
    }

    private fun <T> describedMore(matcher: Matcher<T>, text: String): Matcher<T> = object: BaseMatcher<T>() {
        override fun describeTo(description: Description) {
            matcher.describeTo(description)
            description.appendText(" $text")
        }

        override fun matches(item: Any?) = matcher.matches(item)
    }
}
