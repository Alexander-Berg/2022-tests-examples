package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ChatMessagesScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
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
 * @author scrooge on 22.04.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class UserOffersTest {

    private val activityTestRule = UserOffersActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldShowStoryUnavailable() {
        configureWebServer {
            registerRequiredUnsupportedFeature()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            waitUntil { storyUnavailableView.isCompletelyDisplayed() }

            root.isViewStateMatches("UserOffersTest/shouldShowStoryUnavailable")
        }
    }

    @Test
    fun shouldShowUnauthorizedList() {
        configureWebServer {
            registerRequiredSupportedFeatures()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            waitUntil { unauthorizedView.isCompletelyDisplayed() }

            root.isViewStateMatches("UserOffersTest/shouldShowUnauthorizedList")
        }
    }

    @Test
    fun shouldShowUserBlocked() {
        configureWebServer {
            registerRequiredSupportedFeatures()
            registerBlockedUserProfile()
            registerBlockedUserProfile()
            registerTechSupportChat()
        }
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            waitUntil { blockedView.isCompletelyDisplayed() }

            root.isViewStateMatches("/UserOffersTest/shouldShowUserBlocked")

            blockedViewActionButton.click()
        }

        onScreen<ChatMessagesScreen> {
            waitUntil { titleView.isTextEquals(R.string.chat_support_title) }
            pressBack()
        }

        onScreen<UserOffersScreen> {
            waitUntil { blockedView.isCompletelyDisplayed() }
        }
    }

    private fun DispatcherRegistry.registerRequiredUnsupportedFeature() {
        register(
            request {
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": [\"UNSUPPORTED_FEATURE\"]}}")
            }
        )
    }

    private fun DispatcherRegistry.registerRequiredSupportedFeatures() {
        register(
            request {
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": [\"PAID_LISTING\"]}}")
            }
        )
    }

    private fun DispatcherRegistry.registerBlockedUserProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userBlocked.json")
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
}
