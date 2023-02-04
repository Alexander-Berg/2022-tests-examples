package com.yandex.mobile.realty.test.yandexrent.showings

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ShowingsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentShowingsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_ACCENT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_IMPORTANT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_INFO
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_WARNING
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.showing
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.showingWidget
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 29.06.2022
 */
@LargeTest
class ShowingsNotificationsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ShowingsActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowInfoNotification() {
        shouldShowNotification(SHOWING_TYPE_INFO)
    }

    @Test
    fun shouldShowAccentNotification() {
        shouldShowNotification(SHOWING_TYPE_ACCENT)
    }

    @Test
    fun shouldShowImportantNotification() {
        shouldShowNotification(SHOWING_TYPE_IMPORTANT)
    }

    @Test
    fun shouldShowWarningNotification() {
        shouldShowNotification(SHOWING_TYPE_WARNING)
    }

    private fun shouldShowNotification(type: String) {
        configureWebServer {
            registerShowings(type)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            notificationItem(FIRST_WIDGET_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))

            notificationItem(SECOND_WIDGET_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notificationWithAction"))
        }
    }

    private fun DispatcherRegistry.registerShowings(type: String) {
        registerShowings(
            showings = listOf(
                showing(
                    showingId = FIRST_SHOWING,
                    widget = showingWidget(
                        html = FIRST_WIDGET_TEXT,
                        type = type
                    )
                ),
                showing(
                    showingId = SECOND_SHOWING,
                    widget = showingWidget(
                        html = SECOND_WIDGET_TEXT,
                        type = type,
                        action = jsonObject {
                            "fallbackUrl" to jsonObject { "url" to "test" }
                            "buttonText" to WIDGET_ACTION_TEXT
                        }
                    )
                ),
            )
        )
    }

    private companion object {

        const val FIRST_SHOWING = "showingId0001"
        const val SECOND_SHOWING = "showingId0002"
        const val FIRST_WIDGET_TEXT = "Это тестовый текст нотификации"
        const val SECOND_WIDGET_TEXT = "Это тестовый текст нотификации с действием"
        const val WIDGET_ACTION_TEXT = "Открыть"
    }
}
