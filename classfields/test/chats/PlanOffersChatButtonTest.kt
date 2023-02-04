package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PlanOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.PlanOffersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author merionkov on 21.10.2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlanOffersChatButtonTest : ChatButtonTest() {

    private val activityTestRule = PlanOffersActivityTestRule(
        siteId = SITE_ID,
        planId = PLAN_ID,
        launchActivity = false,
    )

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        activityTestRule,
    )

    @Test
    fun shouldStartChatWhenButtonPressed() {

        configureWebServer {
            registerOfferStat()
            registerOffersCount()
            registerOfferWithSiteSearchPlanOffers()
            registerEmptyChat()
            registerEmptyListMessages(CHAT_ID)
        }

        activityTestRule.shouldDisplayChatButton = true
        activityTestRule.launchActivity()

        authorizationRule.registerAuthorizationIntent()

        onScreen<PlanOffersScreen> {
            callButton.waitUntil { isCompletelyDisplayed() }
            chatButton.isCompletelyDisplayed()
            root.isViewStateMatches(getTestRelatedFilePath("planOffersViewState"))
            chatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))
        }
    }

    @Test
    fun shouldNotDisplayChatButton() {

        configureWebServer {
            registerOfferStat()
            registerOffersCount()
            registerOfferWithSiteSearchPlanOffers()
        }

        activityTestRule.shouldDisplayChatButton = false
        activityTestRule.launchActivity()

        onScreen<PlanOffersScreen> {
            callButton.waitUntil { isCompletelyDisplayed() }
            chatButton.doesNotExist()
            root.isViewStateMatches(getTestRelatedFilePath("planOffersViewState"))
        }
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                assetBody("offerStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffersCount() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("countOnly", "true")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                setBody("{\"response\":{\"total\":3}}")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferWithSiteSearchPlanOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("siteFlatPlanId", PLAN_ID)
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyChat() {
        register(
            request {
                path("2.0/chat/room/developer/site/$SITE_ID")
            },
            response {
                assetBody("DeveloperChatSiteCardTest/emptySiteChat.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "2"
        private const val PLAN_ID = "6"
        private const val CHAT_ID = "abc"
    }
}
