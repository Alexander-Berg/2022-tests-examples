package ru.yandex.direct.ui.testutils

import android.accounts.AccountManager
import androidx.test.InstrumentationRegistry
import org.junit.rules.ExternalResource
import ru.yandex.direct.BuildConfig
import ru.yandex.direct.Configuration

class ClearAppDataBeforeEachTestRule : ExternalResource() {
    override fun before() {
        logout()
        removeAccounts()
    }

    override fun after() {
        logout()
        removeAccounts()
    }

    private fun logout() {
        val configuration = Configuration.get()
        configuration.logOut()
        configuration.lastError = null
        configuration.isFirstTimeLaunch
        configuration.lastOnboardingVersionCode = BuildConfig.VERSION_CODE
    }

    private fun removeAccounts() {
        val targetContext = InstrumentationRegistry.getTargetContext()
        val accountManager = AccountManager.get(targetContext)
        for (account in accountManager.accounts) {
            accountManager.removeAccount(account, null, null)
        }
    }
}
