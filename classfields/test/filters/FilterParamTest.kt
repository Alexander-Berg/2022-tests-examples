package com.yandex.mobile.realty.test.filters

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.robot.FiltersRobot
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 09/06/2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
abstract class FilterParamTest {

    private var activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    protected fun shouldChangeOffersCount(
        webServerConfiguration: DispatcherRegistry.() -> Unit = {},
        actionConfiguration: FiltersRobot.() -> Unit,
        params: Array<Pair<String, String?>?>
    ) {
        configureWebServer {
            apply(webServerConfiguration)

            registerSearchCountOnly(EXPECTED_TOTAL_COUNT, *params)
            registerSearchCountOnly(EXPECTED_TOTAL_COUNT, *params)
            registerMapSearch(*params)
            registerListSearch(*params)
        }

        activityTestRule.launchActivity()

        performOnFiltersScreen {
            apply(actionConfiguration)

            waitUntil { offersCountEquals(EXPECTED_TOTAL_COUNT) }

            tapOn(lookup.matchesSubmitButton())
        }
        performOnSearchMapScreen {
            waitUntil { offersCountEquals(EXPECTED_MAP_COUNT, EXPECTED_TOTAL_COUNT) }

            tapOn(lookup.matchesSwitchViewModeButton())
        }
        onScreen<SearchListScreen> {
            offerSnippet("1").waitUntil { listView.contains(this) }
        }
    }

    protected fun DispatcherRegistry.registerSearchCountOnly(
        expectedCount: Int,
        vararg params: Pair<String, String?>?
    ) {

        val objectType = params
            .filterNotNull()
            .firstOrNull { it.first == "objectType" }
            ?.second
            ?.uppercase()

        when (objectType) {
            "NEWBUILDING",
            "VILLAGE" -> {
                register(
                    request {
                        path("1.0/offerWithSiteSearch.json")
                        queryParam("countOnly", "true")
                        for (item in params) {
                            item?.let { (name, value) ->
                                value?.let { queryParam(name, it) }
                            }
                        }
                    },
                    response {
                        setBody("{\"response\":{\"total\":$expectedCount}}")
                    }
                )
            }
            else -> {
                register(
                    request {
                        path("2.0/offers/number")
                        for (item in params) {
                            item?.let { (name, value) ->
                                value?.let { queryParam(name, it) }
                            }
                        }
                    },
                    response {
                        setBody("{\"response\":{\"number\":$expectedCount}}")
                    }
                )
            }
        }
    }

    private fun DispatcherRegistry.registerMapSearch(
        vararg params: Pair<String, String?>?
    ) {
        val objectType = params.find { it?.first == "objectType" }?.second

        val pathPrefix: String
        val responseBody: String
        when (objectType) {
            "NEWBUILDING" -> {
                pathPrefix = "2.0/newbuilding/pointSearch"
                responseBody = "{\"response\":{" +
                    "   \"logQueryId\": \"b7128fa3fad87a0c\"," +
                    "   \"url\": \"offerSearchV2.json\"," +
                    "   \"slicing\" : { " +
                    "       \"total\" : $EXPECTED_MAP_COUNT" +
                    "    }" +
                    " }" +
                    "}"
            }
            "VILLAGE" -> {
                pathPrefix = "2.0/village/pointSearch"
                responseBody = "{\"response\":{" +
                    "   \"logQueryId\": \"b7128fa3fad87a0c\"," +
                    "   \"url\": \"offerSearchV2.json\"," +
                    "   \"slicing\" : { " +
                    "       \"total\" : $EXPECTED_MAP_COUNT" +
                    "    }" +
                    " }" +
                    "}"
            }
            else -> {
                pathPrefix = "1.0/pointStatisticSearch.json"
                responseBody = "{\"response\":{" +
                    "   \"totalOffers\":$EXPECTED_MAP_COUNT, " +
                    "   \"searchQuery\" : { " +
                    "       \"logQueryId\" : \"b7128fa3fad87a0c\"," +
                    "       \"url\": \"offerSearchV2.json\"" +
                    "    }" +
                    " }" +
                    "}"
            }
        }

        register(
            request {
                path(pathPrefix)
                for (item in params) {
                    item?.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
            },
            response {
                setBody(responseBody)
            }
        )
    }

    private fun DispatcherRegistry.registerListSearch(
        vararg params: Pair<String, String?>?
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                for (item in params) {
                    item?.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
            },
            response {
                assetBody("offerWithSiteSearchDefaultSorting.json")
            }
        )
    }

    companion object {

        const val EXPECTED_TOTAL_COUNT = 10
        const val EXPECTED_MAP_COUNT = 5
    }
}
