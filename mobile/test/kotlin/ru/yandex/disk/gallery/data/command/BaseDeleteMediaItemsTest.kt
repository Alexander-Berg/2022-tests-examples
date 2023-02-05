package ru.yandex.disk.gallery.data.command

import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.gallery.data.MediaStoreDeleteInProgressRegistry
import ru.yandex.disk.gallery.data.event.MissingOpenTreePermissionEvent
import ru.yandex.disk.gallery.data.model.MediaItem
import ru.yandex.disk.gallery.data.model.SyncStatuses
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.utils.MimeTypeUtils

abstract class BaseDeleteMediaItemsTest : TestCase2() {

    protected val eventSender = mock<EventSender>()

    private fun createMediaItem(id: Long) = MediaItem(id, null, MimeTypeUtils.UNKNOWN_MIME_TYPE,
            0L, id, null, SyncStatuses.UPLOADED, null, null, 0.0, 0.0)

    protected fun createItemsList(size: Int = 1) = createItemsList(0, size)

    protected fun createItemsList(from: Int, to: Int) = (from until to).map { createMediaItem(it.toLong()) }

    protected fun verifyMissingPermissionEventSentForStorage(storage: String) {
        verify(eventSender)
                .send(argThat { (this as MissingOpenTreePermissionEvent).storage == storage })
    }

    protected fun MediaStoreDeleteInProgressRegistry.shouldSkipFileSync(item: MediaItem) =
            shouldSkipFileSync(item.mediaStoreId)
}
