package com.yandex.mobile.realty.test.filters

import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.doesNotExist
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AllSuggestScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 16/02/2021.
 */
class YandexRentNegativeTest {

    private val activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldNotShowYandexRentWhenRentShortApartment() {
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            tapOn(lookup.matchesDealTypeSelector())
            tapOn(DealType.RENT.matcher.invoke(lookup))
            tapOn(lookup.matchesPropertyTypeSelector())
            tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
            tapOn(lookup.matchesRentTimeSelectorShort())

            scrollToPosition(lookup.matchesYandexRentField()).check(doesNotExist())
        }
    }

    @Test
    fun shouldNotShowYandexRentWhenRentLongRoom() {
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            tapOn(lookup.matchesDealTypeSelector())
            tapOn(DealType.RENT.matcher.invoke(lookup))
            tapOn(lookup.matchesPropertyTypeSelector())
            tapOn(PropertyType.ROOM.matcher.invoke(lookup))
            tapOn(lookup.matchesRentTimeSelectorLong())

            scrollToPosition(lookup.matchesYandexRentField()).check(doesNotExist())
        }
    }

    @Test
    fun shouldNotShowYandexRentWhenRegionNotAllowIt() {
        configureWebServer {
            listGeoSuggest()
            regionInfo()
        }

        activityTestRule.launchActivity()

        performOnFiltersScreen {
            tapOn(lookup.matchesDealTypeSelector())
            tapOn(DealType.RENT.matcher.invoke(lookup))
            tapOn(lookup.matchesPropertyTypeSelector())
            tapOn(PropertyType.APARTMENT.matcher.invoke(lookup))
            tapOn(lookup.matchesRentTimeSelectorLong())

            scrollToPosition(lookup.matchesYandexRentField())
                .check(matches(isCompletelyDisplayed()))

            scrollToPosition(lookup.matchesGeoSuggestField()).tapOn()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("Saint-Petersburg")

            onScreen<AllSuggestScreen> {
                geoSuggestItem("город Санкт-Петербург")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .click()
            }

            geoObjectView("город Санкт-Петербург").waitUntil { isCompletelyDisplayed() }
            pressBack()
        }

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesYandexRentField()).check(doesNotExist())
        }
    }

    private fun DispatcherRegistry.listGeoSuggest() {
        register(
            request {
                path("1.0/geosuggest.json")
                queryParam("text", "Saint-Petersburg")
            },
            response {
                assetBody("geoSuggestSaintPetersburg.json")
            }
        )
    }

    private fun DispatcherRegistry.regionInfo() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", "417899")
            },
            response {
                assetBody("regionInfo417899.json")
            }
        )
    }
}
