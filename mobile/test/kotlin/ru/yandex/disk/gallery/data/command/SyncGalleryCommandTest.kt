package ru.yandex.disk.gallery.data.command

import org.mockito.kotlin.*
import org.junit.Before
import org.junit.Test
import ru.yandex.disk.gallery.data.MediaStoreDeleteInProgressRegistry
import ru.yandex.disk.gallery.data.provider.MediaStoreProvider
import ru.yandex.disk.gallery.data.sync.MediaStoreSyncerProcessor
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.test.TestCase2

class SyncGalleryCommandTest: TestCase2() {

    private var hasPermission = true

    private val mediaStoreProvider = mock<MediaStoreProvider> { _ ->
        on { hasReadMediaStorePermission() } doAnswer { hasPermission }
    }

    private val mediaStoreProcessorProvider = mock<MediaStoreSyncerProcessor>()

    private val command = SyncGalleryCommand({ mediaStoreProvider }, mock(), mock(),
        { mock() }, { mediaStoreProcessorProvider },
        mock(), CommandLogger(), MediaStoreDeleteInProgressRegistry())

    @Before
    override fun before() {
        super.before()

        hasPermission = true
    }

    @Test
    fun `should not sync if no permission`() {
        hasPermission = false

        command.execute(SyncGalleryCommandRequest())

        verify(mediaStoreProvider).hasReadMediaStorePermission()
        verifyNoMoreInteractions(mediaStoreProvider)
    }

    @Test
    fun `should not start sync by not force requests if no error`() {
        command.execute(SyncGalleryCommandRequest(false))

        verify(mediaStoreProvider, never()).hasReadMediaStorePermission()
    }

    @Test
    fun `should start sync by not force request if occurred an error during start media store processor`() {
        whenever(mediaStoreProcessorProvider.start(any())).doReturn(false)
        command.execute(SyncGalleryCommandRequest())

        command.execute(SyncGalleryCommandRequest(false))

        verify(mediaStoreProvider, times(2)).hasReadMediaStorePermission()
    }
}
