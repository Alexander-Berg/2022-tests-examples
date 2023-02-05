package ru.yandex.disk

import android.accounts.AccountManager
import android.content.ContentValues
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.yandex.disk.multilogin.CommandStarterProvider
import ru.yandex.disk.service.CommandLogger
import ru.yandex.disk.service.CommandRequest
import ru.yandex.disk.test.SeclusiveContext
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.test.instanceOf
import ru.yandex.disk.util.SystemClock
import ru.yandex.disk.utils.set
import javax.inject.Provider
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CredentialsManagerTest {
    private val context = SeclusiveContext(RuntimeEnvironment.application).apply {
        setAccountManager(Mocks.mockAccountManager())
        Mocks.addContentProviders(this)
    }
    private val contentProviderClient = TestObjectsFactory.createSelfContentProviderClient(context)
    private val commandLogger = CommandLogger()
    private val mockAccountManager = mock<CredentialsManager.SystemAccountManagerMediator> {
        on { getToken(anyLong()) } doReturn "token"
    }

    private val cm = createCredentialsManager()

    private fun createCredentialsManager(): CredentialsManager {
        return CredentialsManager(
            mockAccountManager,
            Provider { CommandStarterProvider { commandLogger } },
            CredentialsDatabase(contentProviderClient, SystemClock.REAL),
            ApplicationUpgradeWatcher.STUB,
            mock(AccountManager::class.java),
            mock(),
            TestObjectsFactory.createStrictModeManager(),)
    }

    @Test
    fun `should set active account on login`() {
        cm.login("test", 0L)

        assertThat(cm.hasActiveAccount(), equalTo(true))
    }

    @Test
    fun `should get active account creds after login`() {
        cm.login("name", 0L)

        val creds = cm.activeAccountCredentials!!
        assertThat(creds.user, equalTo("name"))
    }

    @Test
    fun `should not get active account if no active account`() {
        val creds = cm.activeAccountCredentials
        assertThat(creds, nullValue())
    }

    @Test
    fun `should request AM once`() {
        cm.login("name", 0L)

        val creds = cm.activeAccountCredentials!!
        cm.updateToken(creds)
        cm.updateToken(creds)

        verify(mockAccountManager, times(1)).getToken(anyLong())
    }

    @Test
    fun `should reset active account on logout`() {
        cm.login("name", 0L)

        assertThat(commandLogger.get(0), instanceOf<CommandRequest, LoginCommandRequest>())

        cm.logout(CredentialsManager.LogoutCause.UNPROCESSED)

        assertThat(commandLogger.get(1), instanceOf<CommandRequest, LogoutCommandRequest>())
        assertThat(cm.activeAccountCredentials!!.user, equalTo("name"))
    }

    @Test
    fun `should normally process logout if no account`() {
        cm.logout(CredentialsManager.LogoutCause.UNPROCESSED)
    }

    @Test
    fun `should invalidate master token on UNAUTHORIZED cause`() {
        val uid = 123L
        cm.login("name", uid)

        cm.logout(CredentialsManager.LogoutCause.UNAUTHORIZED)

        verify(mockAccountManager).invalidateMasterToken(uid)
    }

    @Test
    fun `should not invalidate master token if null uid`() {
        val cv = ContentValues()
        cv["USER"] = "name"
        cv["UID"] = null
        cv["IS_LOGGED"] = 0
        contentProviderClient.insert("/creds", cv)

        cm.logout(CredentialsManager.LogoutCause.UNAUTHORIZED)

        verify(mockAccountManager, never()).invalidateMasterToken(anyLong())
    }

    @Test
    fun `should single account be photo after login`() {
        cm.login("name", 1L)
        val usedInAppCredentials = cm.usedInAppCredentials
        assertThat(usedInAppCredentials, hasSize(1))
        assertThat(usedInAppCredentials[0].user, equalTo("name"))
        assertThat(usedInAppCredentials[0].uid, equalTo(1L))
        assertTrue { cm.isPhotoAccount(usedInAppCredentials[0]) }
    }

    @Test
    fun `should second account not be photo`() {
        cm.login("name", 1L)
        cm.login("another_name", 2L)
        val usedInAppCredentials = cm.usedInAppCredentials
        assertThat(usedInAppCredentials, hasSize(2))
        assertThat(usedInAppCredentials[1].user, equalTo("another_name"))
        assertFalse { cm.isPhotoAccount(usedInAppCredentials[1]) }
    }

    @Test
    fun `should photo account changed`() {
        cm.login("name", 1L)
        cm.login("another_name", 2L)
        cm.markAsPhotoAccount(Credentials("another_name", 2L))
        val usedInAppCredentials = cm.usedInAppCredentials
        assertThat(usedInAppCredentials, hasSize(2))
        assertFalse { cm.isPhotoAccount(usedInAppCredentials[0]) }
        assertTrue { cm.isPhotoAccount(usedInAppCredentials[1]) }
    }

    @Test
    fun `should remove account when logout`() {
        cm.login("name", 1L)
        cm.resetActiveAccountCredentials()
        val usedInAppCredentials = cm.usedInAppCredentials
        assertThat(usedInAppCredentials, hasSize(0))
    }

    @Test
    fun `should remove account when logout second`() {
        cm.login("name", 1L)
        cm.login("another_name", 2L)
        cm.resetActiveAccountCredentials()
        val usedInAppCredentials = cm.usedInAppCredentials
        assertThat(usedInAppCredentials, hasSize(1))
        assertThat(usedInAppCredentials[0].user, equalTo("name"))
    }
}
