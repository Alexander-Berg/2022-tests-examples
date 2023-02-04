package com.yandex.mobile.realty.test.yandexrent.showingcard

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ShowingCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesShareIntent
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.DateRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.DatePickerScreen
import com.yandex.mobile.realty.core.screen.RentShowingCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.SHOWING_ID
import com.yandex.mobile.realty.test.yandexrent.showings.Showings
import com.yandex.mobile.realty.test.yandexrent.showings.registerCheckInDate
import com.yandex.mobile.realty.test.yandexrent.showings.registerCheckInDateError
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class ShowingCardActionsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ShowingCardActivityTestRule(showingId = SHOWING_ID, launchActivity = false)
    private val dateRule = DateRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun selectCheckInDate() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetSaveCheckInDate()
                )
            )
            registerCheckInDateError()
            registerCheckInDate()
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetWithoutAction()
                )
            )
        }

        dateRule.setDate(2022, 6, 1)
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        val openPickerEvent = event("Аренда. Переход к выбору даты заселения") {
            "Источник" to "Карточка показа"
        }
        val errorEvent = event("Аренда. Выбор даты заселения") {
            "Результат" to "Ошибка"
        }
        val successEvent = event("Аренда. Выбор даты заселения") {
            "Результат" to "Успех"
        }

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            importantActionButton.click()
        }

        onScreen<DatePickerScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            waitUntil { openPickerEvent.isOccurred() }
            root.isViewStateMatches(getTestRelatedFilePath("picker"))
            cancelButton.click()
        }

        onScreen<RentShowingCardScreen> {
            importantActionButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<DatePickerScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            confirmButton.click()
        }

        onScreen<RentShowingCardScreen> {
            waitUntil { toastView(Showings.CHECK_IN_DATE_ERROR).isCompletelyDisplayed() }
            waitUntil { errorEvent.isOccurred() }
            importantActionButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        onScreen<DatePickerScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            confirmButton.click()
        }

        onScreen<RentShowingCardScreen> {
            waitUntil {
                listView.contains(headerItem)
                headerItem.view
                    .invoke { notificationView.isTextEquals(Showing.WIDGET_SIMPLE_TEXT) }
            }
            waitUntil { successEvent.isOccurred() }
        }
    }

    @Test
    fun showRoommatesInvitation() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetShareLinkToQuestionnaire()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesShareIntent(Showings.SHARED_LINK), null)

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            accentActionButton.click()
        }

        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(Showings.ROOMMATES_INVITATION_URL) }
            event("Аренда. Переход к отправке ссылки сожителям") {
                "Источник" to "Карточка показа"
            }.waitUntil { isOccurred() }

            webView.invoke { evaluateJavascript(Showings.SHARE_LINK_SCRIPT) }

            waitUntil { intended(matchesShareIntent(Showings.SHARED_LINK)) }
        }
    }

    @Test
    fun showTenantRentConditions() {
        configureWebServer {
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetExploreConditionsOfHousingServices()
                )
            )
            registerShowingDetails(
                body = Showing.simpleBody(
                    widget = Showing.widgetWithoutAction()
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingCardScreen> {
            waitUntil { listView.contains(headerItem) }
            warningActionButton.click()
        }

        val openWebViewEvent = event("Аренда. Переход к принятию условий по ЖКХ") {
            "Источник" to "Карточка показа"
        }

        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(Showings.UTILITIES_CONDITIONS_URL) }
            waitUntil { openWebViewEvent.isOccurred() }
            webView.invoke { evaluateJavascript(Showings.SUBMIT_FORM_SCRIPT) }
        }

        onScreen<RentShowingCardScreen> {
            waitUntil {
                listView.contains(headerItem)
                headerItem.view
                    .invoke { notificationView.isTextEquals(Showing.WIDGET_SIMPLE_TEXT) }
            }
        }
    }
}
