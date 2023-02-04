package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.domain.mortgageprogram.model.FlatType
import com.yandex.mobile.realty.input.createStandardProgram
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 6/22/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardSuitableOffersTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(
        program = createStandardProgram(CARD_PROGRAM_ID)
            .copy(flatType = setOf(FlatType.SECONDARY)),
        launchActivity = false
    )

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

    @Test
    fun shouldShowSuitableOffersDefault() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
            repeat(2) {
                registerOffersWithSiteSearch("offersWithSiteSearchDefault.json")
            }
            registerOffer(OFFER_ID)
        }
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardSuitableOffersTest/shouldShowSuitableOffersDefault"
        onScreen<MortgageProgramCardScreen> {
            offersTitleItem.waitUntil { listView.contains(this) }
                .isViewStateMatches("$prefix/suitableOffersTitle")

            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .click()

            onScreen<OfferCardScreen> {
                waitUntil { floatingCallButton.isCompletelyDisplayed() }
                pressBack()
            }

            listView.scrollTo(showAllOffersButtonItem)
                .isViewStateMatches("$prefix/showAllOffersButton")
                .click()

            onScreen<SearchListScreen> {
                offerSnippet(OFFER_ID)
                    .waitUntil { listView.contains(this) }
            }
        }
    }

    @Test
    fun shouldApplyCalculatorParameters() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
            registerOffersWithSiteSearch("offersWithSiteSearchDefault.json")
            repeat(2) {
                registerOffersWithSiteSearch(
                    "offersWithSiteSearchChanged.json",
                    price = 9_000_000 to 11_000_000
                )
            }
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(calculatorTitleItem)

            priceInputView.replaceText(PRICE_CHANGED.toString())

            offerSnippet(OFFER_ID_CHANGED)
                .waitUntil { listView.contains(this) }

            listView.scrollTo(showAllOffersButtonItem)
                .click()

            onScreen<SearchListScreen> {
                offerSnippet(OFFER_ID_CHANGED)
                    .waitUntil { listView.contains(this) }
            }
        }
    }

    @Test
    fun shouldShowSuitableOffersError() {
        configureWebServer {
            registerCalculatorConfig()
            registerCalculatorResult()
            registerOffersWithSiteSearchError()
            registerOffersWithSiteSearch("offersWithSiteSearchDefault.json")
        }
        activityTestRule.launchActivity()

        val prefix = "MortgageProgramCardSuitableOffersTest/shouldShowSuitableOffersError"
        onScreen<MortgageProgramCardScreen> {
            offersErrorItem.waitUntil { listView.contains(this) }
                .isViewStateMatches("$prefix/errorState")
                .click()

            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }

            listView.doesNotContain(offersErrorItem)
        }
    }

    @Test
    fun shouldRetrySuitableOffersAfterCalculatorError() {
        configureWebServer {
            registerCalculatorConfig(error = true)
            registerCalculatorConfig(error = false)
            registerCalculatorResult()
            registerOffersWithSiteSearch("offersWithSiteSearchDefault.json")
        }
        activityTestRule.launchActivity()

        onScreen<MortgageProgramCardScreen> {
            calculatorBlockErrorItem
                .waitUntil { listView.contains(this) }
                .click()

            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch(
        responseFileName: String,
        price: Pair<Int, Int> = 2_700_000 to 3_300_000
    ) {
        registerOffersWithSiteSearch(
            price = price,
            response = response {
                assetBody("MortgageProgramCardSuitableOffersTest/$responseFileName")
            }
        )
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearchError(
        price: Pair<Int, Int> = 2_700_000 to 3_300_000
    ) {
        registerOffersWithSiteSearch(
            price = price,
            error()
        )
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch(
        price: Pair<Int, Int>,
        response: MockResponse
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("rgid", RGID)
                queryParam("page", "0")
                queryParam("priceMin", price.first.toString())
                queryParam("priceMax", price.second.toString())
                queryParam("type", "SELL")
                queryParam("category", "APARTMENT")
                queryParam("objectType", "OFFER")
                queryParam("newFlat", "NO")
            },
            response
        )
    }

    private fun DispatcherRegistry.registerCalculatorConfig(error: Boolean = false) {
        register(
            request {
                path("2.0/mortgage/program/$CARD_PROGRAM_ID/calculator")
            },
            response {
                if (error) {
                    setResponseCode(400)
                } else {
                    assetBody("MortgageProgramCardSuitableOffersTest/calculatorConfig.json")
                }
            }
        )
    }

    private fun DispatcherRegistry.registerCalculatorResult() {
        register(
            request {
                path("2.0/mortgage/program/$CARD_PROGRAM_ID/calculator")
            },
            response {
                assetBody("MortgageProgramCardSuitableOffersTest/calculatorResultDefault.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer(id: String) {
        register(
            request {
                path("1.0/cardWithViews.json")
                queryParam("id", id)
            },
            response {
                assetBody("MortgageProgramCardSuitableOffersTest/cardWithViews.json")
            }
        )
    }

    companion object {

        private const val RGID = "587795"
        private const val OFFER_ID = "1"
        private const val PRICE_CHANGED = 10_000_000L
        private const val OFFER_ID_CHANGED = "10"
        private const val CARD_PROGRAM_ID = "1"
    }
}
