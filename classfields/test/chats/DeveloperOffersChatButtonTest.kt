package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.activity.DeveloperOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.DeveloperOffersScreen
import com.yandex.mobile.realty.core.screen.Screen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author merionkov on 07.04.2022.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperOffersChatButtonTest : ChatButtonTest() {

    private val activityTestRule = DeveloperOffersActivityTestRule(
        siteId = SITE_ID,
        siteName = SITE_NAME,
        launchActivity = false,
    )

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val rule = baseChainOf(activityTestRule, authorizationRule)

    @Test
    fun shouldStartChat() {
        configureWebServer {
            registerOfferStat()
            registerOffersSearch()
            registerEmptyChat()
            registerEmptyListMessages(CHAT_ID)
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        Screen.onScreen<DeveloperOffersScreen> {
            chatButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        Screen.onScreen<ChatMessagesScreen> {
            emptyView.waitUntil { isCompletelyDisplayed() }
        }
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
            },
            response {
                assetBody("DeveloperOffersChatButtonTest/offerStat.json")
            },
        )
    }

    private fun DispatcherRegistry.registerOffersSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("DeveloperOffersChatButtonTest/offersSearch.json")
            },
        )
    }

    private fun DispatcherRegistry.registerEmptyChat() {
        register(
            request {
                path("2.0/chat/room/developer/site/$SITE_ID")
            },
            response {
                assetBody("DeveloperOffersChatButtonTest/emptySiteChat.json")
            },
        )
    }

    private companion object {
        const val SITE_ID = "0"
        const val SITE_NAME = "stub"
        const val CHAT_ID = "abc"
    }
}
