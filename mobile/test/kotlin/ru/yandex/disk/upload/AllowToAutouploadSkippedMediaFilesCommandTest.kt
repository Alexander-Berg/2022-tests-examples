package ru.yandex.disk.upload

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.UserSettings

class AllowToAutouploadSkippedMediaFilesCommandTest {
    private val commandStarter = mock<CommandStarter>()
    private val autoUploadSettings = mock<AutoUploadSettings>()
    private val userSettings = mock<UserSettings>()
    private val uploadQueue = mock<UploadQueue>()
    private val command = AllowToAutouploadSkippedMediaFilesCommand(commandStarter, autoUploadSettings, userSettings, uploadQueue)

    @Test
    fun `should remove all skipped autouploads`() {
        command.execute(AllowToAutouploadSkippedMediaFilesCommandRequest())

        verify(uploadQueue).removeAllSkippedAutouploads()
        verify(userSettings).skipOldPhotosForAutoupload = false
    }

    @Test
    fun `should queue autouploads if any autoupload enabled`() {
        whenever(autoUploadSettings.anyAutouploadEnabled()).thenReturn(true)

        command.execute(AllowToAutouploadSkippedMediaFilesCommandRequest())

        verify(commandStarter).start(any<QueueAutouploadsCommandRequest>())
    }

    @Test
    fun `should not queue autouploads if no autoupload enabled`() {
        whenever(autoUploadSettings.anyAutouploadEnabled()).thenReturn(false)

        command.execute(AllowToAutouploadSkippedMediaFilesCommandRequest())

        verify(commandStarter, never()).start(any<QueueAutouploadsCommandRequest>())
    }
}
