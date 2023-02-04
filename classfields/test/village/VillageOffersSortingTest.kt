package com.yandex.mobile.realty.test.village

import com.yandex.mobile.realty.activity.VillageCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SortingDialogScreen
import com.yandex.mobile.realty.core.screen.VillageCardScreen
import com.yandex.mobile.realty.core.screen.VillageOffersScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.search.Sorting
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author matek3022 on 2019-11-15.
 */
@RunWith(Parameterized::class)
class VillageOffersSortingTest(
    private val sorting: Sorting,
    private val extraConfiguration: ExtraConfiguration
) {

    private var activityTestRule =
        VillageCardActivityTestRule(villageId = VILLAGE_ID, launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldChangeListWhenSortingSelected() {
        configureWebServer {
            registerVillageCard()
            registerVillageCardOffersSorting(Sorting.PRICE, "villageCardOffersDefaultSorting.json")
            registerVillageCardOffersSorting(sorting, "villageCardOffersSelectedSorting.json")
        }

        activityTestRule.launchActivity()

        onScreen<VillageCardScreen> {
            appBar.collapse()
            apply(extraConfiguration.actionConfiguration)
        }

        onScreen<VillageOffersScreen> {
            waitUntil {
                listView.contains(offerSnippet("0"))
            }
            sortingButton.click()
        }

        onScreen<SortingDialogScreen> {
            listView.scrollTo(sorting.matcher()).click()
        }

        onScreen<VillageOffersScreen> {
            waitUntil {
                listView.contains(offerSnippet("1"))
            }
        }
    }

    private fun DispatcherRegistry.registerVillageCard() {
        register(
            request {
                path("2.0/village/$VILLAGE_ID/card")
            },
            response {
                assetBody("villageCard.json")
            }
        )
    }

    private fun DispatcherRegistry.registerVillageCardOffersSorting(
        sorting: Sorting,
        jsonName: String
    ) {
        register(
            request {
                path("2.0/village/$VILLAGE_ID/offers")
                queryParam("sort", sorting.value)
                queryParam("page", "0")
                extraConfiguration.villageOffersQueryParams.forEach { (name, value) ->
                    queryParam(name, value)
                }
            },
            response {
                assetBody(jsonName)
            }
        )
    }

    companion object {

        private const val VILLAGE_ID = "0"

        @JvmStatic
        @Parameterized.Parameters(name = "{1} offers sort by {0}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(Sorting.PRICE, houseConfiguration),
                arrayOf(Sorting.PRICE_DESC, houseConfiguration),
                arrayOf(Sorting.HOUSE_AREA, houseConfiguration),
                arrayOf(Sorting.HOUSE_AREA_DESC, houseConfiguration),
                arrayOf(Sorting.LAND_AREA, houseConfiguration),
                arrayOf(Sorting.LAND_AREA_DESC, houseConfiguration),
                arrayOf(Sorting.PRICE, landConfiguration),
                arrayOf(Sorting.PRICE_DESC, landConfiguration),
                arrayOf(Sorting.LAND_AREA, landConfiguration),
                arrayOf(Sorting.LAND_AREA_DESC, landConfiguration)
            )
        }

        private val houseConfiguration = object : ExtraConfiguration {

            override val villageOffersQueryParams: Map<String, String> = mapOf(
                "villageOfferType" to "TOWNHOUSE",
                "primarySale" to "NO",
                "villageOfferType" to "LAND",
                "villageOfferType" to "COTTAGE"
            )

            override val actionConfiguration: VillageCardScreen.() -> Unit = {
                waitUntil {
                    listView.contains(agentAndOtherOffersButtonItem)
                }
                agentAndOtherOffersButtonItem.view.click()
            }

            override fun toString(): String {
                return "HOUSE"
            }
        }

        private val landConfiguration = object : ExtraConfiguration {

            override val villageOffersQueryParams: Map<String, String> = mapOf(
                "primarySale" to "YES",
                "villageOfferType" to "LAND"
            )

            override val actionConfiguration: VillageCardScreen.() -> Unit = {
                waitUntil {
                    listView.contains(landOffersButtonItem)
                }
                landOffersButtonItem.view.click()
            }

            override fun toString(): String {
                return "LAND"
            }
        }

        interface ExtraConfiguration {
            val villageOffersQueryParams: Map<String, String>
            val actionConfiguration: VillageCardScreen.() -> Unit
        }
    }
}
