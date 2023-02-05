package ru.yandex.disk.notifications

import android.content.SharedPreferences
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2
import java.util.*

class NotificationSettingsLegacyTest : AndroidTestCase2() {

    private val randomPreferences = HashMap<String, Boolean>()
    private val prefs = mock<SharedPreferences>()
    private val legacyNotificationSettings = NotificationSettingsLegacy(NotificationSettingsStorageLegacy(prefs))

    override fun setUp() {
        super.setUp()
        val random = Random()
        for (nt in NotificationType.values()) {
            randomPreferences.put(nt.preferenceKey, random.nextBoolean())
            val prefKey = nt.preferenceKey
            whenever(prefs.getBoolean(eq(prefKey), any()))
                    .thenReturn(randomPreferences[prefKey])
        }
    }

    @Test
    fun `should delegate settings extraction to prefs`() {
        for (nt in NotificationType.values()) {
            assertThat(legacyNotificationSettings.isNotificationEnabled(nt),
                    equalTo(randomPreferences[nt.preferenceKey]))
        }
    }
}
