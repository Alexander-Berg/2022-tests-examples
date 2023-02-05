package ru.yandex.disk.gallery.data.command

import android.net.Uri
import org.mockito.kotlin.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import ru.yandex.disk.domain.albums.PhotosliceAlbumId
import ru.yandex.disk.gallery.data.AlbumMediaItemSource
import ru.yandex.disk.gallery.data.MediaStoreDeleteInProgressRegistry
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.storage.FileDeleteProcessor
import ru.yandex.disk.storage.MissingOpenTreePermissionException
private const val MOCK_STORAGE = "storage"

class DeleteMediaItemsFromStorageCommandTest : BaseDeleteMediaItemsTest() {

    private val deleteProcessor = mock<FileDeleteProcessor>()
    private val commandStarter = mock<CommandStarter>()
    private val deleteInProgressRegistry = MediaStoreDeleteInProgressRegistry()
    private val command = DeleteMediaItemsFromStorageCommand(deleteInProgressRegistry, eventSender,
            deleteProcessor, commandStarter)

    @Test
    fun `should start delete processor`() {
        command.execute(deleteFromStorageCommandRequest(createItemsList()))

        verify(deleteProcessor).delete(any<Uri>())
    }

    @Test
    fun `should send correct storage in error`() {
        setupException()

        command.execute(deleteFromStorageCommandRequest(createItemsList()))

        verifyMissingPermissionEventSentForStorage(MOCK_STORAGE)
    }

    @Test
    fun `should start sync on error`() {
        setupException()

        command.execute(deleteFromStorageCommandRequest(createItemsList()))

        verify(commandStarter).start(argThat { javaClass == SyncGalleryCommandRequest::class.java })
    }

    @Test
    fun `should stop delete when missing permission is detected`() {
        setupException()

        command.execute(deleteFromStorageCommandRequest(createItemsList(3)))

        verify(deleteProcessor).delete(any<Uri>())
    }

    @Test
    fun `should remove items from DeleteInProgressRegistry`() {
        val items = createItemsList()
        val item = items.first()
        deleteInProgressRegistry.add(item)

        command.execute(deleteFromStorageCommandRequest(items))

        assertNotInDeleteInProgressRegistry(item)
    }

    @Test
    fun `should remove all not processed items from DeleteInProgressRegistry`() {
        setupException()
        val items = createItemsList(3)
        items.forEach<MediaItem> {
            deleteInProgressRegistry.add(it)
        }

        command.execute(deleteFromStorageCommandRequest(items))

        items.forEach<MediaItem> {
            assertNotInDeleteInProgressRegistry(it)
        }
    }

    fun setupException() {
        whenever(deleteProcessor.delete(any<Uri>())) doThrow MissingOpenTreePermissionException(MOCK_STORAGE)
    }

    private fun assertNotInDeleteInProgressRegistry(item: MediaItem) {
        assertThat(deleteInProgressRegistry.shouldSkipFileSync(item), equalTo(false))
    }

    companion object {
        fun deleteFromStorageCommandRequest(items: List<MediaItem>): DeleteMediaItemsFromStorageCommandRequest {
            return DeleteMediaItemsFromStorageCommandRequest(items, AlbumMediaItemSource(PhotosliceAlbumId))
        }
    }
}
