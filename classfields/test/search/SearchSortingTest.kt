package com.yandex.mobile.realty.test.search

import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.robot.FiltersRobot
import com.yandex.mobile.realty.core.robot.performOnCommercialTypeScreen
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.robot.performOnSortingDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.filters.CommercialType
import com.yandex.mobile.realty.test.filters.DealType
import com.yandex.mobile.realty.test.filters.OfferCategory
import com.yandex.mobile.realty.test.filters.PropertyType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

/**
 * @author misha-kozlov on 2019-09-13
 */
@RunWith(Parameterized::class)
class SearchSortingTest(
    private val dealType: DealType,
    private val propertyType: PropertyType,
    private val sortingOptions: EnumSet<Sorting>,
    private val sorting: Sorting,
    private val extraConfiguration: ExtraConfiguration
) {

    private var activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule
    )

    @Test
    fun shouldChangeSearchListWhenSortingSelected() {
        configureWebServer {
            registerSearchWithSorting(Sorting.RELEVANCE, "offerWithSiteSearchDefaultSorting.json")
            registerSearchWithSorting(sorting, "offerWithSiteSearchSelectedSorting.json")
        }

        activityTestRule.launchActivity()

        performOnFiltersScreen {
            tapOn(lookup.matchesDealTypeSelector())
            tapOn(dealType.matcher.invoke(lookup))
            tapOn(lookup.matchesPropertyTypeSelector())
            tapOn(propertyType.matcher.invoke(lookup))
            apply(extraConfiguration.actionConfiguration)
            tapOn(lookup.matchesSubmitButton())
        }

        onScreen<SearchListScreen> {
            sortingItem
                .waitUntil { listView.contains(this) }
                .click()

            performOnSortingDialog {
                isAllItemsExists(sortingOptions)
                scrollToPosition(sorting.matcher.invoke()).tapOn()
            }

            sortingItem.view.waitUntil { labelView.isTextEquals(sorting.expected) }

            offerSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerSearchWithSorting(
        sorting: Sorting,
        responseFileName: String
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("sort", sorting.value)
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1} {4} sort by {3}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    Sorting.apartmentValues,
                    Sorting.RELEVANCE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    Sorting.apartmentValues,
                    Sorting.FLOOR,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    Sorting.apartmentValues,
                    Sorting.FLOOR_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.APARTMENT,
                    Sorting.apartmentRentLongValues,
                    Sorting.RELEVANCE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.APARTMENT,
                    Sorting.apartmentRentLongValues,
                    Sorting.FLOOR,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.APARTMENT,
                    Sorting.apartmentRentLongValues,
                    Sorting.FLOOR_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.APARTMENT,
                    Sorting.apartmentRentLongValues,
                    Sorting.CONFIDENCE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.APARTMENT,
                    Sorting.apartmentValues,
                    Sorting.PRICE,
                    rentShortConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.COMMERCIAL,
                    Sorting.commercialLandValues,
                    Sorting.DATE_DESC,
                    commercialLandConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.COMMERCIAL,
                    Sorting.commercialLandValues,
                    Sorting.DATE_DESC,
                    commercialLandConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.COMMERCIAL,
                    Sorting.commercialValues,
                    Sorting.PRICE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.COMMERCIAL,
                    Sorting.commercialValues,
                    Sorting.PRICE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.GARAGE,
                    Sorting.commercialValues,
                    Sorting.PRICE_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.GARAGE,
                    Sorting.commercialValues,
                    Sorting.PRICE_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.HOUSE,
                    Sorting.houseValues,
                    Sorting.AREA,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.HOUSE,
                    Sorting.houseValues,
                    Sorting.AREA_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.HOUSE,
                    Sorting.houseValues,
                    Sorting.AREA,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.HOUSE,
                    Sorting.houseValues,
                    Sorting.AREA_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.LOT,
                    Sorting.lotValues,
                    Sorting.LOT_AREA,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.LOT,
                    Sorting.lotValues,
                    Sorting.LOT_AREA_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.ROOM,
                    Sorting.roomValues,
                    Sorting.LIVING_SPACE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.ROOM,
                    Sorting.roomValues,
                    Sorting.LIVING_SPACE_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.ROOM,
                    Sorting.roomValues,
                    Sorting.LIVING_SPACE,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.ROOM,
                    Sorting.roomValues,
                    Sorting.LIVING_SPACE_DESC,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    Sorting.siteValues,
                    Sorting.PRICE_PER_SQUARE,
                    siteConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    Sorting.siteValues,
                    Sorting.PRICE_PER_SQUARE_DESC,
                    siteConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.HOUSE,
                    Sorting.villageValues,
                    Sorting.COMMISSIONING_DATE,
                    villageConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.HOUSE,
                    Sorting.villageValues,
                    Sorting.COMMISSIONING_DATE_DESC,
                    villageConfiguration
                )
            )
        }

        private val defaultConfiguration = object : ExtraConfiguration {
            override val actionConfiguration: FiltersRobot.() -> Unit = {}

            override fun toString(): String {
                return ""
            }
        }

        private val rentShortConfiguration = object : ExtraConfiguration {
            override val actionConfiguration: FiltersRobot.() -> Unit = {
                tapOn(lookup.matchesRentTimeSelectorShort())
            }

            override fun toString(): String {
                return "SHORT"
            }
        }

        private val commercialLandConfiguration = object : ExtraConfiguration {
            override val actionConfiguration: FiltersRobot.() -> Unit = {
                tapOn(lookup.matchesFieldCommercialType())
                performOnCommercialTypeScreen {
                    scrollTo(CommercialType.LAND.matcher.invoke(lookup)).tapOn()
                    tapOn(lookup.matchesApplyButton())
                }
            }

            override fun toString(): String {
                return "LAND"
            }
        }

        private val siteConfiguration = object : ExtraConfiguration {
            override val actionConfiguration: FiltersRobot.() -> Unit = {
                val category = OfferCategory.PRIMARY.invoke(PropertyType.APARTMENT)
                tapOn(category.matcher.invoke(lookup))
            }

            override fun toString(): String {
                return "SITE"
            }
        }

        private val villageConfiguration = object : ExtraConfiguration {
            override val actionConfiguration: FiltersRobot.() -> Unit = {
                val category = OfferCategory.PRIMARY.invoke(PropertyType.HOUSE)
                tapOn(category.matcher.invoke(lookup))
            }

            override fun toString(): String {
                return "VILLAGE"
            }
        }
    }

    interface ExtraConfiguration {
        val actionConfiguration: FiltersRobot.() -> Unit
    }
}
