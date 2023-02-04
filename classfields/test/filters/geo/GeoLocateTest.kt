package ru.auto.ara.test.filters.geo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.geo.GeoSuggestDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.OfferLocatorCountersResponse
import ru.auto.ara.core.dispatchers.search_offers.getOfferLocatorCounters
import ru.auto.ara.core.robot.filters.checkMultiGeo
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

@RunWith(AndroidJUnit4::class)
class GeoLocateTest {
    private val GEO_RADIUS_REQUEST_PARAM = "geo_radius"
    private val RID_REQUEST_PARAM = "rid"
    private val geoSuggestWatcher = RequestWatcher()
    private val countWatcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher("cars", countWatcher),
        GeoSuggestDispatcher("ufa", geoSuggestWatcher)
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

    @Test
    fun shouldSeeCorrectRegionAfterConfirm() {
        webServerRule.routing {
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
                .watch {
                    checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("172"))
                    checkRequestBodyParameter(GEO_RADIUS_REQUEST_PARAM, "200")
                }
        }
        performMultiGeo {
            waitGeoListDisplayed()
            clickLocateIcon()
        }
        checkMultiGeo {
            isLocateMessageDisplayed("Ваш регион: Уфа. Продолжить поиск в нём и сбросить выбранные регионы?")
            isLocateDialogConfirmButtonDisplayed()
            isLocateDialogDeclineButtonDisplayed()
        }
        performMultiGeo { clickLocateConfirmButton() }
        geoSuggestWatcher.checkQueryParameter("only_cities", "false")
        checkFilter { isRegion("Уфа") }
    }

    @Test
    fun shouldNotApplyNewRegionAfterDecline() {
        performMultiGeo {
            clickLocateIcon()
            clickLocateDeclineButton()
        }.checkResult { isGeoScreen() }
        countWatcher.checkNotRequestBodyParameter(RID_REQUEST_PARAM)
    }

    @Test
    fun shouldClearSelectedGeoAfterConfirm() {
        performMultiGeo {
            clickCheckBoxWithIndex(10)
            clickAcceptButton()
        }
        performFilter { clickRegionFilter() }
        performMultiGeo {
            clickLocateIcon()
            clickLocateConfirmButton()
        }
        performFilter { clickRegionFilter() }
        checkMultiGeo { isGeoItemWithTextNotChecked("Алтайский край") }
        countWatcher.checkRequestBodyExactlyArrayParameter(RID_REQUEST_PARAM, setOf("172"))
        countWatcher.checkRequestBodyParameter(GEO_RADIUS_REQUEST_PARAM, "200")
    }
}
