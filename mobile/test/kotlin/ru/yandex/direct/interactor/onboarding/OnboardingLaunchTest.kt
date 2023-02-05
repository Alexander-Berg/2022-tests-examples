package ru.yandex.direct.interactor.onboarding

import android.app.Application
import android.os.Build
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.direct.BuildConfig
import ru.yandex.direct.Configuration
import ru.yandex.direct.newui.onboarding.OnboardingActivity
import ru.yandex.direct.newui.onboarding.OnboardingFragment

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [Build.VERSION_CODES.P])
class OnboardingLaunchTest {

    private lateinit var configuration: Configuration

    private lateinit var onboardingInteractor: OnboardingInteractor

    @Before
    fun runBeforeAnyTest() {
        configuration = mock()
        onboardingInteractor = OnboardingInteractor(configuration, RuntimeEnvironment.systemContext)
    }

    @Test
    fun interactor_shouldTriggerAppTutorial_onFirstStartup() {
        configuration.stub {
            on { configuration.isFirstTimeLaunch } doReturn true
            on { lastOnboardingVersionCode } doReturn 0
        }

        val nullableIntent = onboardingInteractor.onboardingIntentOnAppStartup
        assertThat(nullableIntent).isNotNull()

        val defaultValue = Int.MIN_VALUE
        val extra = nullableIntent!!.getIntExtra(OnboardingActivity.INTENT_EXTRA_APPEARANCE, defaultValue)
        assertThat(extra).isNotEqualTo(defaultValue)
        assertThat(extra).isEqualTo(OnboardingFragment.Appearance.APP_TUTORIAL.ordinal)
    }

    @Test
    fun interactor_shouldTriggerFeaturesHighlight_onUpdate() {
        configuration.stub {
            on { configuration.isFirstTimeLaunch } doReturn false
            on { lastOnboardingVersionCode } doReturn 0
        }
        val nullableIntent = onboardingInteractor.onboardingIntentOnAppStartup
        assertThat(nullableIntent).isNotNull()

        val defaultValue = Int.MIN_VALUE
        val extra = nullableIntent!!.getIntExtra(OnboardingActivity.INTENT_EXTRA_APPEARANCE, defaultValue)
        assertThat(extra).isNotEqualTo(defaultValue)
        assertThat(extra).isEqualTo(OnboardingFragment.Appearance.FEATURES_HIGHLIGHT.ordinal)
    }

    @Test
    fun interactor_shouldDoNothing_ifAppVersionNotChanged() {
        configuration.stub {
            on { configuration.isFirstTimeLaunch } doReturn false
            on { lastOnboardingVersionCode } doReturn BuildConfig.VERSION_CODE
        }
        val nullableIntent = onboardingInteractor.onboardingIntentOnAppStartup
        assertThat(nullableIntent).isNull()
    }
}