package ru.auto.ara.test.support_report

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.support_report.PostSupportReportDispatcher
import ru.auto.ara.core.robot.support_report.checkSupport
import ru.auto.ara.core.robot.support_report.performSupport
import ru.auto.ara.core.robot.transporttab.performTransport
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getRandomEmail
import ru.auto.ara.core.utils.getRandomString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class SupportReportTest {

    private val watcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSupportReportDispatcher(watcher)
    )

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()


    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityTestRule
    )

    @Test
    fun showSupportReportWhenCameraDeeplinkCalledWithoutAuth() {
        activityTestRule.launchDeepLinkActivity(SUPPORT_CAMERA_DEEPLINK)

        checkSupport {
            isSupportReport()
        }
    }

    @Test
    fun showSupportReportWhenSupportDeeplinkCalledWithoutAuth() {
        activityTestRule.launchDeepLinkActivity(SUPPORT_CAMERA_DEEPLINK)

        checkSupport {
            isSupportReport()
        }
    }

    @Test
    fun showDisabledSendBtnIfEmptyFields() {
        activityTestRule.launchDeepLinkActivity(SUPPORT_DEEPLINK)
        checkSupport {
            checkSendButtonIsDisabled()
        }

        performSupport {
            clickOnSendBtn()
        }.checkResult {
            watcher.checkRequestWasNotCalled()
        }

    }

    @Test
    fun showNotValidEmail() {
        activityTestRule.launchDeepLinkActivity(SUPPORT_DEEPLINK)
        performSupport {
            inputMessage(getRandomString())
            inputEmail("test")
            clickOnSendBtn()
        }.checkResult {
            isEmailErrorDisplayed(R.string.support_bottom_sheet_email_error)
        }
    }

    @Test
    fun sendSupportReportSuccess() {
        activityTestRule.launchDeepLinkActivity(SUPPORT_DEEPLINK)
        val emailValue = getRandomEmail()
        val messageValue = getRandomString()
        performSupport {
            inputMessage(messageValue)
            inputEmail(emailValue)
        }.checkResult {
            checkSendButtonIsEnabled()
        }

        performSupport {
            clickOnSendBtn()
        }.checkResult {
            watcher.checkRequestBodyParameters(
                "email" to emailValue,
                "message" to messageValue
            )
            checkDialogIsDismissed()
        }

        performTransport {}.checkResult {
            checkMainSceneIsCompletelyDisplayed()
        }
    }

    companion object {
        private const val SUPPORT_DEEPLINK = "autoru://app/techsupport"
        private const val SUPPORT_CAMERA_DEEPLINK = "$SUPPORT_DEEPLINK/camera"
    }
}
