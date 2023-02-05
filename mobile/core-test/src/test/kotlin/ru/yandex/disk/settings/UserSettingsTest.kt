package ru.yandex.disk.settings

import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.mockito.Mockito.mock
import org.robolectric.annotation.Config
import ru.yandex.disk.provider.DiskContentProviderTest
import ru.yandex.disk.remote.SubscriptionId
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.util.SystemClock

@Config(manifest = Config.NONE)
class UserSettingsTest2 : DiskContentProviderTest() {

    private lateinit var userSettings: UserSettings

    private val systemClock = mock(SystemClock::class.java)

    override fun setUp() {
        super.setUp()
        recreateUserSettings()
    }

    private fun recreateUserSettings() {
        val settings = TestObjectsFactory.createApplicationSettings(mockContext, systemClock)
        userSettings = settings.getUserSettings(TestObjectsFactory.createCredentials())!!
    }

    @Test
    fun shouldPersistSubscriptionId() {
        userSettings.subscriptionId = SubscriptionId("id1", "id2", "token")

        recreateUserSettings()

        val subscriptionId = userSettings.subscriptionId
        assertNotNull(subscriptionId)
        assertThat(subscriptionId!!.shortMessagesSubscriptionId, equalTo("id1"))
        assertThat(subscriptionId.dataSyncSubscriptionId, equalTo("id2"))
        assertThat(subscriptionId.webdavToken, equalTo("token"))
    }

    @Test
    fun shouldGetNullSubscriptionId() {
        val subscriptionId = userSettings.subscriptionId
        assertNull(subscriptionId)
    }

    @Test
    fun shouldSetNullSubscriptionId() {
        userSettings.subscriptionId = null
        val subscriptionId = userSettings.subscriptionId
        assertNull(subscriptionId)
    }

    @Test
    fun shouldPostponeCleanupPush() {
        assertThat(userSettings.canShowCleanupPush(), equalTo(true))
        userSettings.postponeCleanupPush()
        assertThat(userSettings.canShowCleanupPush(), equalTo(false))
        whenever(systemClock.currentTimeMillis()).thenReturn(UserSettings.DAY - 1)
        assertThat(userSettings.canShowCleanupPush(), equalTo(false))
        whenever(systemClock.currentTimeMillis()).thenReturn(UserSettings.DAY + 1)
        assertThat(userSettings.canShowCleanupPush(), equalTo(true))
    }
}
