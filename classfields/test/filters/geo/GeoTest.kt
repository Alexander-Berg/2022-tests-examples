package ru.auto.ara.test.filters.geo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.filters.checkMultiGeo
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GeoTest {
    private val GEO_RADIUS_REQUEST_PARAM = "geo_radius"
    private val RID_REQUEST_PARAM = "rid"

    private val watcher = RequestWatcher()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", watcher)
    )
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter { clickRegionFilter() }
    }

    @Test
    fun shouldSeeMultiGeoScreenControls() {
        checkMultiGeo {
            isLocateIconDisplayed()
            isSearchIconDisplayed()
            isAcceptButtonDisplayed()
            isClearButtonDisplayed()
            isGeoScreen()
            isCounterOfOffersDisplayed("50,826")
        }
    }

    @Test
    fun shouldSelectRussiaRegion() {
        val indexFirstItem = 4
        performMultiGeo { clickRussiaRegion(indexFirstItem) }
        checkFilter {
            isRegion("Россия")
            isRadiusNotShown()
        }
        performFilter { clickRegionFilter() }
        checkMultiGeo {
            isRussiaRegionSelected()
        }
        watcher.checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("225"))
        watcher.checkNotRequestBodyParameter(GEO_RADIUS_REQUEST_PARAM)
    }

    @Test
    fun shouldUncheckRussiaRegionAfterSelectAnotherRegion() {
        val indexFirstItem = 1
        val indexNotRussiaItem = 6
        performMultiGeo { clickRussiaRegion(indexFirstItem) }
        performFilter { clickRegionFilter() }
        performMultiGeo { clickCheckBoxWithIndex(indexNotRussiaItem) }
        checkMultiGeo {
            isRussiaRegionNotSelected()
            isGeoItemWithIndexChecked(indexNotRussiaItem)
        }
    }

    @Test
    fun shouldSelectRegionWithAcceptButton() {
        val index = 10
        val indexFirstItem = 1
        val selectedCitiesCount = "12"
        performMultiGeo {
            clickCheckBoxWithIndex(index)
            clickAcceptButton()
        }
        checkFilter {
            isRegion("Алтайский край")
            isRadiusNotShown()
        }
        performFilter { clickRegionFilter() }
        checkMultiGeo {
            isGeoItemWithIndexChecked(indexFirstItem)
            checkGeoItemHasJustTitle(index = indexFirstItem, titleText = "Алтайский край")
            isSelectedRegionsCountDisplayed(indexFirstItem, selectedCitiesCount)
            isGeoItemWithIndexChecked(index)
        }
    }

    @Test
    fun shouldSelectGeoFromExpandedList() {
        val indexCity = 9
        val indexRegion = 8
        val indexFirstItem = 1
        val selectedCitiesCount = "1"
        val city = "Санкт-Петербург"
        performMultiGeo {
            clickExpandArrowWithGeoTitle("Санкт-Петербург и Ленинградская область")
            clickCheckBoxWithIndex(indexCity)
        }.checkResult {
            isGeoItemWithTextChecked(city)
            isGeoItemWithIndexNotChecked(indexRegion)
            isSelectedRegionsCountDisplayed(indexRegion, selectedCitiesCount)
        }
        performMultiGeo { clickAcceptButton() }
        checkFilter {
            isRegion(city)
            isRadius("+ 200 км")
        }
        performFilter { clickRegionFilter() }
        checkMultiGeo {
            isSelectedRegionsCountDisplayed(indexFirstItem, selectedCitiesCount)
            isSelectedRegionAtIndexSubtitle(
                index = indexFirstItem,
                titleText = "Санкт-Петербург и Ленинградская область",
                subtitleText = "Санкт-Петербург"
            )
            isSelectedRegionsCountDisplayed(indexRegion, selectedCitiesCount)
            isGeoItemNotShown(city)
        }
    }

    @Test
    fun shouldMultiSelectRegions() {
        val indexFirstItem = 1
        val indexSecondItem = 3
        val indexRegionFirst = 8
        val indexRegionSecond = 10
        val indexRegionAfterConfirmFirst = 10
        val indexRegionAfterConfirmSecond = 12
        performMultiGeo {
            clickCheckBoxWithIndex(indexRegionFirst)
            clickCheckBoxWithIndex(indexRegionSecond)
            clickAcceptButton()
        }
        checkFilter {
            isRegion("Алтайский край, Санкт-Петербург и Ленинградская область")
            isRadiusNotShown()
        }
        performFilter { clickRegionFilter() }
        checkMultiGeo {
            isGeoItemWithIndexChecked(indexFirstItem)
            isGeoItemWithIndexChecked(indexSecondItem)
            isGeoItemWithIndexChecked(indexRegionAfterConfirmFirst)
            isGeoItemWithIndexChecked(indexRegionAfterConfirmSecond)
        }
    }

    @Test
    fun shouldMultiSelectRegionsFromExpandedList() {
        val indexFirstItem = 1
        val indexCityFirst = 9
        val indexCitySecond = 10
        performMultiGeo {
            clickExpandArrowWithGeoTitle("Санкт-Петербург и Ленинградская область")
            waitSomething(200, TimeUnit.MILLISECONDS)
            clickCheckBoxWithIndex(indexCityFirst)
            clickCheckBoxWithIndex(indexCitySecond)
            clickAcceptButton()
        }
        checkFilter {
            isRegion("Бокситогорск, Санкт-Петербург")
            isRadiusNotShown()
        }
        performFilter { clickRegionFilter() }
        checkMultiGeo {
            isSelectedRegionAtIndexSubtitle(
                index = indexFirstItem,
                titleText = "Санкт-Петербург и Ленинградская область",
                subtitleText = "Санкт-Петербург, Бокситогорск"
            )
        }
        performMultiGeo { clickExpandArrowWithIndex(indexFirstItem) }
        checkMultiGeo {
            isGeoItemWithIndexChecked(2)
            isGeoItemWithIndexChecked(3)
        }
        watcher.checkRequestBodyArrayParameter(RID_REQUEST_PARAM, setOf("2"))
        watcher.checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("10861", "2"))
        watcher.checkRequestBodyArrayNotParameter(RID_REQUEST_PARAM, setOf("1"))
    }

    @Test
    fun shouldSeeDefaultRegionRadius() {
        val city = "Омск"
        val region = "Омская область"
        checkMultiGeo { waitRussiaRegionWithIndexDisplayed(1) }
        performMultiGeo {
            scrollToGeoItem(region)
            clickExpandArrowWithGeoTitle(region)
            scrollToGeoItem(city)
            clickCheckBoxWithGeoTitle(city)
            clickAcceptButton()
        }
        checkFilter {
            isRegion(city)
            isRadius("+ 500 км")
        }
    }
}
