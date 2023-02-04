package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.AgencyCardActivityTestRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AgencyCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.input.createSellAgencyParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 28.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AgencyCardFavoriteButtonTest : FavoriteButtonTest() {

    private val activityTestRule = AgencyCardActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeOfferFavoriteState() {
        testOfferSnippetFavoriteButton<AgencyCardScreen>(
            offerId = OFFER_ID,
            webServerConfiguration = {
                registerAgencyCard()
                registerOffer()
            },
            actionConfiguration = {
                val params = createSellAgencyParams(UID)
                val intent = AgencyCardActivityTestRule.createIntent(params)
                activityTestRule.launchActivity(intent)
                onScreen<AgencyCardScreen> {
                    appBar.collapse()
                    offerSnippet(OFFER_ID)
                        .waitUntil { listView.contains(this) }
                        .also { listView.scrollByFloatingButtonHeight() }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "на карточке агентства"
        )
    }

    private fun DispatcherRegistry.registerAgencyCard() {
        register(
            request {
                path("2.0/agencies/active/user/uid:$UID")
            },
            response {
                assetBody("agencyTest/agency.json")
            }
        )
        register(
            request {
                path("1.0/dynamicBoundingBox")
            },
            response {
                assetBody("agencyTest/agencyAllOffersBoundingBox.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    companion object {

        private const val UID = "1"
        private const val OFFER_ID = "0"
    }
}
