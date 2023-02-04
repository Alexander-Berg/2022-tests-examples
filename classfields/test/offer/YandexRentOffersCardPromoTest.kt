package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 25/06/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class YandexRentOffersCardPromoTest {

    private var activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun showYandexRentOffersPromo() {
        configureWebServer {
            registerRentOffer()
            registerYandexRentAvailable()
            registerDynamicBoundingBox()
            registerOffersSearch()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCallButton.isCompletelyDisplayed() }
            appBar.collapse()

            listView.contains(yandexRentOffersPromoItem)
                .isViewStateMatches("YandexRentOffersCardPromoTest/promoBlock")
            showYandexRentOffersButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("1")) }
        }
    }

    @Test
    fun shouldNotShowPromoWhenYandexRentIsNotAvailableInLocation() {
        configureWebServer {
            registerRentOffer()
            registerYandexRentAvailable(false)
            registerDynamicBoundingBox()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCallButton.isCompletelyDisplayed() }
            appBar.collapse()

            listView.doesNotContain(yandexRentOffersPromoItem)
        }
    }

    @Test
    fun shouldNotShowPromoInYandexRentOffer() {
        configureWebServer {
            registerYandexRentOffer()
            registerYandexRentAvailable()
            registerDynamicBoundingBox()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCallButton.isCompletelyDisplayed() }
            appBar.collapse()

            listView.doesNotContain(yandexRentOffersPromoItem)
        }
    }

    private fun DispatcherRegistry.registerYandexRentOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("authorTest/cardWithYandexRentInfo.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffersSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("type", "RENT")
                queryParam("category", "APARTMENT")
                queryParam("rentTime", "LARGE")
                queryParam("yandexRent", "YES")
                queryParam("page", "0")
                queryParamKey("leftLongitude")
                queryParamKey("rightLongitude")
                queryParamKey("bottomLatitude")
                queryParamKey("topLatitude")
                excludeQueryParamKey("rgid")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchDefaultSorting.json")
            }
        )
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class YandexRentOffersCardPromoNegativeTest {

    private var activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            regionParams = RegionParams(
                0,
                0,
                "в Москве и МО",
                emptyMap(),
                emptyMap(),
                RegionParamsConfigImpl.DEFAULT.schoolInfo,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                null
            )
        ),
        activityTestRule
    )

    @Test
    fun shouldNotShowPromoWhenRegionParamsNotAllowIt() {
        configureWebServer {
            registerRentOffer()
            registerYandexRentAvailable()
            registerDynamicBoundingBox()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            waitUntil { floatingCallButton.isCompletelyDisplayed() }
            appBar.collapse()

            listView.doesNotContain(yandexRentOffersPromoItem)
        }
    }
}

private fun DispatcherRegistry.registerRentOffer() {
    register(
        request {
            path("1.0/cardWithViews.json")
        },
        response {
            assetBody("YandexRentOffersCardPromoTest/offer.json")
        }
    )
}

private fun DispatcherRegistry.registerYandexRentAvailable(
    available: Boolean = true
) {
    register(
        request {
            method("GET")
            path("2.0/rent/is-point-rent")
            queryParam("latitude", "55.72911")
            queryParam("longitude", "37.62253")
        },
        response {
            setBody("{\"response\":{\"isPointInsidePolygon\":$available}}")
        }
    )
}

private fun DispatcherRegistry.registerDynamicBoundingBox() {
    register(
        request {
            path("1.0/dynamicBoundingBox")
            queryParam("rgid", "741965")
            queryParam("type", "RENT")
            queryParam("category", "APARTMENT")
            queryParam("rentTime", "LARGE")
            queryParam("priceType", "PER_OFFER")
            queryParam("yandexRent", "YES")
        },
        response {
            setBody(
                """
                                {
                                    "response": {
                                        "boundingBox": {
                                            "leftLongitude": 37.56635,
                                            "rightLongitude": 37.601276,
                                            "bottomLatitude": 55.75428,
                                            "topLatitude": 55.76093
                                        }
                                    }
                                }
                """.trimIndent()
            )
        }
    )
}
