package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnUserOffersScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class UserOffersEditableTest {

    private val activityTestRule = UserOffersActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun notEditableWhenPublishedAndFromFeed() {
        val id = "2"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersPublishedFromFeed.json")
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { containsOffer(id) }

            performOnSnippet(id) {
                isPriceNotEditable()
                isProlongateButtonHidden()
            }
        }
    }

    @Test
    fun notEditableWhenUnpublishedAndFromFeed() {
        val id = "4"

        configureWebServer {
            registerUserOfferList("userOffers/userOffersUnpublishedFromFeed.json")
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { containsOffer(id) }

            performOnSnippet(id) {
                isPriceNotEditable()
                isRemoveButtonHidden()
                isActivateButtonHidden()
            }
        }
    }

    private fun DispatcherRegistry.registerUserOfferList(responseFileName: String) {
        registerNoRequiredFeatures()
        registerUserProfile()
        registerUserProfile()
        registerUserOffers(responseFileName)
    }

    private fun DispatcherRegistry.registerNoRequiredFeatures() {
        register(
            request {
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": []}}")
            }
        )
    }

    private fun DispatcherRegistry.registerUserOffers(responseFileName: String) {
        register(
            request {
                path("2.0/user/me/offers")
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                path("1.0/user")
            },
            response {
                assetBody("user/userOwner.json")
            }
        )
    }
}
