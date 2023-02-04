package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 10.02.2022
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class YandexRentChatOfferCardTest : ChatButtonTest() {

    private val activityTestRule = OfferCardActivityTestRule(
        YANDEX_RENT_OFFER_ID,
        launchActivity = false
    )
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldStartChatWhenButtonPressed() {
        configureWebServer {
            registerOffer()
            registerYandexRentOfferChat()
            registerYandexRentOfferChatMessages()
            registerYandexRentOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }

            floatingCommButtons.isViewStateMatches(getTestRelatedFilePath("floatingButtons"))

            floatingChatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))

            pressBack()
        }

        onScreen<OfferCardScreen> {
            appBar.collapse()
            listView.scrollTo(commButtonsItem)
            listView.scrollByFloatingButtonHeight()

            commButtonsItem.view.isViewStateMatches(getTestRelatedFilePath("contentButtons"))

            chatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))
        }
    }

    @Test
    fun shouldShowChatErrorWhenFlatCanNoLongerBeRented() {
        configureWebServer {
            registerOffer()
            registerChatErrorFlatCanNoLongerBeRented()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }
            floatingChatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { errorView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("error"))
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("YandexRentChatOfferCardTest/cardWithViews.json")
            }
        )
    }

    private fun DispatcherRegistry.registerChatErrorFlatCanNoLongerBeRented() {
        register(
            request {
                method("POST")
                path("2.0/chat/room/offer/$YANDEX_RENT_OFFER_ID")
            },
            response {
                setResponseCode(403)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CHAT_FORBIDDEN"
                        "chatActionForbiddenReason" to jsonObject {
                            "flatCanNoLongerBeRented" to jsonObject {}
                        }
                    }
                }
            }
        )
    }
}
