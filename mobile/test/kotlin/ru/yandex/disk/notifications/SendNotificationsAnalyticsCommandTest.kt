package ru.yandex.disk.notifications

import org.mockito.kotlin.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import ru.yandex.disk.stats.AnalyticsAgent
import ru.yandex.disk.stats.EventLog

@RunWith(JUnit4::class)
class SendNotificationsAnalyticsCommandTest {

    private val notificationSettings = mock<NotificationSettings> {
        on { isNotificationEnabled(NotificationType.DEFAULT) } doReturn true
        on { isNotificationEnabled(NotificationType.AUTOUPLOAD) } doReturn true
        on { isNotificationEnabled(NotificationType.PHOTO_SELECTION) } doReturn false
        on { isNotificationEnabled(NotificationType.UNKNOWN_SHORT_MESSAGE) } doReturn false
        on { isNotificationEnabled(NotificationType.CLEANUP) } doReturn false
        on { isNotificationEnabled(NotificationType.AUDIO_PLAYER) } doReturn false
        on { isNotificationEnabled(NotificationType.NEW_AUTOUPLOAD_DIRS) } doReturn true
        on { isNotificationEnabled(NotificationType.UPLOAD) } doReturn true
    }

    private val sendNotificationsAnalyticsCommand =
        SendNotificationsAnalyticsCommand(notificationSettings)

    private val analyticsAgent = mock<AnalyticsAgent>()

    init {
        EventLog.init(true, mock(), analyticsAgent)
    }

    @Test
    fun `should report notifications settings`() {

        sendNotificationsAnalyticsCommand.execute(SendNotificationsAnalyticsCommandRequest())

        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("default", "true"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("autoupload", "true"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("photo_selection", "false"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("unknown", "false"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("cleanup", "false"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("audio_player", "false"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("new_autoupload_dirs", "true"))
        verify(analyticsAgent).reportTechEvent(eq("notification_enabled"), singletonMapMatcher("upload", "true"))

        verifyNoMoreInteractions(analyticsAgent)
    }

    private fun singletonMapMatcher(key: String, value: String) =
        argThat<Map<String, Any>> {
            return@argThat this.size == 1 && this[key] == value
        }
}
