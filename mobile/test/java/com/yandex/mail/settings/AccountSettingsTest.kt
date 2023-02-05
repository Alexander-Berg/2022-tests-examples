package com.yandex.mail.settings

import android.content.SharedPreferences
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.mail.model.GeneralSettingsModel
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(IntegrationTestRunner::class)
class AccountSettingsTest {

    private lateinit var testUser: User

    private lateinit var teamUser: User

    private lateinit var model: GeneralSettingsModel

    @Before
    fun beforeEachTest() {
        model = IntegrationTestRunner.app().getApplicationComponent().settingsModel()
        testUser = User.create(Accounts.testLoginData)
        teamUser = User.create(Accounts.teamLoginData)
    }

    @Test
    fun `null value in prefs is fixed by resetting theme`() {
        val mockedPrefs = mockedPrefsWithPossibleNullValues(AtomicInteger(0))

        val context = IntegrationTestRunner.app()
        val oldSettings = AccountSettings(context, testUser.uid, mockedPrefs)

        assertThat(oldSettings.themePreference().isSet).isFalse()
        assertThat(oldSettings.themePreference().get()).isEqualTo(MailSettings.DEFAULT_THEME)

        mockedPrefs.edit().putString(AccountSettingsConstants.THEME, null) // see https://st.yandex-team.ru/MOBILEMAIL-11387
        assertThat(oldSettings.themePreference().isSet).isTrue()
        assertThatThrownBy { oldSettings.themePreference().get() }
            .isInstanceOf(AssertionError::class.java)
            // means that null value is in prefs
            .hasStackTraceContaining("at com.f2prateek.rx.preferences2.StringAdapter.get(StringAdapter.java:11)")

        oldSettings.edit().setTheme(null).apply()

        assertThat(oldSettings.themePreference().isSet).isTrue()
        assertThat(oldSettings.themePreference().get()).isEqualTo(MailSettings.DEFAULT_THEME)
    }

    private fun mockedPrefsWithPossibleNullValues(currentVersion: AtomicInteger): SharedPreferences {
        val valueInPref = AtomicReference<String>(null)
        val valuesIsSet = AtomicBoolean(false)

        val editor = mock<SharedPreferences.Editor>()
        whenever(editor.putString(eq(AccountSettingsConstants.THEME), any()))
            .thenAnswer { invocation ->
                valueInPref.set(invocation.getArgument(1))
                valuesIsSet.set(true)
                editor
            }

        return mock {
            on { getInt(eq("prefsVersion"), eq(0)) }
                .then { currentVersion.get() }
            on { contains(eq(AccountSettingsConstants.THEME)) }
                .then { valuesIsSet.get() }
            on { getString(eq(AccountSettingsConstants.THEME), any()) }
                .thenAnswer { invocation -> valueInPref.get() ?: invocation.getArgument<String>(1) }
            on { edit() }
                .thenReturn(editor)
        }
    }
}
