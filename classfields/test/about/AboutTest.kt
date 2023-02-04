package ru.auto.ara.test.about

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.AboutActivity
import ru.auto.ara.R
import ru.auto.ara.core.robot.othertab.aboutapplication.performAbout
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class AboutTest {

    private val composeRule = createAndroidComposeRule<AboutActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule(),
        composeRule
    )

    @Test
    fun shouldOpenLicenseAgreement() {
        composeRule.performAbout { clickOnLicenseAgreement() }
        checkWebView { isWebViewToolBarDisplayed(R.string.about_license_agreement) }
    }
}
