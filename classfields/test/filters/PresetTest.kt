package com.yandex.mobile.realty.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AllSuggestScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
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
 * @author scrooge on 22.03.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PresetTest {

    private var activityTestRule = FilterActivityTestRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldActivateRentTwoRoomsPreset() {
        onScreen<FiltersScreen> {
            promoListView.scrollTo(rentTwoRoomPresetItem).click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            isRentSelected()
            rentTimeSelectorLong.isChecked()
            roomsCountSelectorTwo.isChecked()
        }
    }

    @Test
    fun shouldActivateBuySitePreset() {
        onScreen<FiltersScreen> {
            promoListView.scrollTo(buySitePresetItem).click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            apartmentCategorySelectorNew.isChecked()
            priceValue.isTextEquals("от 1,4 млн \u20BD")
        }
    }

    @Test
    fun shouldActivateBuyOneRoomPreset() {
        onScreen<FiltersScreen> {
            promoListView.scrollTo(buyOneRoomPresetItem).click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            isBuySelected()
            apartmentCategorySelectorAny.isChecked()
            roomsCountSelectorOne.isChecked()
        }
    }

    @Test
    fun shouldActivateYandexRentPreset() {
        onScreen<FiltersScreen> {
            promoListView.scrollTo(yandexRentPresetItem).click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            isRentSelected()
            isApartmentSelected()
            rentTimeSelectorLong.isChecked()
            listView.scrollTo(yandexRentField)
            yandexRentValue.isChecked()
        }
    }

    @Test
    fun shouldNotShowYandexRentPresetWhenRegionNotAllowIt() {
        configureWebServer {
            listGeoSuggest()
            regionInfo()
        }

        onScreen<FiltersScreen> {
            promoListView.contains(yandexRentPresetItem)
            listView.scrollTo(geoSuggestField).click()
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

        onScreen<FiltersScreen> {
            promoListView.doesNotContain(yandexRentPresetItem)
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
