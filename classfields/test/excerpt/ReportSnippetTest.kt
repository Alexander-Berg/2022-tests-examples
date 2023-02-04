package com.yandex.mobile.realty.test.excerpt

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ReportsActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnReportsScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 09.12.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ReportSnippetTest {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ReportsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun shouldShowReportDoneWithOffer() {
        shouldShowReportSnippet(
            "reportSnippetTest/reportDoneWithOffer.json",
            "ReportSnippetTest/reportDoneWithOffer"
        )
    }

    @Test
    fun shouldShowReportDoneWithoutOffer() {
        shouldShowReportSnippet(
            "reportSnippetTest/reportDoneWithoutOffer.json",
            "ReportSnippetTest/reportDoneWithoutOffer"
        )
    }

    @Test
    fun shouldShowReportErrorWithOffer() {
        shouldShowReportSnippet(
            "reportSnippetTest/reportErrorWithOffer.json",
            "ReportSnippetTest/reportErrorWithOffer"
        )
    }

    @Test
    fun shouldShowReportErrorWithoutOffer() {
        shouldShowReportSnippet(
            "reportSnippetTest/reportErrorWithoutOffer.json",
            "ReportSnippetTest/reportErrorWithoutOffer"
        )
    }

    @Test
    fun shouldShowReportInProgressWithOffer() {
        shouldShowReportSnippet(
            "reportSnippetTest/reportInProgressWithOffer.json",
            "ReportSnippetTest/reportInProgressWithOffer"
        )
    }

    @Test
    fun shouldShowReportInProgressWithoutOffer() {
        shouldShowReportSnippet(
            "reportSnippetTest/reportInProgressWithoutOffer.json",
            "ReportSnippetTest/reportInProgressWithoutOffer"
        )
    }

    private fun shouldShowReportSnippet(
        responseFile: String,
        screenshotKey: String
    ) {
        configureWebServer {
            registerReports(responseFile)
        }

        activityTestRule.launchActivity()

        performOnReportsScreen {
            waitUntil { containsReportSnippet("1") }

            performOnReportSnippet("1") {
                isViewStateMatches(screenshotKey)
            }
        }
    }

    private fun DispatcherRegistry.registerReports(responseFile: String) {
        register(
            request {
                path("2.0/paid-report/user/me")
                queryParam("paymentStatus", "paid")
                queryParam("reportStatus", "in_progress")
                queryParam("reportStatus", "done")
                queryParam("reportStatus", "error")
            },
            response {
                assetBody(responseFile)
            }
        )
    }
}
