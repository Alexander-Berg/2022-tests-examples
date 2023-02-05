package ru.yandex.disk.upload

import android.database.Cursor
import com.yandex.pulse.histogram.StubHistogram
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.annotation.Config
import ru.yandex.disk.autoupload.AutouploadQueueHelper
import ru.yandex.disk.autoupload.cursor.MediaItemCursor
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.gallery.data.provider.MediaStoreProvider
import ru.yandex.disk.provider.BucketAlbumsProvider
import ru.yandex.disk.provider.DiskUploadQueueCursor
import ru.yandex.disk.service.CommandScheduler
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.stats.AnalyticsAgent
import ru.yandex.disk.stats.EventLog
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.toggle.SeparatedAutouploadToggle

@Config(manifest = Config.NONE)
class QueueAutouploadsCommandTest : AndroidTestCase2() {

    private val request = QueueAutouploadsCommandRequest()
    private val diskUploader = mock<DiskUploader>()
    private val uploadQueue = mock<UploadQueue>()
    private val photoUploadSettings = mock<AutoUploadSettings> {
        on { anyAutouploadEnabled() } doReturn true
        on { isPhotoAutouploadEnabled() } doReturn true
        on { isVideoAutouploadEnabled() } doReturn true
    }
    private val userSettings = mock<UserSettings> {
        on { autoUploadSettings } doReturn photoUploadSettings
    }
    private val mediaStoreProvider = mock<MediaStoreProvider>()
    private val commandScheduler = mock<CommandScheduler>()
    private val eventSender = mock<EventSender>()
    private val albumsProvider = mock<BucketAlbumsProvider>()
    private val commandStarter = mock<CommandStarter>()

    private lateinit var command: QueueAutouploadsCommand

    public override fun setUp() {
        super.setUp()
        val analyticsAgent = mock<AnalyticsAgent>()
        EventLog.init(true, mock(), analyticsAgent)
        whenever((analyticsAgent.getHistogram(any()))).thenReturn(StubHistogram)

        val emptyCursor = mock<Cursor>()
        whenever(emptyCursor.count).thenReturn(0)
        whenever(emptyCursor.position).thenReturn(0)
        whenever(emptyCursor.moveToPosition(anyInt())).thenReturn(false)
        whenever(emptyCursor.getColumnIndex(anyString())).thenReturn(0)
        val diskUploadQueueCursor = DiskUploadQueueCursor(emptyCursor)
        whenever(uploadQueue.queryReuploadsAndAutouploads(anyString())).thenReturn(diskUploadQueueCursor)
        val mediaItemCursor = MediaItemCursor(emptyCursor)
        whenever(mediaStoreProvider.getImagesForAlbum(any())).thenReturn(mediaItemCursor)

        val mediaStoreCollector = MediaStoreCollector(mediaStoreProvider, commandScheduler)

        val autouploadHelper = AutouploadQueueHelper(uploadQueue, mock(), mock(), mock(), mock(), mock(), mock())

        command = QueueAutouploadsCommand(userSettings, albumsProvider,
            mediaStoreCollector, autouploadHelper, diskUploader, eventSender,
            commandStarter, SeparatedAutouploadToggle(false))
    }

    @Test
    fun `should start upload if settings set manually`() {
        whenever(photoUploadSettings.isSettingsWereSetManually).thenReturn(true)
        whenever(userSettings.unlimOnboardingShown).thenReturn(false)

        command.execute(request)

        verifyUploadStarts()
    }

    @Test
    fun `should start upload if promo shown`() {
        whenever(photoUploadSettings.isSettingsWereSetManually).thenReturn(false)
        whenever(userSettings.unlimOnboardingShown).thenReturn(true)

        command.execute(request)

        verifyUploadStarts()
    }


    @Test
    fun `should not upload before promo shown`() {
        whenever(photoUploadSettings.isSettingsWereSetManually).thenReturn(false)
        whenever(userSettings.unlimOnboardingShown).thenReturn(false)

        command.execute(request)

        verifyUploadNotStarts()
    }

    private fun verifyUploadStarts() {
        verify(diskUploader).startUpload()
    }

    private fun verifyUploadNotStarts() {
        verify(diskUploader, never()).startUpload()
    }
}
