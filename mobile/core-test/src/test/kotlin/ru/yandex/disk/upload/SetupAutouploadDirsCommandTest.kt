package ru.yandex.disk.upload

import org.mockito.kotlin.*
import org.junit.Test
import ru.yandex.disk.gallery.data.sync.PhotosliceItemsHelper
import ru.yandex.disk.domain.albums.BucketAlbum
import ru.yandex.disk.domain.albums.BucketAlbumId
import ru.yandex.disk.provider.BucketAlbumsProvider
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.settings.AlbumAutouploadSettings
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.UserSettings

private const val MOCK_PATH = "/storage/emulated/0"

class SetupAutouploadDirsCommandTest {

    private val albumsProvider = mock<BucketAlbumsProvider> {
        on { getAlbumsSync() } doReturn emptyList()
        on { hasReadPermission() } doReturn true
    }
    private val photoAutouploadSettings = mock<AutoUploadSettings> {
        on { anyAutouploadEnabled() } doReturn true
    }
    private val userSettings = mock<UserSettings> {
        on { autoUploadSettings } doReturn photoAutouploadSettings
    }
    private val albumAutouploadSettings = mock<AlbumAutouploadSettings>()
    private val commandLogger = CommandLogger()
    private val uploadQueue = mock<UploadQueue> {
        on { queryUploadedSrcParentsForUser() } doReturn emptyList()
    }
    private val uploader = mock<DiskUploader>()
    private val photosliceHelper = mock<PhotosliceItemsHelper>()
    private val command = SetupAutouploadDirsCommand(albumsProvider, userSettings,
        albumAutouploadSettings, commandLogger, uploadQueue, uploader, photosliceHelper)
    private val request = SetupAutouploadDirsCommandRequest()

    @Test
    fun `should add files from albums provider`() {
        setupAlbumsProvider()

        command.execute(request)

        verify(albumAutouploadSettings).setUploadingState(eq(MOCK_PATH), eq(BucketAlbum.AutoUploadMode.ENABLED))
    }

    @Test
    fun `should add files from upload queue`() {
        whenever(uploadQueue.queryUploadedSrcParentsForUser()).thenReturn(listOf(MOCK_PATH))

        command.execute(request)

        verify(albumAutouploadSettings).setUploadingState(eq(MOCK_PATH), eq(BucketAlbum.AutoUploadMode.ENABLED))
    }

    @Test
    fun `should notify autoupload invalidated`() {
        setupAlbumsProvider()

        command.execute(request)

        verify(albumsProvider).invalidate()
        verify(uploader).markQueueChanged()
    }

    @Test
    fun `should not notify if nothing found`() {
        command.execute(request)

        verify(uploader, never()).markQueueChanged()
    }

    @Test
    fun `should always invalidate albums provider`() {
        command.execute(request)

        verify(albumsProvider).invalidate()
    }

    @Test
    fun `should not setup if no permissions`() {
        whenever(albumsProvider.hasReadPermission()) doReturn false
        setupAlbumsProvider()

        command.execute(request)

        verify(userSettings, never()).isAutouploadSetupCompleted = eq(true)
    }

    private fun setupAlbumsProvider() {
        whenever(albumsProvider.getAlbumsSync()).thenReturn(listOf(createMockAlbum()))
    }

    private fun createMockAlbum(always: Boolean = false, default: Boolean = true): BucketAlbum {
        return BucketAlbum(BucketAlbumId("MOCK_BUCKET_ID"), "MOCK_NAME", 1, 0,
            BucketAlbum.AutoUploadMode.ENABLED, MOCK_PATH, 0, always, default, true)
    }
}
