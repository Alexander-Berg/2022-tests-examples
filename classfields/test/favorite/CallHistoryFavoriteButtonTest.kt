package com.yandex.mobile.realty.test.favorite

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.CallHistoryActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.CallHistoryScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.input.createOfferPreview
import com.yandex.mobile.realty.input.createSitePreview
import com.yandex.mobile.realty.input.createVillagePreview
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 27.04.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CallHistoryFavoriteButtonTest : FavoriteButtonTest() {

    private val offer = createOfferPreview(OFFER_ID)
    private val site = createSitePreview(SITE_ID)
    private val village = createVillagePreview(VILLAGE_ID)

    private val activityTestRule = CallHistoryActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        DatabaseRule(
            DatabaseRule.createAddCallsHistoryEntryStatement(offer),
            DatabaseRule.createAddCallsHistoryEntryStatement(site),
            DatabaseRule.createAddCallsHistoryEntryStatement(village)
        ),
        MetricaEventsRule()
    )

    @Test
    fun shouldChangeOfferFavoriteState() {
        testOfferSnippetFavoriteButton<CallHistoryScreen>(
            offerId = OFFER_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<CallHistoryScreen> {
                    offerSnippet(OFFER_ID)
                        .waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { offerSnippet(OFFER_ID).view },
            offerCategories = arrayListOf("Sell", "SecondaryFlat_Sell"),
            metricaSource = "на экране истории звонков"
        )
    }

    @Test
    fun shouldChangeSiteFavoriteState() {
        testSiteSnippetFavoriteButton<CallHistoryScreen>(
            siteId = SITE_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<CallHistoryScreen> {
                    siteSnippet(SITE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { siteSnippet(SITE_ID).view },
            siteCategories = arrayListOf("Sell", "ZhkNewbuilding_Sell"),
            metricaSource = "на экране истории звонков"
        )
    }

    @Test
    fun shouldChangeVillageFavoriteState() {
        testVillageSnippetFavoriteButton<CallHistoryScreen>(
            villageId = VILLAGE_ID,
            actionConfiguration = {
                activityTestRule.launchActivity()
                onScreen<CallHistoryScreen> {
                    villageSnippet(VILLAGE_ID).waitUntil { listView.contains(this) }
                }
            },
            snippetViewSelector = { villageSnippet(VILLAGE_ID).view },
            villageCategories = arrayListOf("Village_Sell", "Sell"),
            metricaSource = "на экране истории звонков"
        )
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"
    }
}
