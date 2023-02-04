package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.ChatMessagesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.test.callButton.CallButtonTest
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 28/01/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class P2PChatTest : CallButtonTest() {

    private val activityTestRule = ChatMessagesActivityTestRule(
        chatId = CHAT_ID,
        launchActivity = false
    )
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun checkDeveloperChatToolbarMenu() {
        configureWebServer {
            registerChat("P2PChatTest/developerChat.json")
            registerEmptyListMessages()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            menuButton.waitUntil { isCompletelyDisplayed() }
                .click(true)
            menuWindow.isViewStateMatches(getTestRelatedFilePath("menuWindow"))
        }
    }

    @Test
    fun checkYandexRentOfferChatToolbarMenu() {
        configureWebServer {
            registerChat("P2PChatTest/yandexRentOfferChat.json")
            registerEmptyListMessages()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            menuButton.waitUntil { isCompletelyDisplayed() }
                .click(true)
            menuWindow.isViewStateMatches(getTestRelatedFilePath("menuWindow"))
        }
    }

    @Test
    fun shouldShowBlockedDevSiteChatCauseChatsDisabled() {
        val siteId = "0"
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = siteId,
            eventPlace = "CHAT",
            currentScreen = "CHAT"
        )
        dispatcher.registerChat("P2PChatTest/blockedDevSiteChatCauseChatsDisabled.json")
        dispatcher.registerEmptyListMessages()
        dispatcher.registerSitePhone(siteId)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = siteId,
            source = "чат с застройщиком",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))

            callButton.click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowBlockedDevSiteIdChatCauseChatsDisabled() {
        configureWebServer {
            registerChat("P2PChatTest/blockedDevSiteIdChatCauseChatsDisabled.json")
            registerEmptyListMessages()
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("type", "SELL")
                    queryParam("category", "APARTMENT")
                    queryParam("objectType", "NEWBUILDING")
                    queryParam("rgid", "587795")
                    excludeQueryParamKey("countOnly")
                },
                response {
                    assetBody("P2PChatTest/otherSitesSearch.json")
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))

            otherSitesButton.click()
        }

        onScreen<SearchListScreen> {
            siteSnippet("1").waitUntil { listView.contains(this, false) }
        }
    }

    @Test
    fun shouldShowBlockedDevOfferIdSiteIdChatCauseChatsDisabled() {
        configureWebServer {
            registerChat("P2PChatTest/blockedDevOfferIdSiteIdChatCauseChatsDisabled.json")
            registerEmptyListMessages()
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("type", "SELL")
                    queryParam("category", "APARTMENT")
                    queryParam("objectType", "NEWBUILDING")
                    queryParam("rgid", "587795")
                    excludeQueryParamKey("countOnly")
                },
                response {
                    assetBody("P2PChatTest/otherSitesSearch.json")
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))

            otherSitesButton.click()
        }

        onScreen<SearchListScreen> {
            siteSnippet("1").waitUntil { listView.contains(this, false) }
        }
    }

    @Test
    fun shouldShowBlockedDevOfferChatCauseChatsDisabled() {
        val offerId = "1"
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = offerId,
            eventPlace = "CHAT",
            currentScreen = "CHAT"
        )
        dispatcher.registerChat("P2PChatTest/blockedDevOfferChatCauseChatsDisabled.json")
        dispatcher.registerEmptyListMessages()
        dispatcher.registerOfferPhone(offerId)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = offerId,
            source = "чат с застройщиком",
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))

            callButton.click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowBlockedDevInactiveOfferChatCauseChatsDisabled() {
        val siteId = "46511"
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = siteId,
            eventPlace = "CHAT",
            currentScreen = "CHAT"
        )
        dispatcher.registerChat("P2PChatTest/blockedDevInactiveOfferChatCauseChatsDisabled.json")
        dispatcher.registerEmptyListMessages()
        dispatcher.registerSitePhone(siteId)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = siteId,
            source = "чат с застройщиком",
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))

            callButton.click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowBlockedDevInactiveOfferSiteIdChatCauseChatsDisabled() {
        configureWebServer {
            registerChat("P2PChatTest/blockedDevInactiveOfferSiteIdChatCauseChatsDisabled.json")
            registerEmptyListMessages()
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("type", "SELL")
                    queryParam("category", "APARTMENT")
                    queryParam("objectType", "NEWBUILDING")
                    queryParam("rgid", "587795")
                    excludeQueryParamKey("countOnly")
                },
                response {
                    assetBody("P2PChatTest/otherSitesSearch.json")
                }
            )
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))

            otherSitesButton.click()
        }

        onScreen<SearchListScreen> {
            siteSnippet("1").waitUntil { listView.contains(this, false) }
        }
    }

    @Test
    fun shouldShowBlockedChatCauseChatsDisabled() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseChatsDisabled.json")
            registerEmptyListMessages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))
        }
    }

    @Test
    fun shouldBlockAndUnblockChat() {
        configureWebServer {
            registerEmptyChat()
            registerEmptyListMessages()
            registerMarkChatSuccess("block")
            registerMarkChatSuccess("unblock")
            registerMarkChatSuccess("block")
            registerMarkChatSuccess("unblock")
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }
            isViewStateMatches("P2PChatTest/shouldBlockAndUnblockChat/unblocked")

            tapOn(lookup.matchesToolbarMenuButton(), true)
            tapOn(lookup.matchesToolbarMenuBlockButton())

            waitUntil { isToastShown("Чат заблокирован") }
            waitUntil { isUnblockButtonShown() }
            isViewStateMatches("P2PChatTest/shouldBlockAndUnblockChat/blocked")

            tapOn(lookup.matchesToolbarMenuButton(), true)
            tapOn(lookup.matchesToolbarMenuUnblockButton())

            waitUntil { isToastShown("Чат разблокирован") }
            waitUntil { isInputFieldShown() }
            isViewStateMatches("P2PChatTest/shouldBlockAndUnblockChat/unblocked")

            tapOn(lookup.matchesToolbarMenuButton(), true)
            tapOn(lookup.matchesToolbarMenuBlockButton())

            waitUntil { isToastShown("Чат заблокирован") }
            waitUntil { isUnblockButtonShown() }
            isViewStateMatches("P2PChatTest/shouldBlockAndUnblockChat/blocked")

            tapOn(lookup.matchesUnblockButton())

            waitUntil { isToastShown("Чат разблокирован") }
            waitUntil { isInputFieldShown() }
            isViewStateMatches("P2PChatTest/shouldBlockAndUnblockChat/unblocked")
        }
    }

    @Test
    fun shouldShowBlockedChatCauseUnknown() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseUnknown.json")
            registerEmptyListMessages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseUnknown/blocked")
        }
    }

    @Test
    fun shouldShowBlockedChat() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChat.json")
            registerEmptyListMessages()
            registerMarkChatSuccess("unblock")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChat/blocked")

            unblockButton.click()

            waitUntil { inputView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChat/unblocked")
        }
    }

    @Test
    fun shouldShowBlockedByUserChat() {
        configureWebServer {
            registerChat("P2PChatTest/blockedByUserChat.json")
            registerEmptyListMessages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedByUserChat/blocked")
        }
    }

    @Test
    fun shouldShowBlockedChatCauseBanned() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseBanned.json")
            registerEmptyListMessages()
            registerTechSupportChat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseBanned/blocked")

            supportButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { errorView.isCompletelyDisplayed() }

            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseBanned/support")
        }
    }

    @Test
    fun shouldShowBlockedChatCauseUserBanned() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseUserBanned.json")
            registerEmptyListMessages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseUserBanned/blocked")
        }
    }

    @Test
    fun shouldShowBlockedChatCauseOwnerType() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseOwnerType.json")
            registerEmptyListMessages()
            registerTechSupportChat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseOwnerType/blocked")

            supportButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { errorView.isCompletelyDisplayed() }

            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseOwnerType/support")
        }
    }

    @Test
    fun shouldShowBlockedChatCauseUserOwnerType() {
        val offerId = "731271620849149185"
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = offerId,
            eventPlace = "CHAT",
            currentScreen = "CHAT"
        )
        dispatcher.registerChat("P2PChatTest/blockedChatCauseUserOwnerType.json")
        dispatcher.registerEmptyListMessages()
        dispatcher.registerOfferPhone("731271620849149185")
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = offerId,
            source = "чат по объявлению",
            categories = jsonArrayOf("SecondaryHouse_Sell", "Sell")
        )

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches("P2PChatTest/shouldShowBlockedChatCauseUserOwnerType/blocked")

            callButton.click()

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowBlockedChatCauseUserOwnerTypeButOfferRemoved() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatNoSubjectCauseUserOwnerType.json")
            registerEmptyListMessages()
            registerTechSupportChat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(
                "P2PChatTest/shouldShowBlockedChatCauseUserOwnerTypeButOfferRemoved/blocked"
            )
        }
    }

    @Test
    fun shouldShowBlockedChatCauseFlatCanNoLongerBeRented() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseFlatCanNoLongerBeRented.json")
            registerEmptyListMessages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))
        }
    }

    @Test
    fun shouldShowBlockedChatCauseRentCallCenterClosedChat() {
        configureWebServer {
            registerChat("P2PChatTest/blockedChatCauseRentCallCenterClosedChat.json")
            registerEmptyListMessages()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("blocked"))
        }
    }

    @Test
    fun shouldBlockChatWhen403ErrorReceived() {
        configureWebServer {
            registerEmptyChat()
            registerEmptyListMessages()
            registerSendTextMessage403()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }
            isViewStateMatches("P2PChatTest/shouldBlockChatWhen403ErrorReceived/unblocked")

            typeText(lookup.matchesInputField(), "blocked")
            tapOn(lookup.matchesSendButton())

            waitUntil { isBlockedWarningViewShown() }
            isViewStateMatches("P2PChatTest/shouldBlockChatWhen403ErrorReceived/blocked")
        }
    }

    @Test
    fun shouldShowUnpublishedOfferSnippet() {
        configureWebServer {
            registerEmptyChatUnpublishedOffer()
            registerEmptyListMessages()
            registerCardWithViewsUnpublishedOffer()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }
            isViewStateMatches("P2PChatTest/shouldShowUnpublishedOfferSnippet/viewState")

            tapOn(lookup.matchesSubjectView())
        }

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("3\u00a0608\u00a0556\u00a0\u20BD") }
        }
    }

    @Test
    fun shouldShowMissingOfferReference() {
        configureWebServer {
            registerEmptyChatSubjectError()
            registerEmptyListMessages()
            registerCardWithViewsUnpublishedOffer()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }
            isViewStateMatches("P2PChatTest/shouldShowMissingOfferReference/viewState")

            tapOn(lookup.matchesSubjectView())
        }

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("3\u00a0608\u00a0556\u00a0\u20BD") }
        }
    }

    @Test
    fun shouldShowRemovedOfferReference() {
        configureWebServer {
            registerEmptyChatWithoutSubject()
            registerEmptyListMessages()
            registerCardWithViewsUnpublishedOffer()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        performOnChatMessagesScreen {
            waitUntil { isFullscreenEmptyViewShown() }
            isViewStateMatches("P2PChatTest/shouldShowRemovedOfferReference/viewState")

            tapOn(lookup.matchesSubjectView())
        }

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("3\u00a0608\u00a0556\u00a0\u20BD") }
        }
    }

    private fun DispatcherRegistry.registerEmptyChat() {
        registerChat("P2PChatTest/emptyChat.json")
    }

    private fun DispatcherRegistry.registerChat(fileName: String) {
        register(
            request {
                path("2.0/chat/room/$CHAT_ID")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    /**
     * пустой список региструруем 2 раза, т.к. может быть ситуация с 2 запросами подряд, когда
     * вызывается обновление из - за screenHolder и из - за getMessages при старте экрана
     */
    private fun DispatcherRegistry.registerEmptyListMessages() {
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
        register(
            request {
                path("2.0/chat/messages/room/$CHAT_ID")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyChatUnpublishedOffer() {
        register(
            request {
                path("2.0/chat/room/$CHAT_ID")
            },
            response {
                assetBody("P2PChatTest/emptyChatUnpublishedOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyChatSubjectError() {
        register(
            request {
                path("2.0/chat/room/$CHAT_ID")
            },
            response {
                assetBody("P2PChatTest/emptyChatSubjectError.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptyChatWithoutSubject() {
        register(
            request {
                path("2.0/chat/room/$CHAT_ID")
            },
            response {
                assetBody("P2PChatTest/emptyChatWithoutSubject.json")
            }
        )
    }

    private fun DispatcherRegistry.registerCardWithViewsUnpublishedOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("P2PChatTest/cardWithViewsUnpublishedOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMarkChatSuccess(action: String) {
        register(
            request {
                method("PUT")
                path("2.0/chat/room/$CHAT_ID/mark/$action")
            },
            success()
        )
    }

    private fun DispatcherRegistry.registerSendTextMessage403() {
        register(
            request {
                method("POST")
                path("2.0/chat/messages")
            },
            response {
                setResponseCode(403)
                setBody("""{"error":{"code": "CHAT_FORBIDDEN"}}""")
            }
        )
    }

    private fun DispatcherRegistry.registerTechSupportChat() {
        register(
            request {
                path("2.0/chat/room/tech-support")
            },
            response {
                assetBody("techSupportChatCommon.json")
            }
        )
    }

    companion object {
        private const val CHAT_ID = "test_chat_id"
    }
}
