package com.yandex.mobile.realty.test.agency

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.AgencyCardActivityTestRule
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.robot.performOnAgencyCardScreen
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.rule.DateRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.Screen
import com.yandex.mobile.realty.domain.model.ScreenReferrer
import com.yandex.mobile.realty.domain.model.agency.AgencyContext
import com.yandex.mobile.realty.domain.model.agency.AgencyProfilePreview
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.ui.model.AgencyCardParams
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author andrey-bgm on 12/11/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AgencyCardTest {

    private val activityTestRule = AgencyCardActivityTestRule(launchActivity = false)
    private val dateRule = DateRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        dateRule
    )

    @Before
    fun setTestDate() {
        dateRule.setDate(2020, 12, 14)
    }

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun checkAgencyCard() {
        configureWebServer {
            registerAgencyCard()
            registerOffers("SELL", "APARTMENT")
        }
        createImageOnExternalDir(rColor = 255, gColor = 0, bColor = 0)
        launchAgencyActivity()

        performOnAgencyCardScreen {
            waitUntil { isExpandedToolbarNameEquals(AGENCY_NAME) }
            collapseAppBar()
            waitUntil { containsTotalOffersSubtitle() }
            scrollToTop()
            expandAppBar()

            isInitialStateMatches("/AgencyCardTest/agency/initialState")
            collapseAppBar()
            isContentMatches("/AgencyCardTest/agency/content")

            scrollToPosition(lookup.matchesShowAllOffersButton())
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesShowAllOffersButton())
        }

        onScreen<SearchListScreen> {
            offerSnippet("1").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun checkAgentCard() {
        configureWebServer {
            registerAgentCard()
            registerOffers("RENT", "ROOMS", "SHORT")
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        launchAgentActivity()

        performOnAgencyCardScreen {
            waitUntil { isExpandedToolbarNameEquals(AGENT_NAME) }
            collapseAppBar()
            waitUntil { containsTotalOffersSubtitle() }
            scrollToTop()
            expandAppBar()

            isInitialStateMatches("/AgencyCardTest/agent/initialState")
            collapseAppBar()
            isContentMatches("/AgencyCardTest/agent/content")

            scrollToPosition(lookup.matchesShowAllOffersButton())
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesShowAllOffersButton())
        }

        onScreen<SearchListScreen> {
            offerSnippet("1").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun openOfferCardFromSnippet() {
        configureWebServer {
            registerAgencyCard()
            registerOffers("SELL", "APARTMENT")
            registerCardWithViews()
        }
        launchAgencyActivity()

        performOnAgencyCardScreen {
            waitUntil { isExpandedToolbarNameEquals(AGENCY_NAME) }
            collapseAppBar()

            waitUntil { containsOfferSnippet("1") }
            tapOn(lookup.matchesSnippetView("1"))
        }

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("1\u00a0531\u00a0231\u00a0\u20BD") }
        }
    }

    private fun launchAgencyActivity() {
        val agencyContext = AgencyContext.Sell(
            regionId = RGID,
            agencyPreview = AgencyProfilePreview(
                uid = UID,
                creationDate = Calendar.getInstance().run {
                    set(2019, 10, 12)
                    time
                },
                userType = AgencyProfilePreview.UserType.AGENCY,
                name = AGENCY_NAME,
                photo = "file:///sdcard/realty_images/test_image.jpeg"
            ),
            property = Filter.Property.APARTMENT
        )

        val params = AgencyCardParams(
            agencyContext = agencyContext,
            screenReferrer = ScreenReferrer.valueOf(Screen.OFFER_DETAILS)
        )

        activityTestRule.launchActivity(AgencyCardActivityTestRule.createIntent(params))
    }

    private fun launchAgentActivity() {
        val agencyContext = AgencyContext.Rent(
            regionId = RGID,
            agencyPreview = AgencyProfilePreview(
                uid = UID,
                creationDate = Calendar.getInstance().run {
                    set(2020, 1, 16)
                    time
                },
                userType = AgencyProfilePreview.UserType.AGENT,
                name = AGENT_NAME,
                photo = "file:///sdcard/realty_images/test_image.jpeg"
            ),
            property = Filter.Property.ROOM,
            rentTime = Filter.RentTime.SHORT
        )

        val params = AgencyCardParams(
            agencyContext = agencyContext,
            screenReferrer = ScreenReferrer.valueOf(Screen.OFFER_DETAILS)
        )

        activityTestRule.launchActivity(AgencyCardActivityTestRule.createIntent(params))
    }

    private fun DispatcherRegistry.registerAgencyCard() {
        register(
            request {
                path("2.0/agencies/active/user/uid:$UID")
                queryParam("rgid", RGID.toString())
            },
            response {
                assetBody("agencyTest/agency.json")
            }
        )
    }

    private fun DispatcherRegistry.registerAgentCard() {
        register(
            request {
                path("2.0/agencies/active/user/uid:$UID")
                queryParam("rgid", RGID.toString())
            },
            response {
                assetBody("agencyTest/agent.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffers(
        type: String,
        category: String,
        rentTime: String? = null
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("showOnMobile", "YES")
                queryParam("showSimilar", "NO")
                queryParam("currency", "RUR")
                queryParam("sort", "RELEVANCE")
                queryParam("page", "0")
                queryParam("pageSize", "5")
                queryParam("priceType", "PER_OFFER")
                queryParam("rgid", "$RGID")
                queryParam("type", type)
                queryParam("category", category)
                if (rentTime != null) {
                    queryParam("rentTime", rentTime)
                } else {
                    queryParam("objectType", "OFFER")
                }
                queryParam("uid", UID)
            },
            response {
                assetBody("offerWithSiteSearchDefaultSorting.json")
            }
        )

        register(
            request {
                path("1.0/dynamicBoundingBox")
                queryParam("rgid", "$RGID")
                queryParam("type", type)
                queryParam("category", category)
                if (rentTime != null) {
                    queryParam("rentTime", rentTime)
                }
                queryParam("uid", UID)
                queryParam("priceType", "PER_OFFER")
            },
            response {
                assetBody("agencyTest/agencyAllOffersBoundingBox.json")
            }
        )

        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("type", type)
                queryParam("category", category)
                if (rentTime != null) {
                    queryParam("rentTime", rentTime)
                }
                queryParam("uid", UID)
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

    private fun DispatcherRegistry.registerCardWithViews() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("cardWithViews.json")
            }
        )
    }

    private companion object {

        const val AGENCY_NAME = "Этажи"
        const val AGENT_NAME = "Александр"
        const val UID = "1"
        const val RGID = 741_965L
    }
}
