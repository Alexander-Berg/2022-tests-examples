package ru.yandex.disk.cleanup

import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import ru.yandex.disk.cleanup.command.CheckForCleanupCommand
import ru.yandex.disk.cleanup.command.CheckForCleanupCommandRequest
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSender
import ru.yandex.disk.notifications.NotificationSettings
import ru.yandex.disk.notifications.NotificationType
import ru.yandex.disk.service.CommandScheduler
import ru.yandex.disk.settings.UserSettings

private const val DEFAULT_CLEANUP_PERIOD: Long = 1
private const val POSTPONED_CLEANUP_PERIOD: Long = 2

class CheckForCleanupCommandTest {

    private val eventSender = mock(EventSender::class.java)
    private val commandScheduler = mock(CommandScheduler::class.java)
    private val prefs = mock(NotificationSettings::class.java).apply {
        whenever(isNotificationEnabled(any<NotificationType>())).thenReturn(true)
    }
    private val userSettings = mock(UserSettings::class.java).apply {
        whenever(canShowCleanupPush()).thenReturn(true)
    }

    private val cleanupPolicy = mock(CleanupPolicy::class.java).apply {
        whenever(nextDefaultCleanupDate).thenReturn(DEFAULT_CLEANUP_PERIOD)
        whenever(nextPostponedCleanupDate).thenReturn(POSTPONED_CLEANUP_PERIOD)
    }
    private val cleanupSizeCalculator = mock(CleanupSizeCalculator::class.java).apply {
        whenever(calculate(anyLong())).thenReturn(CleanupSize(oldUploadedFilesSize = CleanupPolicy.MIN_SIZE_FOR_CLEANUP + 1))
    }

    private val command: CheckForCleanupCommand = CheckForCleanupCommand(eventSender, commandScheduler, userSettings,
        cleanupPolicy, prefs, cleanupSizeCalculator)

    @Test
    fun `should suggest cleanup`() {
        command.execute(CheckForCleanupCommandRequest())
        verify(eventSender).send(any<DiskEvents.CleanupPromoPushEvent>())
        verify(commandScheduler).scheduleAt(any<CheckForCleanupCommandRequest>(), eq(DEFAULT_CLEANUP_PERIOD))
    }

    @Test
    fun `should not suggest cleanup`() {
        whenever(cleanupSizeCalculator.calculate(anyLong())).thenReturn(CleanupSize())
        command.execute(CheckForCleanupCommandRequest())
        verify(eventSender, never()).send(any<DiskEvents.CleanupPromoPushEvent>())
        verify(commandScheduler).scheduleAt(any<CheckForCleanupCommandRequest>(), eq(DEFAULT_CLEANUP_PERIOD))
    }

    @Test
    fun `should mute notification if disabled`() {
        whenever(prefs.isNotificationEnabled(any<NotificationType>())).thenReturn(false)
        command.execute(CheckForCleanupCommandRequest())
        verify(eventSender, never()).send(any<DiskEvents.CleanupPromoPushEvent>())
        verify(commandScheduler).scheduleAt(any<CheckForCleanupCommandRequest>(), eq(DEFAULT_CLEANUP_PERIOD))
    }

    @Test
    fun `should mute notification if postponed`() {
        whenever(userSettings.canShowCleanupPush()).thenReturn(false)
        command.execute(CheckForCleanupCommandRequest())
        verify(eventSender, never()).send(any<DiskEvents.CleanupPromoPushEvent>())
        verify(commandScheduler).scheduleAt(any<CheckForCleanupCommandRequest>(), eq(POSTPONED_CLEANUP_PERIOD))
    }

}
