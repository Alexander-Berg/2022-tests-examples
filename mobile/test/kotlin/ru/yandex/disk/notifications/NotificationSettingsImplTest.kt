package ru.yandex.disk.notifications

import android.app.NotificationChannel
import androidx.core.app.NotificationManagerCompat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.junit.Test
import org.hamcrest.Matchers.equalTo
import org.junit.Ignore
import ru.yandex.disk.Credentials
import ru.yandex.disk.test.AndroidTestCase2
import java.util.*

class NotificationSettingsImplTest : AndroidTestCase2() {

    val randomPreferences = HashMap<String, Boolean>()
    val credentials = mock<Credentials>() {
        on {this.user} doReturn "test";
    }
    var notificationManager = mock<NotificationManagerCompat> {
        fun getNotificationChannel(channelId: String) = NotificationChannel(
                channelId, channelId, if (randomPreferences[channelId]!!) 3 else 0)
    }

    var oreoNotificationSettings = NotificationSettingsImpl(notificationManager, credentials)

    override fun setUp() {
        super.setUp()
        val r = Random()
        for (nt in NotificationType.values()) {
            randomPreferences.put(nt.getChannelId(credentials.uid), r.nextBoolean())
        }
    }

    @Test
    @Ignore
    fun `should take properties from notification manager`() {
        for (nt in NotificationType.values()) {
            assertThat(oreoNotificationSettings!!.isNotificationEnabled(nt),
                    equalTo(nt.getChannelId(credentials.uid).hashCode() % 2 == 0))
        }
    }
}
