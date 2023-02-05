package ru.yandex.disk.gallery.data.command

import android.content.Context
import org.mockito.kotlin.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.gallery.data.AlbumMediaItemSource
import ru.yandex.disk.gallery.data.MediaStoreDeleteInProgressRegistry
import ru.yandex.disk.gallery.data.database.GalleryDataProvider
import ru.yandex.disk.gallery.data.event.MissingOpenTreePermissionEvent
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.provider.MediaStoreProvider
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.storage.DocumentsTreeManager
import ru.yandex.util.Path

private const val NORMAL_PATH = "/normal/path"
private const val BAD_ROOT = "/bad/"
private const val BAD_PATH = "/bad/path"

class DeleteMediaItemsCommandTest : BaseDeleteMediaItemsTest() {

    private val context = mock<Context>()
    private val mediaStoreProvider = mock<MediaStoreProvider>()
    private val galleryDataProvider = mock<GalleryDataProvider>()
    private val commandStarter = mock<CommandStarter>()
    private val documentsTreeManager = mock<DocumentsTreeManager>()
    private val deleteInProgressRegistry = MediaStoreDeleteInProgressRegistry()
    private val command = DeleteMediaItemsCommand(context, mediaStoreProvider, eventSender, galleryDataProvider, mock(),
            commandStarter, documentsTreeManager, deleteInProgressRegistry)

    @Test
    fun `should start delete from storage command`() {
        setupNotPermittedRoots()
        setupPaths(NORMAL_PATH)

        val items = createItemsList(5)
        command.execute(deleteCommandRequest(items))

        verifyDeleteFromStorageStarted(items)
    }

    @Test
    fun `should not start delete for empty list`() {
        setupNotPermittedRoots()
        setupPaths(NORMAL_PATH)

        command.execute(deleteCommandRequest(createItemsList(0)))

        verifyDeleteFromStorageNotStarted()
    }

    @Test
    fun `should not start delete for not permitted path`() {
        setupNotPermittedRoots(BAD_ROOT)
        setupPaths(BAD_PATH)

        command.execute(deleteCommandRequest(createItemsList()))
        verifyDeleteFromStorageNotStarted()
    }

    @Test
    fun `should send missing open tree permission event for not permitted path`() {
        setupNotPermittedRoots(BAD_ROOT)
        setupPaths(BAD_PATH)

        val items = createItemsList(5)
        command.execute(deleteCommandRequest(items))

        verifyMissingPermissionEventSent(items)
    }

    @Test
    fun `should take correct name of root`() {
        setupNotPermittedRoots(BAD_ROOT)
        setupPaths(BAD_PATH)

        val items = createItemsList()
        command.execute(deleteCommandRequest(items))

        verifyMissingPermissionEventSentForStorage("/bad")
    }

    @Test
    fun `should start delete normal path if bad root exist root`() {
        setupNotPermittedRoots(BAD_ROOT)
        setupPaths(NORMAL_PATH)

        val items = createItemsList()
        command.execute(deleteCommandRequest(items))

        verifyDeleteFromStorageStarted(items)
    }

    @Test
    fun `should delete from database before start delete from storage`() {
        setupNotPermittedRoots()
        setupPaths(NORMAL_PATH)

        command.execute(deleteCommandRequest(createItemsList()))

        verify(galleryDataProvider).deleteItemsAndRecountHeaders(any<List<MediaItem>>())
    }

    @Test
    fun `should not delete if one file in batch requires permission`() {
        setupNotPermittedRoots(BAD_ROOT)
        setupPaths(NORMAL_PATH, BAD_PATH)

        val items = createItemsList(2)
        command.execute(deleteCommandRequest(items))

        verifyDeleteFromStorageNotStarted()
    }


    @Test
    fun `should add items to DeleteInProgressRegistry`() {
        setupNotPermittedRoots()
        setupPaths(NORMAL_PATH)

        val items = createItemsList(5)
        command.execute(deleteCommandRequest(items))

        verifyDeleteInProgressRegistry(items)
    }

    @Test
    fun `should not add to DeleteInProgressRegistry if root is not permitted`() {
        setupNotPermittedRoots(BAD_ROOT)
        setupPaths(BAD_PATH)

        command.execute(deleteCommandRequest(createItemsList()))
        assertThat(deleteInProgressRegistry.totalAdded, equalTo(0))
    }

    private fun setupNotPermittedRoots(vararg roots: String) {
        val paths = roots.map { Path.asPath(it) }
        whenever(documentsTreeManager.rootsWithMissingPermission) doReturn(paths)
    }

    private fun setupPaths(vararg paths: String) {
        whenever(mediaStoreProvider.queryPaths(any(), any())) doReturn(paths.toList())
    }

    private fun setupPaths(items: List<MediaItem>, vararg paths: String) {
        val imagesIds = items.map { it.mediaStoreId!! }
        whenever(mediaStoreProvider.queryPaths(eq(imagesIds), eq(emptyList()))) doReturn(paths.toList())
    }

    private fun verifyDeleteFromStorageStarted(items: List<MediaItem>) {
        verify(commandStarter)
                .start(argThat { (this as DeleteMediaItemsFromStorageCommandRequest).items == items })
    }

    private fun verifyDeleteFromStorageNotStarted() {
        verify(commandStarter, never())
                .start(argThat { javaClass == DeleteMediaItemsFromStorageCommandRequest::class.java })
    }

    private fun verifyMissingPermissionEventSent(items: List<MediaItem>) {
        verify(eventSender)
                .send(argThat { (this as MissingOpenTreePermissionEvent).itemsToDelete == items })
    }

    private fun verifyDeleteInProgressRegistry(items: List<MediaItem>) {
        assertThat(deleteInProgressRegistry.totalAdded, equalTo(items.size))
        items.forEach {
            assertThat(deleteInProgressRegistry.shouldSkipFileSync(it), equalTo(true))
        }
    }

    companion object {
        fun deleteCommandRequest(items: List<MediaItem>): DeleteMediaItemsCommandRequest {
            return DeleteMediaItemsCommandRequest(items, AlbumMediaItemSource(PhotosliceAlbumId))
        }
    }
}
