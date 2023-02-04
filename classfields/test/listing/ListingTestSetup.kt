package ru.auto.ara.test.listing

import ru.auto.ara.core.TestGeoRepository
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.BasicRegion
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.model.geo.SuggestGeoItem

open class ListingTestSetup {

    protected val activityRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()

    protected val geoArgs = TestGeoRepository.GeoArgs.moscow300(geoRadiusSupport = true)

    protected fun openCarsSearchFeed(
        stateGroup: StateGroup,
        geoRadiusSupport: Boolean = true,
        vararg regions: BasicRegion = arrayOf(MOSCOW)
    ) {
        openSearchFeed(
            stateGroup = stateGroup,
            category = VehicleCategory.CARS,
            geoRadiusSupport = geoRadiusSupport,
            regions = regions.toList()
        )
    }

    protected fun openMotoSearchFeed() {
        openSearchFeed(stateGroup = StateGroup.ALL, category = VehicleCategory.MOTO)
    }

    protected fun openTrucksSearchFeed() {
        openSearchFeed(stateGroup = StateGroup.ALL, category = VehicleCategory.TRUCKS)
    }

    private fun openSearchFeed(
        category: VehicleCategory,
        stateGroup: StateGroup,
        geoRadius: Int = DEFAULT_GEO_RADIUS,
        regions: List<BasicRegion> = listOf(MOSCOW),
        geoRadiusSupport: Boolean = true
    ) {
        geoArgs.regions = createSuggestGeoItems(geoRadiusSupport, regions)
        geoArgs.radius = geoRadius

        activityRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragmentBundles.searchFeedBundle(
                category = category,
                stateGroup = stateGroup,
                geoRadius = geoRadius,
                regions = regions,
                geoRadiusSupport = geoRadiusSupport
            )
        )

        performSearchFeed {
            waitSearchFeed()
        }
    }

    private fun createSuggestGeoItems(geoRadiusSupport: Boolean, regions: List<BasicRegion>) =
        regions.map { region ->
            SuggestGeoItem(id = region.id, region.name, geoRadiusSupport = geoRadiusSupport)
        }


    companion object {
        private const val DEFAULT_GEO_RADIUS = 200
        val MOSCOW = BasicRegion("213", "Москва")
        val MOSCOW_AREA = BasicRegion(id = "1", name = "Москва и Московская область")
        val SAINT_PETERSBURG = BasicRegion(id = "2", name = "Санкт-Петербург")
    }

}
