package ru.auto.ara.test.filters

import org.junit.Rule
import org.junit.Test
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.search_offers.OfferLocatorCountersResponse
import ru.auto.ara.core.dispatchers.search_offers.PostEquipmentFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.getOfferLocatorCounters
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.robot.filters.checkMark
import ru.auto.ara.core.robot.filters.checkMultiGeneration
import ru.auto.ara.core.robot.filters.performGeoRadius
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performModel
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.offercard.performGroupOfferCard
import ru.auto.ara.core.robot.search_filter.checkSearchFilter
import ru.auto.ara.core.robot.search_filter.performSearchFilter
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

class ExtendedCounterTest {

    private val webServerRule = WebServerRule {
        getOfferLocatorCounters(OfferLocatorCountersResponse.GEO_RADIUS_BUBBLES)
        postSearchOffers(assetPath = "listing_offers/bmw_x3.json")
        delegateDispatchers(
            PostEquipmentFiltersDispatcher(filePath = "filters/equipment_filters_bmw_x3.json")
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(webServerRule)

    @Test
    fun shouldShowExtendedCounterWhenNewCarsFilterScreenOpenedSelected() {
        val rule = lazyActivityScenarioRule<DeeplinkActivity>()
        rule.launchDeepLinkActivity("https://auto.ru/cars/new/group/bmw/x3/21029738/21184790/")

        performGroupOfferCard { clickOnFilterChip("Параметры") }

        val regionName = "Регион"
        performSearchFilter {
            clickTextField(regionName)
        }

        performMultiGeo {
            clickCheckBoxWithGeoTitle("Москва и Московская область")
            clickAcceptButton()
        }

        checkSearchFilter {
            isShowOffersButtonScreenshotTheSame(PATH_SHOW_NEW_OFFERS_BUTTON)
        }
    }

    @Test
    fun shouldShowExtendedCountWhenCarsFilterSelected() {
        startMain()
        performMain {
            openFilters()
        }
        selectCityAndRadius()
        checkFilter { isDoSearchButtonScreenshotSame(PATH_SHOW_OFFERS_BUTTON) }
    }

    @Test
    fun shouldShowExtendedCountWhenCommercialFilterSelected() {
        startMain()
        performMain {
            openFilters()
        }
        performFilter { interactions.onToggleButton("Комтранс").performClick() }
        selectCityAndRadius()
        checkFilter { isDoSearchButtonScreenshotSame(PATH_SHOW_OFFERS_BUTTON) }
    }

    @Test
    fun shouldShowExtendedCountWhenMotoFilterSelected() {
        startMain()
        performMain {
            openFilters()
        }
        performFilter { interactions.onToggleButton("Мото").performClick() }
        selectCityAndRadius()
        checkFilter { isDoSearchButtonScreenshotSame(PATH_SHOW_OFFERS_BUTTON) }
    }

    @Test
    fun shouldShowExtendedCountForMultigenerationSelect() {
        startMain()

        performMain { openFilters() }
        selectCityAndRadius()

        performFilter {
            chooseMarkModel()
        }
        performMark {
            selectMark("BMW")
        }
        performModel {
            tapOnCheckbox("3 серия")
            tapOnAcceptButton()
        }
        performFilter {
            clickOnEmptyGenerationField()
        }
        checkMultiGeneration { isDoSearchButtonScreenshotSame(PATH_SHOW_OFFERS_BUTTON) }
    }

    @Test
    fun shouldShowExtendedCountForMultigenerationSelectFromMain() {
        startMain()

        performMain { openFilters() }
        selectCityAndRadius()

        performFilter { doSearch() }
        checkSearchFeed {
            isFabDisplayed()
        }
        performSearchFeed { clickEmptyMMNGField() }

        checkMark { isShowResultsButtonScreenshotSame(PATH_SHOW_OFFERS_BUTTON) }
    }

    private fun startMain() {
        val rule = lazyActivityScenarioRule<MainActivity>()
        rule.launchActivity()
    }

    private fun selectCityAndRadius() {
        performFilter { clickRegionFilter() }
        performMultiGeo {
            waitGeoListDisplayed()
            clickExpandArrowWithGeoTitle("Москва и Московская область")
            clickCheckBoxWithGeoTitle("Москва")
            clickAcceptButton()
        }
        performFilter { clickGeoRadiusField() }
        performGeoRadius { clickRadius("200 км") }
    }

    companion object {
        private const val PATH_SHOW_OFFERS_BUTTON = "search_filter/extended_counter/show_offers_button_with_extended_counter.png"
        private const val PATH_SHOW_NEW_OFFERS_BUTTON = "extended_counter/show_offers_button_with_extended_counter_for_new"
    }
}
