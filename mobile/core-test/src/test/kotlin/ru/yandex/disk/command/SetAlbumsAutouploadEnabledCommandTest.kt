package ru.yandex.disk.command

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.gallery.data.sync.PhotosliceItemsHelper
import ru.yandex.disk.domain.albums.BucketAlbum
import ru.yandex.disk.domain.albums.AlbumId
import ru.yandex.disk.provider.BucketAlbumsProvider
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.settings.command.SetAlbumsAutouploadStateCommand
import ru.yandex.disk.settings.command.SetAlbumsAutouploadStateCommandRequest
import ru.yandex.disk.stats.EventLog
import ru.yandex.disk.upload.DiskUploader
import ru.yandex.disk.upload.UploadQueue

private const val MOCK_PATH = "mock/path"

class SetAlbumsAutouploadEnabledCommandTest {

    private val albumsProvider = mock<BucketAlbumsProvider>()
    private val diskUploader = mock<DiskUploader>()
    private val uploadQueue = mock<UploadQueue>()
    private val commandStarter = mock<CommandStarter>()
    private val photoAutoUploadSettings = mock<AutoUploadSettings> {
        on { anyAutouploadEnabled() } doReturn true
    }
    private val userSettings = mock<UserSettings> {
        on { autoUploadSettings } doReturn photoAutoUploadSettings
    }
    private val photosliceItemsHelper = mock<PhotosliceItemsHelper>()
    private val command = SetAlbumsAutouploadStateCommand(albumsProvider, diskUploader,
        uploadQueue, commandStarter, userSettings, photosliceItemsHelper)

    @Test
    fun `should enable album autoupload`() {
        val album = createAlbum()
        val request = SetAlbumsAutouploadStateCommandRequest(album, true)

        command.execute(request)

        verify(albumsProvider).setAlbumAutouploadState(same(album), eq(BucketAlbum.AutoUploadMode.ENABLED))
    }

    @Test
    fun `should clear queue if autoupload disabed`() {
        val request = createRequest(enabledBefore = true, enabledAfter = false)

        command.execute(request)

        verify(uploadQueue).removeAutouploadsFromDir(eq(MOCK_PATH))
    }

    @Test
    fun `should mark queue changed if something removed`() {
        val request = createRequest(enabledBefore = true, enabledAfter = false)
        whenever(uploadQueue.removeAutouploadsFromDir(any())).doReturn(1)

        command.execute(request)

        verify(diskUploader).markQueueChanged()
    }

    @Test
    fun `should not trigger autoupload if autoupload disabled`() {
        val request = createRequest(enabledBefore = false, enabledAfter = true)
        whenever(photoAutoUploadSettings.anyAutouploadEnabled()).doReturn(false)

        command.execute(request)

        verify(commandStarter, never()).start(any())
    }

    @Test
    fun `should not trigger autoupload if album uploading disabled`() {
        val request = createRequest(enabledBefore = true, enabledAfter = false)

        command.execute(request)

        verify(commandStarter, never()).start(any())
    }

    @Test
    fun `should invalidate photoslice items helper`() {
        val request = createRequest(enabledBefore = true, enabledAfter = false)

        command.execute(request)

        verify(photosliceItemsHelper).notifyPhotosliceAlbumsChanged()
    }

    private fun createRequest(enabledBefore: Boolean = false, enabledAfter: Boolean = true): SetAlbumsAutouploadStateCommandRequest {
        val album = createAlbum(enabledBefore)
        return SetAlbumsAutouploadStateCommandRequest(album, enabledAfter)
    }

    private fun createAlbum(uploadEnabled: Boolean = false): BucketAlbum {
        val state = if (uploadEnabled) BucketAlbum.AutoUploadMode.ENABLED else BucketAlbum.AutoUploadMode.DISABLED
        return BucketAlbum(AlbumId.bucket("1234567890"), "Mock album", 1, 0, state, MOCK_PATH,
            0, false, true, true)
    }
}
