package ru.auto.ara.test.listing.minifilter

import org.junit.Test
import ru.auto.ara.R
import ru.auto.ara.core.robot.filters.checkMultiGeo
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.testdata.DEFAULT_COMMON_PARAMS
import ru.auto.ara.core.testdata.SOME_RADIUS
import ru.auto.ara.core.testdata.SOME_REGION
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.ui.helpers.form.util.VehicleSearchToFormStateConverter
import ru.auto.data.model.BasicRegion
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch

class MinifilterGeoWithoutRadiusTest : BaseMinifilterListingTest(
    VehicleSearchToFormStateConverter.convert(
        CarSearch(
            carParams = CarParams(),
            commonParams = DEFAULT_COMMON_PARAMS.copy(
                regions = listOf(SOME_REGION)
            )
        )
    )
) {
    @Test
    fun shouldShowSelectedRegionName() {
        performSearchFeed().checkResult {
            isRegionWithText(SOME_REGION.name)
        }
        watcher.checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("39"))
        watcher.checkNotRequestBodyParameter(GEO_RADIUS_REQUEST_PARAM)
    }
}

class MinifilterGeoSelectTest : BaseMinifilterListingTest(
    VehicleSearchToFormStateConverter.convert(
        CarSearch(
            carParams = CarParams(),
            commonParams = DEFAULT_COMMON_PARAMS.copy(
                regions = listOf(BasicRegion(id = "2", name = "Санкт-Петербург"))
            )
        )
    )
) {
    @Test
    fun shouldShowSelectedRegionName() {
        performSearchFeed {
            clickGeo()
        }
        performMultiGeo {
            clickCheckBoxWithGeoTitle("Москва и Московская область")
            clickAcceptButton()
        }
        performSearchFeed {
            clickGeo()
        }
        checkMultiGeo {
            checkGeoItemHasJustTitle(1, "Москва и Московская область")
            isSelectedRegionAtIndexSubtitle(3, "Санкт-Петербург и Ленинградская область", "Санкт-Петербург")
        }
    }
}

class MinifilterGeoDeselectTest : BaseMinifilterListingTest(
    VehicleSearchToFormStateConverter.convert(
        CarSearch(
            carParams = CarParams(),
            commonParams = DEFAULT_COMMON_PARAMS.copy(
                regions = listOf(
                    BasicRegion(id = "2", name = "Санкт-Петербург"),
                    BasicRegion(id = "1", name = "Москва и Московская область")
                )
            )
        )
    )
) {
    @Test
    fun shouldShowSelectedRegionName() {
        performSearchFeed {
            clickGeo()
        }
        performMultiGeo {
            clickCheckBoxWithIndex(1)
            clickAcceptButton()
        }
        performSearchFeed {
            clickGeo()
        }
        checkMultiGeo {
            isSelectedRegionAtIndexSubtitle(1, "Санкт-Петербург и Ленинградская область", "Санкт-Петербург")
        }
    }
}

class MinifilterGeoWithRadiusTest : BaseMinifilterListingTest(
    VehicleSearchToFormStateConverter.convert(
        CarSearch(
            carParams = CarParams(),
            commonParams = DEFAULT_COMMON_PARAMS.copy(
                regions = listOf(SOME_REGION),
                geoRadius = SOME_RADIUS,
                geoRadiusSupport = true
            )
        )
    )
) {
    @Test
    fun shouldShowSelectedRegionName() {
        performSearchFeed().checkResult {
            isRegionWithText(SOME_REGION.name, EXPAND_RADIUS)
        }
        watcher.checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("39"))
        watcher.checkRequestBodyParameter(GEO_RADIUS_REQUEST_PARAM, "300")
    }

    companion object {
        private val EXPAND_RADIUS =
            getResourceString(R.string.geo_radius, SOME_RADIUS)
        private const val SOME_OTHER_RADIUS = 500
        private val EXPAND_OTHER_RADIUS =
            getResourceString(R.string.geo_radius, SOME_OTHER_RADIUS)
    }
}
