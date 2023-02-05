package ru.yandex.disk.service

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.instanceOf
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import ru.yandex.disk.cleanup.CleanupPolicy
import ru.yandex.disk.cleanup.command.CheckForCleanupCommandRequest
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.gallery.data.sync.PhotosliceItemsHelper
import ru.yandex.disk.monitoring.PeriodicJobCommandRequest
import ru.yandex.disk.provider.BucketAlbumsProvider
import ru.yandex.disk.provider.Settings
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.config.CompositeAutoUploadSettings
import ru.yandex.disk.settings.config.PhotoAutoUploadSettings
import ru.yandex.disk.settings.config.VideoAutoUploadSettings
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.toggle.SeparatedAutouploadToggle

private const val INITIAL_CLEANUP_PERIOD: Long = 1

@Config(manifest = Config.NONE)
class SetAutouploadModeCommandTest : AndroidTestCase2() {

    private lateinit var settings: Settings
    private lateinit var commandScheduler: CommandScheduler
    private lateinit var autoUploadSettings: AutoUploadSettings
    private lateinit var photosliceItemsHelper: PhotosliceItemsHelper
    private lateinit var albumsProvider: BucketAlbumsProvider

    private lateinit var command: SetAutouploadModeCommand

    private val cleanupPolicy = mock(CleanupPolicy::class.java).apply {
        whenever(nextInitialCleanupDate).thenReturn(INITIAL_CLEANUP_PERIOD)
    }

    override fun setUp() {
        super.setUp()
        settings = TestObjectsFactory.createSettings(mContext)
        val userSettings = TestObjectsFactory.createUserSettings("test", settings)
        commandScheduler = mock(CommandScheduler::class.java)
        autoUploadSettings = userSettings.autoUploadSettings
        photosliceItemsHelper = mock()
        albumsProvider = mock()

        val photoAutoUploadSettings = PhotoAutoUploadSettings(autoUploadSettings)
        val videoAutoUploadSettings = VideoAutoUploadSettings(autoUploadSettings)
        val compositeAutoUploadSettings = CompositeAutoUploadSettings(
            autoUploadSettings, photoAutoUploadSettings, videoAutoUploadSettings
        )

        command = SetAutouploadModeCommand(
            CommandLogger(), autoUploadSettings, userSettings,
            EventLogger(), commandScheduler, cleanupPolicy,
            mock(), mock(), photosliceItemsHelper, albumsProvider, mock(),
            photoAutoUploadSettings, videoAutoUploadSettings, compositeAutoUploadSettings,
            SeparatedAutouploadToggle(true)
        )
    }

    @Test
    fun `should cancel local reminder if unlim enabled`() {
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.ALWAYS,
            false,
            AutoUploadSettings.UnlimMode.ENABLED)
        command.execute(request)

        val argumentCaptor = ArgumentCaptor.forClass(CommandRequest::class.java)
        verify(commandScheduler, times(1)).cancel(argumentCaptor.capture())
        val allValues = argumentCaptor.allValues
        assertThat(allValues[0], instanceOf(PeriodicJobCommandRequest::class.java))
    }


    @Test
    fun `shoud not enable unlim if user changes autoupload mode from wifi to always`() {
        autoUploadSettings.photounlimMode = AutoUploadSettings.UnlimMode.DISABLED
        autoUploadSettings.uploadPhotoWhen = AutoUploadSettings.UploadWhen.WIFI

        val request = SetAutouploadModeCommandRequest(AutoUploadSettings.UploadWhen.ALWAYS,
            false, AutoUploadSettings.UnlimMode.NOT_SET)
        command.execute(request)

        assertEquals(autoUploadSettings.photounlimMode, AutoUploadSettings.UnlimMode.DISABLED)
    }

    @Test
    fun `shoud not enable unlim if user changes autoupload mode from always to wifi`() {
        autoUploadSettings.photounlimMode = AutoUploadSettings.UnlimMode.DISABLED
        autoUploadSettings.uploadPhotoWhen = AutoUploadSettings.UploadWhen.ALWAYS

        val request = SetAutouploadModeCommandRequest(AutoUploadSettings.UploadWhen.WIFI,
            false, AutoUploadSettings.UnlimMode.NOT_SET)
        command.execute(request)

        assertEquals(autoUploadSettings.photounlimMode, AutoUploadSettings.UnlimMode.DISABLED)
    }

    @Test
    fun `should cancel cleanup push if autoupload canceled`() {
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.NEVER, true,
            AutoUploadSettings.UnlimMode.DISABLED)
        command.execute(request)
        verify(commandScheduler, never()).scheduleAt(any(CheckForCleanupCommandRequest::class.java), anyLong())
        verify(commandScheduler).cancel(any(CheckForCleanupCommandRequest::class.java))
    }

    @Test
    fun `should schedule cleanup push if autoupload enabled`() {
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.ALWAYS, true,
            AutoUploadSettings.UnlimMode.ENABLED)
        command.execute(request)
        verify(commandScheduler, never()).cancel(any(CheckForCleanupCommandRequest::class.java))
        verify(commandScheduler).scheduleAt(any(CheckForCleanupCommandRequest::class.java), eq(INITIAL_CLEANUP_PERIOD))
    }

    @Test
    fun `should invalidate photoslice items helper if changed`() {
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.ALWAYS, true,
            AutoUploadSettings.UnlimMode.ENABLED)
        command.execute(request)
        verify(photosliceItemsHelper).notifyPhotosliceAlbumsChanged()
    }

    @Test
    fun `should invalidate albums provider if changed`() {
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.ALWAYS, true,
            AutoUploadSettings.UnlimMode.ENABLED)
        command.execute(request)
        verify(albumsProvider).invalidate()
    }

    @Test
    fun `should enable both auto uploads if one already enabled`() {
        autoUploadSettings.uploadPhotoWhen = AutoUploadSettings.UploadWhen.WIFI
        autoUploadSettings.uploadVideoWhen = AutoUploadSettings.UploadWhen.NEVER
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.WIFI, AutoUploadSettings.UploadWhen.WIFI,
            true,
            AutoUploadSettings.UnlimMode.NOT_SET, AutoUploadSettings.UnlimMode.NOT_SET
        )
        command.execute(request)
        assertEquals(autoUploadSettings.uploadPhotoWhen, AutoUploadSettings.UploadWhen.WIFI)
        assertEquals(autoUploadSettings.uploadVideoWhen, AutoUploadSettings.UploadWhen.WIFI)
    }

    @Test
    fun `should not clear unlim value if not set received`() {
        autoUploadSettings.uploadPhotoWhen = AutoUploadSettings.UploadWhen.WIFI
        autoUploadSettings.photounlimMode = AutoUploadSettings.UnlimMode.ENABLED
        val request = SetAutouploadModeCommandRequest.createPhotoAutoupload(
            AutoUploadSettings.UploadWhen.ALWAYS, true,
            AutoUploadSettings.UnlimMode.NOT_SET
        )
        command.execute(request)
        assertEquals(autoUploadSettings.photounlimMode, AutoUploadSettings.UnlimMode.ENABLED)
    }

    @Test
    fun `should not change unlim value if different for photo and video`() {
        autoUploadSettings.uploadPhotoWhen = AutoUploadSettings.UploadWhen.WIFI
        autoUploadSettings.uploadVideoWhen = AutoUploadSettings.UploadWhen.WIFI
        autoUploadSettings.photounlimMode = AutoUploadSettings.UnlimMode.ENABLED
        autoUploadSettings.videounlimMode = AutoUploadSettings.UnlimMode.DISABLED
        val request = SetAutouploadModeCommandRequest(
            AutoUploadSettings.UploadWhen.ALWAYS, AutoUploadSettings.UploadWhen.ALWAYS,
            true,
            AutoUploadSettings.UnlimMode.NOT_SET, AutoUploadSettings.UnlimMode.NOT_SET
        )
        command.execute(request)
        assertEquals(autoUploadSettings.uploadPhotoWhen, AutoUploadSettings.UploadWhen.ALWAYS)
        assertEquals(autoUploadSettings.uploadVideoWhen, AutoUploadSettings.UploadWhen.ALWAYS)
        assertEquals(autoUploadSettings.photounlimMode, AutoUploadSettings.UnlimMode.ENABLED)
        assertEquals(autoUploadSettings.videounlimMode, AutoUploadSettings.UnlimMode.DISABLED)
    }
}
