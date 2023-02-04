package com.yandex.mobile.realty.test.chats

import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest

/**
 * @author rogovalex on 16/03/2021.
 */
abstract class ChatButtonTest : BaseTest() {

    protected fun checkSnippetOfferChatViewState() {
        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(
                "ChatButtonTest/checkSnippetOfferChatViewState/snippetOfferChatViewState"
            )
        }
    }

    protected fun checkSnippetSiteOfferChatViewState() {
        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(
                "ChatButtonTest/checkSnippetOfferChatViewState/snippetSiteOfferChatViewState"
            )
        }
    }

    protected fun checkSnippetSiteChatViewState() {
        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(
                "ChatButtonTest/checkSnippetOfferChatViewState/snippetSiteChatViewState"
            )
        }
    }

    protected fun checkSnippetYandexRentOfferChatViewState() {
        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(
                "ChatButtonTest/checkSnippetOfferChatViewState/" +
                    "snippetYandexRentOfferChatViewState"
            )
        }
    }

    protected fun DispatcherRegistry.registerSnippetOfferChat() {
        register(
            request {
                path("2.0/chat/room/offer/$SNIPPET_OFFER_ID")
            },
            response {
                assetBody("ChatButtonTest/snippetOfferEmptyChat.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerSnippetSiteOfferChat() {
        register(
            request {
                path("2.0/chat/room/developer/offer/$SNIPPET_SITE_OFFER_ID")
            },
            response {
                assetBody("ChatButtonTest/snippetSiteOfferEmptyChat.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerSnippetSiteChat() {
        register(
            request {
                path("2.0/chat/room/developer/site/$SNIPPET_SITE_ID")
            },
            response {
                assetBody("ChatButtonTest/snippetSiteEmptyChat.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerYandexRentOfferChat() {
        register(
            request {
                path("2.0/chat/room/offer/$YANDEX_RENT_OFFER_ID")
            },
            response {
                assetBody("ChatButtonTest/yandexRentOfferEmptyChat.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerSnippetOfferChatMessages() {
        registerEmptyListMessages(SNIPPET_OFFER_CHAT_ID)
    }

    protected fun DispatcherRegistry.registerSnippetSiteOfferChatMessages() {
        registerEmptyListMessages(SNIPPET_SITE_OFFER_CHAT_ID)
    }

    protected fun DispatcherRegistry.registerSnippetSiteChatMessages() {
        registerEmptyListMessages(SNIPPET_SITE_CHAT_ID)
    }

    protected fun DispatcherRegistry.registerYandexRentOfferChatMessages() {
        registerEmptyListMessages(YANDEX_RENT_OFFER_CHAT_ID)
    }

    /**
     * пустой список региструруем 2 раза, т.к. может быть ситуация с 2 запросами подряд, когда
     * вызывается обновление из - за screenHolder и из - за getMessages при старте экрана
     */
    protected fun DispatcherRegistry.registerEmptyListMessages(chatId: String) {
        register(
            request {
                path("2.0/chat/messages/room/$chatId")
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
                path("2.0/chat/messages/room/$chatId")
                queryParam("asc", "false")
                queryParam("count", "30")
                excludeQueryParamKey("from")
            },
            response {
                setBody("""{"response":{"messages":[]}}""")
            }
        )
    }

    protected companion object {

        const val SNIPPET_OFFER_ID = "918641003332915200"
        const val SNIPPET_OFFER_CHAT_ID = "f866e7379e649f07c760fca0d96af260"

        const val SNIPPET_SITE_OFFER_ID = "4727644224367723204"
        const val SNIPPET_SITE_OFFER_CHAT_ID = "dev59a762e8e97312c3f4c4b721ae6e055b"

        const val SNIPPET_SITE_ID = "46511"
        const val SNIPPET_SITE_CHAT_ID = "devdeebc707dac7f51d4c8b9a69c5ed1986"

        const val YANDEX_RENT_OFFER_ID = "6405294404798162258"
        const val YANDEX_RENT_OFFER_CHAT_ID = "roch8ea5687d8c9046663e36385eeddacf89"
    }
}
