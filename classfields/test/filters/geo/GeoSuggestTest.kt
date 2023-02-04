package ru.auto.ara.test.filters.geo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.geo.GeoSuggestDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.OfferLocatorCountersResponse
import ru.auto.ara.core.dispatchers.search_offers.getOfferLocatorCounters
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

class GeoSuggestControlsTest : GeoSuggestTest("moscow") {
    @Test
    fun shouldSeeControlsOnGeoSuggestScreen() {
        performMultiGeo { clickSearchIcon() }.checkResult {
            isLocateIconDisplayed()
            isSearchLineDisplayed(R.string.town)
        }
    }
}

class GeoSuggestInputTextTest : GeoSuggestTest("amurskaya_oblast", RequestWatcher()) {
    @Test
    fun shouldSeeGeoSuggest() {
        val cityName = "Амурск"
        performMultiGeo {
            waitGeoListDisplayed()
            clickSearchIcon()
            typeTextToSearchLine(cityName)
        }.checkResult {
            isSearchLineDisplayed(cityName)
            isGeoTitleDisplayed("Амурская область")
            isGeoTitleDisplayed(cityName)
            isGeoSubtitleDisplayed("Хабаровский край")
            isClearSearchLineButtonDisplayed()
        }
        geoSuggestWatcher?.checkQueryParameters(
            listOf(
                "only_cities" to "false",
                "letters" to cityName
            )
        )
    }
}

class GeoSuggestApplyRegionTest : GeoSuggestTest("amurskaya_oblast", null, RequestWatcher()) {
    @Test
    fun shouldApplyRegionFromGeoSuggest() {
        performMultiGeo {
            waitGeoListDisplayed()
            clickSearchIcon()
            typeTextToSearchLine("Амурск")
            clickRegionTitle("Амурская область")
        }.checkResult {
            isGeoItemWithIndexChecked(1)
            isGeoItemWithIndexChecked(12)
        }
        countWatcher?.checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("11375"))
        countWatcher?.checkNotRequestBodyParameter(GEO_RADIUS_REQUEST_PARAM)
    }
}

class GeoSuggestRecentSearchesTest : GeoSuggestTest("amurskaya_oblast") {
    @Test
    fun shouldRecentSearches() {
        val regionName = "Амурская область"
        performMultiGeo {
            waitGeoListDisplayed()
            clickSearchIcon()
            typeTextToSearchLine("Амурск")
            clickRegionTitle(regionName)
            clickSearchIcon()
        }.checkResult {
            isIconWithIndexDisplayed(0)
            isGeoTitleDisplayed(regionName)
        }
    }
}

class GeoSuggestLocateButtonTest : GeoSuggestTest("ufa", RequestWatcher(), RequestWatcher()) {
    @Test
    fun shouldSeeCorrectRegionAfterConfirmFromLocateButton() {
        performMultiGeo {
            waitGeoListDisplayed()
            clickSearchIcon()
            clickLocateIcon()
        }.checkResult {
            isLocateMessageDisplayed("Ваш регион: Уфа. Продолжить поиск в нём и сбросить выбранные регионы?")
            isLocateDialogConfirmButtonDisplayed()
            isLocateDialogDeclineButtonDisplayed()
        }
        webServerRule.routing {
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
                .watch {
                    checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("172"))
                }
        }
        performMultiGeo { clickLocateConfirmButton() }
        geoSuggestWatcher?.checkQueryParameter("only_cities", "false")
        checkFilter { isRegion("Уфа") }
    }
}

@RunWith(AndroidJUnit4::class)
abstract class GeoSuggestTest(
    regionName: String,
    val geoSuggestWatcher: RequestWatcher? = null,
    val countWatcher: RequestWatcher? = null
) {
    val GEO_RADIUS_REQUEST_PARAM = "geo_radius"
    val RID_REQUEST_PARAM = "rid"

    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", countWatcher),
        GeoSuggestDispatcher(regionName, geoSuggestWatcher)
    )

    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    val webServerRule = WebServerRule {
        delegateDispatchers(dispatchers)
    }

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        GrantPermissionsRule(),
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter { clickRegionFilter() }
    }
}
