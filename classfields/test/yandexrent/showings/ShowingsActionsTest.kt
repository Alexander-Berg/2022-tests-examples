package com.yandex.mobile.realty.test.yandexrent.showings

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ShowingsActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.matchesShareIntent
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.metrica.event
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.DateRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.DatePickerScreen
import com.yandex.mobile.realty.core.screen.RentShowingCardScreen
import com.yandex.mobile.realty.core.screen.RentShowingsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.SHOWING_ID
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.CHECK_IN_DATE_ERROR
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.ROOMMATES_INVITATION_URL
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.ROOMMATES_LIST_URL
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHARED_LINK
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHARE_LINK_SCRIPT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SHOWING_TYPE_ACCENT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.SUBMIT_FORM_SCRIPT
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.UTILITIES_CONDITIONS_URL
import com.yandex.mobile.realty.test.yandexrent.showings.Showings.showingWidget
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 29.06.2022
 */
@LargeTest
class ShowingsActionsTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ShowingsActivityTestRule(launchActivity = false)
    private val dateRule = DateRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        dateRule,
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowShowingCard() {
        configureWebServer {
            registerShowing(widget = showingWidget(WIDGET_TEXT))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            showingHeaderItem(SHOWING_ID)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<RentShowingCardScreen> {
            listView.waitUntil { isCompletelyDisplayed() }
            event("????????????. ?????????????? ???? ???????????????? ????????????")
                .waitUntil { isOccurred() }
        }
    }

    @Test
    fun shouldShowRoommatesList() {
        configureWebServer {
            registerShowing(
                roommates = listOf(ROOMMATE_NAME),
                widget = showingWidget(WIDGET_TEXT)
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            showingHeaderItem(SHOWING_ID)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("showing"))
                .invoke { roommatesView.tapOnLinkText(ROOMMATE_NAME) }
        }
        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(ROOMMATES_LIST_URL) }
            event("????????????. ?????????????? ?? ?????????????????? ???????????? ??????????????????") {
                "????????????????" to "???????????? ??????????????"
            }.waitUntil { isOccurred() }
        }
    }

    @Test
    fun shouldSelectCheckInDate() {
        configureWebServer {
            registerShowing(
                widget = showingWidget(
                    html = CHECK_IN_DATE_TEXT,
                    type = SHOWING_TYPE_ACCENT,
                    action = jsonObject {
                        "buttonText" to CHECK_IN_DATE_ACTION
                        "actionSaveCheckInDate" to jsonObject {}
                    }
                )
            )
            registerCheckInDateError()
            registerCheckInDate()
            registerShowing(widget = showingWidget(WIDGET_TEXT))
        }

        val openPickerEvent = event("????????????. ?????????????? ?? ???????????? ???????? ??????????????????") {
            "????????????????" to "???????????? ??????????????"
        }
        val errorEvent = event("????????????. ?????????? ???????? ??????????????????") {
            "??????????????????" to "????????????"
        }
        val successEvent = event("????????????. ?????????? ???????? ??????????????????") {
            "??????????????????" to "??????????"
        }

        dateRule.setDate(2022, 6, 1)
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            notificationItem(CHECK_IN_DATE_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<DatePickerScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            waitUntil { openPickerEvent.isOccurred() }
            root.isViewStateMatches(getTestRelatedFilePath("picker"))
            cancelButton.click()
        }

        onScreen<RentShowingsScreen> {
            notificationItem(CHECK_IN_DATE_TEXT)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<DatePickerScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            confirmButton.click()
        }

        onScreen<RentShowingsScreen> {
            waitUntil { toastView(CHECK_IN_DATE_ERROR).isCompletelyDisplayed() }
            waitUntil { errorEvent.isOccurred() }
            notificationItem(CHECK_IN_DATE_TEXT)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<DatePickerScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            confirmButton.click()
        }

        onScreen<RentShowingsScreen> {
            waitUntil { successEvent.isOccurred() }
            notificationItem(WIDGET_TEXT)
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowTenantRentConditions() {
        configureWebServer {
            registerShowing(
                widget = showingWidget(
                    html = CONDITIONS_TEXT,
                    type = SHOWING_TYPE_ACCENT,
                    action = jsonObject {
                        "buttonText" to CONDITIONS_ACTION
                        "actionExploreConditionsOfHousingServices" to jsonObject {}
                    }
                )
            )
            registerShowing(widget = showingWidget(WIDGET_TEXT))
        }

        val openWebViewEvent = event("????????????. ?????????????? ?? ???????????????? ?????????????? ???? ??????") {
            "????????????????" to "???????????? ??????????????"
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentShowingsScreen> {
            notificationItem(CONDITIONS_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(UTILITIES_CONDITIONS_URL) }
            waitUntil { openWebViewEvent.isOccurred() }
            webView.invoke { evaluateJavascript(SUBMIT_FORM_SCRIPT) }
        }
        onScreen<RentShowingsScreen> {
            notificationItem(WIDGET_TEXT)
                .waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowRoommatesInvitation() {
        configureWebServer {
            registerShowing(
                widget = showingWidget(
                    html = ROOMMATES_INVITATION_TEXT,
                    type = SHOWING_TYPE_ACCENT,
                    action = jsonObject {
                        "buttonText" to ROOMMATES_INVITATION_ACTION
                        "actionShareLinkToQuestionnaire" to jsonObject {}
                    }
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesShareIntent(SHARED_LINK), null)

        onScreen<RentShowingsScreen> {
            notificationItem(ROOMMATES_INVITATION_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<WebViewScreen> {
            webView.waitUntil { isPageUrlEquals(ROOMMATES_INVITATION_URL) }
            event("????????????. ?????????????? ?? ???????????????? ???????????? ??????????????????") {
                "????????????????" to "???????????? ??????????????"
            }.waitUntil { isOccurred() }

            webView.invoke { evaluateJavascript(SHARE_LINK_SCRIPT) }

            waitUntil { intended(matchesShareIntent(SHARED_LINK)) }
        }
    }

    @Test
    fun shouldShowUpdateFallback() {
        configureWebServer {
            registerShowing(
                widget = showingWidget(
                    html = WIDGET_TEXT,
                    action = jsonObject {
                        "unknownAction" to jsonObject {}
                        "fallbackUpdate" to jsonObject {}
                    }
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerMarketIntent()

        onScreen<RentShowingsScreen> {
            notificationItem(UPDATE_NOTIFICATION_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }

            intended(matchesMarketIntent())
        }
    }

    @Test
    fun shouldOpenHtmlLink() {
        configureWebServer {
            registerShowing(widget = showingWidget(HTML_TEXT))
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(EXTERNAL_URL), null)

        onScreen<RentShowingsScreen> {
            notificationItem(FORMATTED_HTML_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { textView.tapOnLinkText(LINK_TEXT) }

            waitUntil { intended(matchesExternalViewUrlIntent(EXTERNAL_URL)) }
        }
    }

    @Test
    fun shouldShowUrlFallback() {
        configureWebServer {
            registerShowing(
                widget = showingWidget(
                    html = WIDGET_TEXT,
                    type = SHOWING_TYPE_ACCENT,
                    action = jsonObject {
                        "unknownAction" to jsonObject {}
                        "fallbackUrl" to jsonObject { "url" to EXTERNAL_URL }
                        "buttonText" to "??????????????"
                    }
                )
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesExternalViewUrlIntent(EXTERNAL_URL), null)

        onScreen<RentShowingsScreen> {
            notificationItem(WIDGET_TEXT)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }

            intended(matchesExternalViewUrlIntent(EXTERNAL_URL))
        }
    }

    private companion object {

        const val WIDGET_TEXT = "?????? ???????? ???? ??????????"
        const val ROOMMATE_NAME = "????????????"
        const val CHECK_IN_DATE_TEXT = "??????????????????????! ?????????????????????? ?????????? ?????????? ?????? ????????????????. " +
            "?????????????????? ?????? ??? ?????????????? ???????? ??????????????????. ?????????? ?????????????????? ?????????????? ?????????????? ???? ?????? " +
            "?? ?????????????????? ?????????????? ????????????. ???????????????? ???????????????? ??????, ?????????? ???????????????? ????????????."
        const val CHECK_IN_DATE_ACTION = "?????????????? ????????"
        const val CONDITIONS_TEXT = "???????????????????????? ?? ?????????????????? ???? ?????? ?????????? ?????????????????????? ????????????????"
        const val CONDITIONS_ACTION = "????????????????"
        const val ROOMMATES_INVITATION_TEXT = "?????????????????? ???????????? ?? ???????????????????? ?????????????? ?? ?????????????? " +
            "????????????????, ?? ?????? ???? ???????????????????? ????????. ?????????????????????? ?????????????????????? ?? ???????????? ???????????????? ?? " +
            "???????????? ?????????????? ?? ?????????? ????????????????."
        const val ROOMMATES_INVITATION_ACTION = "???????????????????? ??????????????"
        const val UPDATE_NOTIFICATION_TEXT = "?????????????????? ???????????????? ??\u00A0?????? ????????????????????. " +
            "????????????????????, ????????????????????."
        const val EXTERNAL_URL = "https://arenda.test.vertis.yandex.ru/external"
        const val LINK_TEXT = "????????????"
        const val HTML_TEXT = "???????? ?????????? ???????????????? <a href=\"$EXTERNAL_URL\">$LINK_TEXT</a>"
        const val FORMATTED_HTML_TEXT = "???????? ?????????? ???????????????? $LINK_TEXT"
    }
}
