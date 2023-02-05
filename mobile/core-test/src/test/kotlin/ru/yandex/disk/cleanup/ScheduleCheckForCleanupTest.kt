package ru.yandex.disk.cleanup

import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import ru.yandex.disk.cleanup.command.CheckForCleanupCommandRequest
import ru.yandex.disk.cleanup.command.ScheduleCheckForCleanupCommand
import ru.yandex.disk.cleanup.command.ScheduleCheckForCleanupCommandRequest
import ru.yandex.disk.service.CommandScheduler
import ru.yandex.disk.settings.AutoUploadSettings
import ru.yandex.disk.settings.UserSettings
import ru.yandex.disk.test.AndroidTestCase2

class ScheduleCheckForCleanupTest : AndroidTestCase2() {

    private val commandScheduler = mock(CommandScheduler::class.java)
    private val autouploadSettings = mock(AutoUploadSettings::class.java)
    private val userSettings = mock(UserSettings::class.java).apply {
        whenever(autoUploadSettings).thenReturn(autouploadSettings)
    }

    @Test
    fun `should schedule command`() {
        whenever(autouploadSettings.anyAutouploadEnabled()).thenReturn(true)
        val command = ScheduleCheckForCleanupCommand(userSettings, commandScheduler)
        command.execute(ScheduleCheckForCleanupCommandRequest(1234L))
        verify(commandScheduler).scheduleAt(any<CheckForCleanupCommandRequest>(), eq(1234L))
    }

    @Test
    fun `should not schedule command`() {
        whenever(autouploadSettings.anyAutouploadEnabled()).thenReturn(false)
        val command = ScheduleCheckForCleanupCommand(userSettings, commandScheduler)
        command.execute(ScheduleCheckForCleanupCommandRequest(1234L))
        verify(commandScheduler, never()).scheduleAt(any<CheckForCleanupCommandRequest>(), anyLong())
    }

}
