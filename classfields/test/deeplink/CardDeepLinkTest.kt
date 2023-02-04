package com.yandex.mobile.realty.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.core.DeepLinkIntentCommand
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.screen.VillageCardScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author merionkov on 11/01/2021.
 */
@RunWith(AndroidJUnit4::class)
class CardDeepLinkTest {

    @Test
    fun shouldOpenOfferCard() {

        val deepLink = "https://realty.yandex.ru/offer/$OFFER_ID/"

        configureWebServer {
            registerOfferCard()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<OfferCardScreen> {
            priceView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(OFFER_PRICE)
        }
    }

    @Test
    fun shouldOpenSiteCard() {

        val deepLink = "https://realty.yandex.ru/$SITE_PATH"

        configureWebServer {
            registerRegionInfoSPB()
            registerDeepLink(deepLink, "SITE_CARD", SITE_ID)
            registerSiteCard()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SiteCardScreen> {
            titleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(SITE_NAME)
        }
    }

    @Test
    fun shouldOpenSiteCardDirectly() {

        val deepLink = "https://realty.yandex.ru/newbuilding/$SITE_ID"

        configureWebServer {
            registerRegionInfoSPB()
            registerSiteCard()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SiteCardScreen> {
            titleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(SITE_NAME)
        }
    }

    @Test
    fun shouldOpenVillageCard() {

        val deepLink = "https://realty.yandex.ru/$VILLAGE_PATH"

        configureWebServer {
            registerRegionInfoSPB()
            registerDeepLink(deepLink, "VILLAGE_CARD", VILLAGE_ID)
            registerVillageCard()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<VillageCardScreen> {
            titleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(VILLAGE_NAME)
        }
    }

    @Test
    fun shouldOpenVillageCardDirectly() {

        val deepLink = "https://realty.yandex.ru/village/$VILLAGE_ID"

        configureWebServer {
            registerRegionInfoSPB()
            registerVillageCard()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<VillageCardScreen> {
            titleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(VILLAGE_NAME)
        }
    }

    private fun DispatcherRegistry.registerRegionInfoSPB() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
            },
            response {
                assetBody("regionInfo417899.json")
            }
        )
    }

    private fun DispatcherRegistry.registerDeepLink(
        deepLink: String,
        action: String,
        subjectId: String,
    ) {
        register(
            request {
                path("1.0/deeplink.json")
                jsonBody { "url" to deepLink }
            },
            response {
                setBody(
                    """
                            {
                                "response": {
                                    "action": "$action",
                                    "region": {
                                        "rgid": 417899,
                                        "name": "Санкт-Петербург",
                                        "point": {
                                            "latitude": 59.938953,
                                            "longitude": 30.31564,
                                            "defined": true
                                        },
                                        "lt": {
                                            "latitude": 60.244812,
                                            "longitude": 29.425114,
                                            "defined": true
                                        },
                                        "rb": {
                                            "latitude": 59.633713,
                                            "longitude": 30.75953,
                                            "defined": true
                                        },
                                        "searchParams": { "rgid": ["417899"] }
                                    },
                                    "params": [
                                        { "name": "id", "values": ["$subjectId"] }
                                    ]
                                }
                            }
                            """
                )
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCard() {
        register(
            request {
                path("1.0/cardWithViews.json")
                queryParam("id", OFFER_ID)
            },
            response {
                assetBody("cardWithViews.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteCard() {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteWithOfferStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerVillageCard() {
        register(
            request {
                path("2.0/village/$VILLAGE_ID/card")
            },
            response {
                assetBody("villageCard.json")
            }
        )
    }

    private companion object {

        const val OFFER_ID = "8302110760157452800"
        const val OFFER_PRICE = "1 531 231 \u20BD"

        const val SITE_ID = "1"
        const val SITE_PATH = "sankt-peterburg/kupit/novostrojka/jk-novostrojka/"
        const val SITE_NAME = "ЖК «Имя»"

        const val VILLAGE_ID = "0"
        const val VILLAGE_PATH = "sankt-peterburg/kupit/kottedzhnye-poselki/poselok/?id=0"
        const val VILLAGE_NAME = "Коттеджный посёлок «Name»"
    }
}
