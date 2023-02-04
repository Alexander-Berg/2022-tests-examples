package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
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
import com.yandex.mobile.realty.domain.mortgageprogram.model.Filter.FlatType
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 3/18/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageSearchListTest {

    private val activityTestRule = MortgageProgramListActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            regionParams = RegionParams(
                RGID.toInt(),
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

    @Before
    fun createImages() {
        createImageOnExternalDir(name = "test_image_0", rColor = 0, gColor = 255, bColor = 0)
    }

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun shouldSearchOffersWithParams() {
        configureWebServer {
            registerInitialParamsSearchSuccess("mortgageProgramInitialParams.json")
            registerMortgageProgramSearchSuccess("mortgageProgramSearchDefault.json")
            registerMortgageProgramSearchSuccess(
                "mortgageProgramSearchDefault.json",
                price = 10_000_000,
                flatType = "SECONDARY"
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchDefault.json",
                price = 2_700_000 to 3_300_000
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchChanged.json",
                price = 9_000_000 to 11_000_000,
                flatType = FlatType.SECONDARY
            )
            registerMortgageProgramSearchSuccess(
                "mortgageProgramSearchDefault.json",
                price = 10_000_000,
                flatType = "NEW"
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchNewFlatChanged.json",
                price = 9_000_000 to 11_000_000,
                flatType = FlatType.NEW
            )
            registerOffer()
        }

        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            offerSnippet(ID_DEFAULT)
                .waitUntil { listView.contains(this) }

            listView.isMortgageProgramListContentMatches(
                "MortgageSearchListTest/defaultFilterPrograms"
            )
            listView.scrollTo(flatTypeItem)

            flatTypeSecondary.click()
            priceSlider.setValue(10_000_000F)
            waitUntil { priceInputView.textMatches("10 000 000") }

            offerSnippet(ID_LAST)
                .waitUntil { listView.contains(this) }

            listView.isMortgageProgramListContentMatches(
                "MortgageSearchListTest/changedFilterPrograms"
            )

            offerSnippet(ID_LAST)
                .waitUntil { listView.contains(this) }
                .click()

            onScreen<OfferCardScreen> {
                waitUntil { floatingCallButton.isCompletelyDisplayed() }
                appBar.collapse()
                isInitialStateMatches("MortgageSearchListTest/offerCardState")
                pressBack()
            }

            listView.scrollTo(flatTypeItem)
            flatTypeNew.click()

            siteSnippet(ID_NEW_FLAT)
                .waitUntil { listView.contains(this) }

            listView.isMortgageProgramListContentMatches(
                "MortgageSearchListTest/changedFilterNewFlatPrograms"
            )
        }
    }

    @Test
    fun shouldShowAllOffers() {
        configureWebServer {
            registerInitialParamsSearchSuccess("mortgageProgramInitialParams.json")
            registerMortgageProgramSearchSuccess("mortgageProgramSearchDefault.json")
            registerOffersWithSiteSearch(
                "offersWithSiteSearchDefault.json",
                flatType = FlatType.ANY
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchDefault.json",
                flatType = FlatType.ANY
            )
            registerMortgageProgramSearchSuccess(
                "mortgageProgramSearchDefault.json",
                flatType = "NEW"
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchNewFlatChanged.json",
                flatType = FlatType.NEW
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchNewFlatChanged.json",
                flatType = FlatType.NEW
            )
            registerMortgageProgramSearchSuccess(
                "mortgageProgramSearchDefault.json",
                flatType = "SECONDARY"
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchChanged.json",
                flatType = FlatType.SECONDARY
            )
            registerOffersWithSiteSearch(
                "offersWithSiteSearchChanged.json",
                flatType = FlatType.SECONDARY
            )
        }

        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            offerSnippet(ID_DEFAULT)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(showAllOffersButtonItem)
                .isViewStateMatches(
                    "MortgageSearchListTest/showAllOffersButton"
                )
                .click()

            onScreen<SearchListScreen> {
                offerSnippet(ID_DEFAULT)
                    .waitUntil { listView.contains(this) }
                pressBack()
            }

            listView.scrollTo(flatTypeItem)
            flatTypeNew.click()

            siteSnippet(ID_NEW_FLAT)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(showAllOffersButtonItem)
                .isViewStateMatches(
                    "MortgageSearchListTest/showAllOffersButton"
                )
                .click()

            onScreen<SearchListScreen> {
                siteSnippet(ID_NEW_FLAT)
                    .waitUntil { listView.contains(this, false) }
                pressBack()
            }

            listView.scrollTo(flatTypeItem)
            flatTypeSecondary.click()

            offerSnippet(ID_DEFAULT)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(showAllOffersButtonItem)
                .isViewStateMatches(
                    "MortgageSearchListTest/showAllOffersButton"
                )
                .click()

            onScreen<SearchListScreen> {
                offerSnippet(ID_DEFAULT)
                    .waitUntil { listView.contains(this) }
            }
        }
    }

    @Test
    fun shouldShowErrorsAndRetry() {
        configureWebServer {
            registerInitialParamsSearchError()
            registerInitialParamsSearchSuccess("mortgageProgramInitialParams.json")
            registerMortgageProgramSearchError()
            registerMortgageProgramSearchSuccess("mortgageProgramSearchDefault.json")
            registerMortgageProgramSearchError(page = 1)
            registerMortgageProgramSearchSuccess(
                "mortgageProgramSearchDefault.json",
                page = 1
            )
            registerOffersWithSiteSearchError()
            registerOffersWithSiteSearch("offersWithSiteSearchDefault.json")
        }

        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            fullScreenErrorItem.waitUntil { listView.contains(this) }
                .also {
                    listView.isMortgageProgramListContentMatches(
                        "MortgageSearchListTest/retryNoOffersHeader"
                    )
                }

            fullScreenRetryButton.click()
            fullScreenErrorItem.waitUntil { listView.contains(this) }
                .also {
                    listView.isMortgageProgramListContentMatches(
                        "MortgageSearchListTest/retryNoOffersHeader"
                    )
                }

            fullScreenRetryButton.click()

            nextPageButtonItem.waitUntil { listView.contains(this) }
                .click()

            programsErrorItem.waitUntil { listView.contains(this) }
                .click()

            searchErrorItem.waitUntil { listView.contains(this) }
                .also {
                    listView.isMortgageProgramListContentMatches(
                        "MortgageSearchListTest/retryWithOffersHeader"
                    )
                }
                .click()

            offerSnippet(ID_DEFAULT)
                .waitUntil { listView.contains(this) }

            listView.isMortgageProgramListContentMatches(
                "MortgageSearchListTest/successAfterRetryWithOffersHeader"
            )
        }
    }

    @Test
    fun shouldShowOrHideOffersBlock() {
        configureWebServer {
            registerInitialParamsSearchSuccess("mortgageProgramInitialParams.json")
            registerMortgageProgramSearchSuccess("mortgageProgramSearchDefault.json")
            registerMortgageProgramSearchSuccess(
                "mortgageProgramSearchDefault.json",
                flatType = "SECONDARY"
            )
            registerOffersWithSiteSearch("offersWithSiteSearchMoreThanPageSize.json")
            registerOffersWithSiteSearch(
                "offersWithSiteSearchEmpty.json",
                flatType = FlatType.SECONDARY
            )
        }

        activityTestRule.launchActivity()
        onScreen<MortgageProgramListScreen> {
            offerSnippet(ID_LAST)
                .waitUntil { listView.contains(this) }

            listView.isMortgageProgramListContentMatches(
                "MortgageSearchListTest/maxPageSizeOffers"
            )

            listView.scrollTo(flatTypeItem)
            flatTypeSecondary.click()

            mortgageProgramSnippet(PROGRAM_ID_DEFAULT)
                .waitUntil { listView.contains(this) }

            listView.isMortgageProgramListContentMatches(
                "MortgageSearchListTest/emptyOffers"
            )
        }
    }

    private fun DispatcherRegistry.registerInitialParamsSearchSuccess(
        responseFileName: String
    ) {
        registerInitialParamsSearch(
            response {
                assetBody("MortgageSearchListTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerInitialParamsSearchError() {
        registerInitialParamsSearch(response { setResponseCode(400) })
    }

    private fun DispatcherRegistry.registerInitialParamsSearch(response: MockResponse) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("page", "0")
            },
            response
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearchSuccess(
        responseFileName: String,
        rgid: String = RGID,
        flatType: String? = null,
        price: Int? = null,
        page: Int = 0
    ) {
        registerMortgageProgramSearch(
            rgid = rgid,
            flatType = flatType,
            price = price,
            page = page,
            response = response { assetBody("MortgageSearchListTest/$responseFileName") }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearchError(
        rgid: String = RGID,
        flatType: String? = null,
        price: Int? = null,
        page: Int = 0
    ) {
        registerMortgageProgramSearch(
            rgid = rgid,
            flatType = flatType,
            price = price,
            page = page,
            response = response { setResponseCode(400) }
        )
    }

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        rgid: String,
        flatType: String?,
        price: Int?,
        page: Int,
        response: MockResponse
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                queryParam("rgid", rgid)
                queryParam("page", page.toString())
                flatType?.let { queryParam("flatType", it) }
                price?.let { queryParam("propertyCost", it.toString()) }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch(
        responseFileName: String,
        rgid: String = RGID_DEFAULT,
        flatType: FlatType = FlatType.ANY,
        price: Pair<Int, Int> = 2_700_000 to 3_300_000
    ) {
        registerOffersWithSiteSearch(
            rgid = rgid,
            flatType = flatType,
            price = price,
            response = response { assetBody("MortgageSearchListTest/$responseFileName") }
        )
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearchError(
        rgid: String = RGID_DEFAULT,
        flatType: FlatType = FlatType.ANY,
        price: Pair<Int, Int> = 2_700_000 to 3_300_000,
    ) {
        registerOffersWithSiteSearch(
            rgid = rgid,
            flatType = flatType,
            price = price,
            response { setResponseCode(400) }
        )
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch(
        rgid: String,
        flatType: FlatType,
        price: Pair<Int, Int>,
        response: MockResponse
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("rgid", rgid)
                queryParam("page", "0")
                queryParam("priceMin", price.first.toString())
                queryParam("priceMax", price.second.toString())
                queryParam("type", "SELL")
                queryParam("category", "APARTMENT")
                when (flatType) {
                    FlatType.SECONDARY -> {
                        queryParam("objectType", "OFFER")
                        queryParam("newFlat", "NO")
                    }
                    FlatType.ANY -> {
                        queryParam("objectType", "OFFER")
                    }
                    FlatType.NEW -> {
                        queryParam("objectType", "NEWBUILDING")
                    }
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("MortgageSearchListTest/cardWithViews.json")
            }
        )
    }

    companion object {

        private const val RGID = "587795"
        private const val RGID_DEFAULT = "587795"

        private const val ID_DEFAULT = "1"
        private const val ID_NEW_FLAT = "15349"
        private const val ID_LAST = "10"

        private const val PROGRAM_ID_DEFAULT = "1"
    }
}
