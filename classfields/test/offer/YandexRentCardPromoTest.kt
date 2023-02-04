package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction
import com.yandex.mobile.realty.core.robot.OfferCardRobot
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author andrey-bgm on 18/02/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class YandexRentCardPromoTest {

    private var activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun showYandexRentPromoBlock() {
        configureWebServer {
            registerOfferCardWithYandexRent()
        }

        activityTestRule.launchActivity()

        performOnOfferCardScreen {
            waitUntil { isFloatingCallButtonShown() }
            collapseAppBar()

            waitUntil { containsYandexRentPromoBlock() }
            isPromoBlockMatches("/YandexRentCardPromoTest/yandexRentPromoBlock")
        }
    }

    private fun OfferCardRobot.isPromoBlockMatches(key: String) {
        NamedViewInteraction.onView(lookup.matchesYandexRentPromoBlock())
            .takeViewScreenshot()
            .check(key)
    }

    private fun DispatcherRegistry.registerOfferCardWithYandexRent() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithYandexRentInfo.json")
            }
        )
    }
}
