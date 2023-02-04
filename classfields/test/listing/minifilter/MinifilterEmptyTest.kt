package ru.auto.ara.test.listing.minifilter

import org.junit.Test
import ru.auto.ara.R
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.data.models.FormState
import ru.auto.data.model.filter.StateGroup

/**
 * @author themishkun on 26/03/2019.
 */
class MinifilterEmptyTest : BaseMinifilterListingTest(FormState.withDefaultCategory()) {
    @Test
    fun shouldShowEmptyMMGRegionAndDefaultState() {
        performSearchFeed().checkResult {
            isStateSelectorChecked(StateGroup.ALL)
            isEmptyMMNGFilterDisplayed()
            isRegionWithText(getResourceString(R.string.any_region))
        }
        watcher.checkNotRequestBodyParameters(listOf(RID_REQUEST_PARAM, GEO_RADIUS_REQUEST_PARAM))
    }

    @Test
    fun shouldShowGeoDialogOnClick() {
        performSearchFeed {
            clickGeo()
        }
        performMultiGeo().checkResult {
            isGeoScreen()
        }
    }
}
