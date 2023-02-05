package ru.yandex.disk.data.command

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.DiskItemFactory
import ru.yandex.disk.FileItem
import ru.yandex.disk.data.QueryDiskItemsCommand
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.feed.data.FeedProvider
import ru.yandex.disk.files.FilesProvider
import ru.yandex.disk.gallery.data.AlbumMediaItemSource
import ru.yandex.disk.gallery.data.FeedBlockMediaItemSource
import ru.yandex.disk.gallery.data.FileListMediaItemSource
import ru.yandex.disk.gallery.data.OfflineMediaItemSource
import ru.yandex.disk.gallery.data.ServerSearchMediaItemSource
import ru.yandex.disk.gallery.data.command.QueryDiskItemsCommandRequest
import ru.yandex.disk.gallery.data.event.FileItemsMissedEvent
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.photoslice.MomentsDatabase
import ru.yandex.disk.provider.DiskDatabase
import ru.yandex.disk.settings.DefaultFolderSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.utils.MimeTypeUtils

class QueryDiskItemsCommandTest : TestCase2() {

    private val eventSender = mock<EventSender>()

    private val defaultFolderSettings = mock<DefaultFolderSettings> {
        on { photostreamFolder } doReturn ""
    }

    private val userSettings = mock<UserSettings> {
        on { defaultFolderSettings } doReturn defaultFolderSettings
    }

    private val diskDatabase = mock<DiskDatabase>()

    private val momentsDatabase = mock<MomentsDatabase> {
        on { queryDiskItemsByETags(any()) } doReturn listOf(createDiskItem())
    }

    private val feedProvider = mock<FeedProvider> {
        on { queryDiskItems(any(), any()) } doReturn listOf(createDiskItem())
    }

    private val filesProvider = mock<FilesProvider> {
        on { queryDiskItemsFromDir(any()) } doReturn listOf(createDiskItem())
        on { queryOfflineDiskItems(any()) } doReturn listOf(createDiskItem())
        on { querySearchResultDiskItems(any()) } doReturn listOf(createDiskItem())
    }

    private val command = QueryDiskItemsCommand(userSettings, diskDatabase, momentsDatabase, feedProvider, filesProvider, eventSender)

    @Test
    fun `should get items from moments database`() {
        val data = listOf(createMixedMediaItem())

        command.execute(queryItemsCommandRequest(data))

        verify(momentsDatabase).queryDiskItemsByETags(any())
    }

    @Test
    fun `should return items for server files`() {
        val data = listOf(createServerMediaItem())

        command.execute(queryItemsCommandRequest(data))

        assertItemsRequested(1)
    }

    @Test
    fun `should return items for mixed files`() {
        val data = listOf(createMixedMediaItem())

        command.execute(queryItemsCommandRequest(data))

        assertItemsRequested(1)
    }

    @Test
    fun `should send missed event for local only items`() {
        val data = listOf(createLocalMediaItem())

        command.execute(queryItemsCommandRequest(data))

        verify(eventSender).send(any<FileItemsMissedEvent>())
    }

    @Test
    fun `should skip local items for files list`() {
        val data = listOf(createServerMediaItem("1"), createMixedMediaItem("2"), createLocalMediaItem("3"))

        command.execute(queryItemsCommandRequest(data))

        assertItemsRequested(2)
    }

    @Test
    fun `should not read from feed provider if source is photoslice`() {
        val data = listOf(createServerMediaItem(), createMixedMediaItem(), createLocalMediaItem())

        command.execute(QueryDiskItemsCommandRequest(data, mediaItemSource = AlbumMediaItemSource(PhotosliceAlbumId)))

        verify(feedProvider, never()).queryDiskItems(any(), any())
    }

    @Test
    fun `should read from feed provider if source is feed block`() {
        val data = listOf(createServerMediaItem())

        command.execute(QueryDiskItemsCommandRequest(data, mediaItemSource = FeedBlockMediaItemSource(201)))

        verify(feedProvider).queryDiskItems(any(), any())
    }

    @Test
    fun `should read from files provider if source is file list`() {
        val data = listOf(createServerMediaItem())

        command.execute(QueryDiskItemsCommandRequest(data, mediaItemSource = FileListMediaItemSource))

        verify(filesProvider).queryDiskItemsFromDir(any())
    }

    @Test
    fun `should read from files provider if source is offline list`() {
        val data = listOf(createServerMediaItem())

        command.execute(QueryDiskItemsCommandRequest(data, mediaItemSource = OfflineMediaItemSource))

        verify(filesProvider).queryOfflineDiskItems(any())
    }

    @Test
    fun `should read from files provider if source is server search list`() {
        val data = listOf(createServerMediaItem())

        command.execute(QueryDiskItemsCommandRequest(data, mediaItemSource = ServerSearchMediaItemSource))

        verify(filesProvider).querySearchResultDiskItems(any())
    }

    private fun createServerMediaItem(eTag: String = "") = createMediaItem(diskItemId = 0, serverPath = "", serverETag = eTag)

    private fun createLocalMediaItem(eTag: String = "") = createMediaItem(mediaStoreId = 0, serverETag = eTag)

    private fun createMixedMediaItem(eTag: String = "") = createMediaItem(mediaStoreId = 0, serverPath = "", serverETag = eTag)

    private fun createMediaItem(mediaStoreId: Long? = null, diskItemId: Long? = null, serverPath: String? = null, serverETag: String = "") =
            MediaItem(0, diskItemId, MimeTypeUtils.GENERIC_TYPE, 0, mediaStoreId, null,
                    SyncStatuses.UNRESOLVED, serverETag, serverPath, 0.0, 0.0)

    private fun assertItemsRequested(count: Int) {
        verify(momentsDatabase).queryDiskItemsByETags(argThat { size == count })
    }

    private fun createDiskItem() = DiskItemFactory.create("", null, "", 0, 0, false, "", "", false,
            false, "", 0, "", "", FileItem.OfflineMark.NOT_MARKED, "", false, 0)

    companion object {
        private fun queryItemsCommandRequest(items: List<MediaItem>): QueryDiskItemsCommandRequest {
            return QueryDiskItemsCommandRequest(items, mediaItemSource = AlbumMediaItemSource(PhotosliceAlbumId))
        }
    }
}
