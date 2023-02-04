package com.yandex.mobile.realty.test.userOffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnUserOfferCardScreen
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
class UserOfferCardEditableTest {

    private val activityTestRule =
        UserOfferCardActivityTestRule(offerId = OFFER_ID, launchActivity = false)

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
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferPublishedFromFeed.json")
        }

        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }

            isPriceNotEditable()

            collapseAppBar()

            isEditButtonHidden()
            doesNotContainsDeactivateButton()
            doesNotContainsProlongateButton()
        }
    }

    @Test
    fun notEditableWhenUnpublishedAndFromFeed() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferUnpublishedFromFeed.json")
        }

        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }

            isPriceNotEditable()

            collapseAppBar()

            isEditButtonHidden()
            doesNotContainsActivateButton()
            doesNotContainsRemoveButton()
        }
    }

    @Test
    fun notEditableWhenBannedAndFromFeed() {
        configureWebServer {
            registerUserOfferCard("userOffer/userOfferBannedFromFeed.json")
        }

        activityTestRule.launchActivity()

        performOnUserOfferCardScreen {
            waitUntil { isPriceViewShown() }

            isPriceNotEditable()

            collapseAppBar()

            isEditButtonHidden()
            doesNotContainsRemoveButton()
        }
    }

    private fun DispatcherRegistry.registerUserOfferCard(responseFileName: String) {
        registerUserProfile()
        registerUserProfile()
        registerUserOffer(responseFileName)
    }

    private fun DispatcherRegistry.registerUserOffer(responseFileName: String) {
        register(
            request {
                path("2.0/user/me/offers/$OFFER_ID/card")
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

    private companion object {

        private const val OFFER_ID = "1"
    }
}
