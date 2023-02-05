package ru.yandex.disk.analytics

import org.mockito.kotlin.*
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.auth.AccountManager
import ru.yandex.auth.YandexAccount
import ru.yandex.disk.Credentials
import ru.yandex.disk.CredentialsManager
import ru.yandex.disk.stats.AnalyticsAgent
import ru.yandex.disk.stats.TechEventKeys
import ru.yandex.disk.stats.EventLog
import ru.yandex.disk.test.AndroidTestCase2
import rx.Single
import java.util.*

private const val USER = "User"

@Config(manifest = Config.NONE)
class AccountsTechReporterTest : AndroidTestCase2() {
    private val credentials = Credentials(USER, null)
    private val account = mock<YandexAccount> {
        on { uid } doReturn 0L
    }

    private val accountManager = mock<AccountManager> {
        on { accountList } doReturn Single.just(Collections.singletonList(account))
    }
    private val credentialsManager = mock<CredentialsManager> {
        on { activeAccountCredentials } doReturn credentials
    }
    private val analyticsAgent = mock<AnalyticsAgent>()

    private val sender = AccountsTechReporter(accountManager, credentialsManager)

    init {
        EventLog.init(true, mock(), analyticsAgent)
    }

    @Test
    fun `should send account data`() {
        sender.report()

        verify(analyticsAgent).reportTechEvent(eq(TechEventKeys.ACCOUNTS), argThat<Map<String, Any>> {
            val count = this[TechEventKeys.COUNT_ATTR] as String
            val emptyUid = this[TechEventKeys.EMPTY_UID_ATTR] as Boolean
            val activeInApp = this[TechEventKeys.ACTIVE_IN_APP_ATTR] as Boolean
            return@argThat count == "1" && emptyUid && activeInApp
        })
    }

    @Test
    fun `should send account without uid event`() {
        sender.report()

        verify(analyticsAgent).reportTechEvent(eq(TechEventKeys.ACCOUNT_WITHOUT_UID), argThat<Map<String, Any>> {
            val name = this[TechEventKeys.ACCOUNT_NAME_ATTR] as String
            return@argThat name == USER
        })
    }
}
