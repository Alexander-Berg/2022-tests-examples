package com.yandex.mobile.realty.test.callHistory

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.BottomNavMenu
import com.yandex.mobile.realty.core.screen.CallHistoryScreen
import com.yandex.mobile.realty.core.screen.CommunicationScreen
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.permission.Permission
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author scrooge on 22.04.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CallHistoryTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value),
        activityTestRule,
    )

    @Test
    fun shouldShowEmptyCallHistory() {

        activityTestRule.launchActivity()

        onScreen<BottomNavMenu> {
            waitUntil { bottomNavView.isCompletelyDisplayed() }
            commItemView.click(true)
        }

        onScreen<CommunicationScreen> {
            callHistoryTabView.click()
        }

        onScreen<CallHistoryScreen> {
            listView.isContentStateMatches("CallHistoryTest/shouldShowEmptyCallHistory/viewState")
        }
    }

    @Test
    fun shouldClearCallHistoryOnOfferHiddenFromSearch() {

        configureWebServer {
            registerFilledOfferSearch()
            registerOfferPhone()
            registerHideOffer()
        }

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SearchListScreen> {
            offerSnippet(TEST_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { callButton.click() }
                .waitUntil { isCallStated() }
        }
        onScreen<BottomNavMenu> {
            commItemView.click()
        }
        onScreen<CommunicationScreen> {
            callHistoryTabView.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<CallHistoryScreen> {
            offerSnippet(TEST_OFFER_ID).waitUntil { listView.contains(this) }
        }
        onScreen<BottomNavMenu> {
            searchItemView.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet(TEST_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { menuButton.click() }
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<SearchListScreen> {
            listView.waitUntil { doesNotContain(offerSnippet(TEST_OFFER_ID)) }
        }
        onScreen<BottomNavMenu> {
            commItemView.click()
        }
        onScreen<CommunicationScreen> {
            callHistoryTabView.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<CallHistoryScreen> {
            emptyView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldClearCallHistoryOnOfferHiddenFromCalls() {

        configureWebServer {
            registerFilledOfferSearch()
            registerOfferPhone()
            registerHideOffer()
        }

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SearchListScreen> {
            offerSnippet(TEST_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { callButton.click() }
                .waitUntil { isCallStated() }
        }
        onScreen<BottomNavMenu> {
            commItemView.click()
        }
        onScreen<CommunicationScreen> {
            callHistoryTabView.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<CallHistoryScreen> {
            offerSnippet(TEST_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { menuButton.click() }
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<CallHistoryScreen> {
            emptyView.waitUntil { isCompletelyDisplayed() }
        }
    }

    private companion object {

        const val TEST_OFFER_ID = "0"
        const val PHONE_NUMBER = "+79998887766"
        const val STAT_PARAMS = "FFFF2222"

        fun registerCallIntent() {
            Intents.intending(matchesCallIntent()).respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_OK, null),
            )
        }

        fun isCallStated() {
            NamedIntents.intended(matchesCallIntent())
        }

        fun matchesCallIntent(): Matcher<Intent> {
            return NamedIntentMatcher(
                "запуск звонилки с номером $PHONE_NUMBER",
                CoreMatchers.allOf(
                    IntentMatchers.hasAction(Intent.ACTION_CALL),
                    IntentMatchers.hasData("tel:$PHONE_NUMBER"),
                ),
            )
        }

        fun DispatcherRegistry.registerFilledOfferSearch() {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                },
                response {
                    assetBody("offerWithSiteSearchOffer.json")
                },
            )
        }

        fun DispatcherRegistry.registerOfferPhone() {
            register(
                request {
                    path("2.0/offers/$TEST_OFFER_ID/phones")
                },
                response {
                    setBody(
                        """{
                                "response": {
                                    "contacts": [{
                                        "phones": [{
                                            "phoneNumber": "$PHONE_NUMBER"
                                        }],
                                        "statParams": "$$STAT_PARAMS"
                                    }]
                                }
                            }"""
                    )
                }
            )
        }

        fun DispatcherRegistry.registerHideOffer() {
            register(
                request {
                    path("1.0/user/me/personalization/hideOffers")
                },
                success(),
            )
        }
    }
}
