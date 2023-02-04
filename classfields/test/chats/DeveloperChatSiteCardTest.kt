package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.screen.SiteSpecialsScreen
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
 * @author misha-kozlov on 3/25/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DeveloperChatSiteCardTest : ChatButtonTest() {

    private val activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
        launchActivity = false
    )
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule,
        authorizationRule,
    )

    @Test
    fun shouldStartChatWhenButtonPressed() {
        configureWebServer {
            registerSite()
            registerEmptyChat()
            registerEmptyListMessages(CHAT_ID)
            registerEmptyListMessages(CHAT_ID)
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteCardScreen> {
            waitUntil { floatingCommButtons.isCompletelyDisplayed() }

            floatingCommButtons.isViewStateMatches(getTestRelatedFilePath("floatingButtons"))

            floatingChatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))

            pressBack()
        }

        onScreen<SiteCardScreen> {
            appBar.collapse()
            listView.scrollTo(commButtons)
            listView.scrollByFloatingButtonHeight()

            commButtons.view.isViewStateMatches(getTestRelatedFilePath("contentButtons"))

            chatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))
        }
    }

    @Test
    fun shouldStartChatWhenButtonOnSpecialsScreenPressed() {
        configureWebServer {
            registerSite()
            registerEmptyChat()
            registerEmptyListMessages(CHAT_ID)
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            listView.waitUntil { contains(specialsProposals) }
            specialProposalsItem("Скидка 20%").click()
        }

        onScreen<SiteSpecialsScreen> {
            chatButton.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("siteSpecialsViewState"))
            chatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))
        }
    }

    @Test
    fun shouldStartChatWhenSimilarSiteChatButtonPressed() {
        configureWebServer {
            registerSite()
            registerSimilarSites()
            registerSnippetSiteChat()
            registerSnippetSiteChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteCardScreen> {
            waitUntil { floatingChatButton.isCompletelyDisplayed() }

            appBar.collapse()

            siteSnippet(SNIPPET_SITE_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteChatViewState()
    }

    @Test
    fun shouldStartChatWhenDeveloperSiteChatButtonPressed() {
        configureWebServer {
            registerSite()
            registerDeveloperSites()
            registerSnippetSiteChat()
            registerSnippetSiteChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteCardScreen> {
            waitUntil { floatingChatButton.isCompletelyDisplayed() }

            appBar.collapse()

            siteSnippet(SNIPPET_SITE_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteChatViewState()
    }

    @Test
    fun shouldStartChatWhenGalleryChatButtonPressed() {
        configureWebServer {
            registerSite()
            registerEmptyChat()
            registerEmptyListMessages(CHAT_ID)
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SiteCardScreen> {
            gallery.waitUntil { isCompletelyDisplayed() }.click()
        }

        onScreen<GalleryScreen> {
            galleryView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("galleryViewState"))
            chatButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { emptyView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("chatViewState"))
        }
    }

    @Test
    fun shouldNotShowChatButton() {
        configureWebServer {
            registerSiteNoChat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            waitUntil { floatingCallButton.isCompletelyDisplayed() }

            floatingChatButton.doesNotExist()

            appBar.collapse()
            listView.scrollTo(commButtons)

            chatButton.doesNotExist()
        }
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("DeveloperChatSiteCardTest/siteWithOffersStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteNoChat() {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
            },
            response {
                assetBody("DeveloperChatSiteCardTest/siteWithOffersStatNoChat.json")
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

    private fun DispatcherRegistry.registerSimilarSites() {
        register(
            request {
                method("GET")
                path("1.0/newbuilding/siteLikeSearch")
                queryParam("siteId", SITE_ID)
                queryParam("excludeSiteId", SITE_ID)
                queryParam("pageSize", "4")
            },
            response {
                assetBody("DeveloperChatSiteCardTest/siteLikeSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerDeveloperSites() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("ChatButtonTest/siteSearch.json")
            }
        )
    }

    companion object {

        private const val SITE_ID = "0"
        private const val CHAT_ID = "abc"
    }
}
