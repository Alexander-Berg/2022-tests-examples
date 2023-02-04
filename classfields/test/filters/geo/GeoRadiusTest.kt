package ru.auto.ara.test.filters.geo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.OfferLocatorCountersResponse
import ru.auto.ara.core.dispatchers.search_offers.getOfferLocatorCounters
import ru.auto.ara.core.robot.filters.checkGeoRadius
import ru.auto.ara.core.robot.filters.performGeoRadius
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.DEFAULT_GEO_RADIUSES
import ru.auto.ara.core.utils.waitSomething
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GeoRadiusTest {
    private val GEO_RADIUS_PARAM_NAME = "geo_radius"
    private val watcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("cars", watcher)
        )
    }
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
        performMain { openFilters() }
        performFilter { clickRegionFilter() }
    }

    @Test
    fun shouldSeeGeoRadiusScreenControls() {
        performMultiGeo {
            clickExpandArrowWithGeoTitle("Санкт-Петербург и Ленинградская область")
            clickRegionTitle("Санкт-Петербург")
            clickAcceptButton()
        }
        performFilter { clickGeoRadiusField() }
        checkGeoRadius {
            isTitleDisplayed()
            isCloseIconDisplayed()
            DEFAULT_GEO_RADIUSES.map { isRadiusDisplayed(it) }
        }
    }

    @Test
    fun shouldSeeDefaultRadiusForSelectedCity() {
        webServerRule.routing {
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
                .watch {
                    checkRequestBodyParameter(GEO_RADIUS_PARAM_NAME, "200")
                }
        }
        performMultiGeo {
            clickExpandArrowWithGeoTitle("Санкт-Петербург и Ленинградская область")
            clickRegionTitle("Санкт-Петербург")
            clickAcceptButton()
        }
        performFilter { clickGeoRadiusField() }
        checkGeoRadius { isRadiusChecked("200 км"); }
    }

    @Test
    fun shouldSeeNoRadiusForRegion() {
        performMultiGeo {
            clickCheckBoxWithIndex(8)
            clickAcceptButton()
        }
        checkFilter {
            isRadiusNotShown()
        }
        watcher.checkNotQueryParameter(GEO_RADIUS_PARAM_NAME)
    }

    @Test
    fun shouldSeeNoRadiusForMultiRegion() {
        val indexRegionFirst = 6
        val indexRegionSecond = 8
        performMultiGeo {
            clickCheckBoxWithIndex(indexRegionFirst)
            clickCheckBoxWithIndex(indexRegionSecond)
            clickAcceptButton()
        }
        checkFilter {
            isRadiusNotShown()
        }
        watcher.checkNotQueryParameter("geo_radius")
    }

    @Test
    fun shouldSeeNoRadiusForMultiCity() {
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
            isRadiusNotShown()
        }
        watcher.checkNotQueryParameter("geo_radius")
    }

    @Test
    fun shouldApplyNewDefaultRadius() {
        val cityFirst = "Санкт-Петербург"
        val citySecond = "Омск"
        val regionFirst = "Санкт-Петербург и Ленинградская область"
        val regionSecond = "Омская область"
        performMultiGeo {
            clickExpandArrowWithGeoTitle(regionFirst)
            clickRegionTitle(cityFirst)
            clickAcceptButton()
        }
        performFilter { clickRegionFilter() }
        webServerRule.routing {
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
                .watch {
                    checkRequestBodyParameter(GEO_RADIUS_PARAM_NAME, "500")
                }
        }
        performMultiGeo {
            clickClearButton()
            scrollToGeoItem(regionSecond)
            clickExpandArrowWithGeoTitle(regionSecond)
            scrollToGeoItem(citySecond)
            clickCheckBoxWithGeoTitle(citySecond)
            clickAcceptButton()
        }
        checkFilter { isRadius("+ 500 км") }
        performFilter { clickGeoRadiusField() }
        checkGeoRadius { isRadiusChecked("500 км"); }
    }

    @Test
    fun shouldApplyNewDefaultRadiusWhenClearRegions() {
        val cityFirst = "Санкт-Петербург"
        val citySecond = "Омск"
        val regionFirst = "Санкт-Петербург и Ленинградская область"
        val regionSecond = "Омская область"
        performMultiGeo {
            clickExpandArrowWithGeoTitle(regionFirst)
            clickRegionTitle(cityFirst)
            clickAcceptButton()
        }
        performFilter { clickGeoRadiusField() }
        performGeoRadius { clickRadius("1 000 км") }
        webServerRule.routing {
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
                .watch {
                    checkRequestBodyParameter(GEO_RADIUS_PARAM_NAME, "500")
                }
        }
        performFilter { clickRegionFilter() }
        performMultiGeo {
            clickClearButton()
            scrollToGeoItem(regionSecond)
            clickExpandArrowWithGeoTitle(regionSecond)
            scrollToGeoItem(citySecond)
            clickCheckBoxWithGeoTitle(citySecond)
            clickAcceptButton()
        }
        checkFilter { isRadius("+ 500 км") }
        performFilter { clickGeoRadiusField() }
        checkGeoRadius { isRadiusChecked("500 км"); }
    }

    @Test
    fun shouldNotApplyNewDefaultRadiusWhenUncheckRegion() {
        val cityFirst = "Санкт-Петербург"
        val citySecond = "Омск"
        val regionFirst = "Санкт-Петербург и Ленинградская область"
        val regionSecond = "Омская область"
        performMultiGeo {
            clickExpandArrowWithGeoTitle(regionFirst)
            clickRegionTitle(cityFirst)
            clickAcceptButton()
        }
        performFilter { clickGeoRadiusField() }
        performGeoRadius { clickRadius("1 000 км") }
        webServerRule.routing {
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
                .watch {
                    checkRequestBodyParameter(GEO_RADIUS_PARAM_NAME, "1000")
                }
        }
        performFilter { clickRegionFilter() }
        performMultiGeo {
            clickExpandArrowWithIndex(1)
            clickRegionTitle(cityFirst)
            scrollToGeoItem(regionSecond)
            clickExpandArrowWithGeoTitle(regionSecond)
            scrollToGeoItem(citySecond)
            clickCheckBoxWithGeoTitle(citySecond)
            clickAcceptButton()
        }
        checkFilter { isRadius("+ 1000 км") }
        performFilter { clickGeoRadiusField() }
        checkGeoRadius { isRadiusChecked("1 000 км"); }
    }
}
