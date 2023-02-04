package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FilterPromoScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.RenovationTypeScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.service.RegionParamsConfigImpl
import com.yandex.mobile.realty.domain.model.geo.RegionParams
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.test.filters.RenovationType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 17/02/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class NonGrandmotherRenovationListPromoTest {

    private var activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            listMode = true,
            filter = Filter.RentApartment(),
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
    fun showRenovationPromo() {
        configureWebServer {
            registerSearchWithDisabledPromo()
            registerSearchWithEnabledPromo()
            registerSearchWithDisabledPromo()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.contains(renovationListPromoView)
                .isViewStateMatches(
                    "/NonGrandmotherRenovationListPromoTest/renovationPromoBlock"
                )
            renovationListPromoSwitch.click()

            waitUntil { listView.contains(offerSnippet("5")) }
            listView.scrollTo(renovationListPromoView)
            renovationListPromoDetailsButton.click()

            onScreen<FilterPromoScreen> {
                waitUntil { promoView.isCompletelyDisplayed() }
                root.isViewStateMatches(
                    "/NonGrandmotherRenovationListPromoTest/renovationPromoDialog"
                )
                okButton.click()
                promoView.doesNotExist()
            }

            renovationListPromoDetailsButton.click()

            onScreen<FilterPromoScreen> {
                waitUntil { promoView.isCompletelyDisplayed() }
                parametersButton.click()
                promoView.doesNotExist()
            }

            onScreen<FiltersScreen> {
                val renovationType = RenovationType.NON_GRANDMOTHER

                waitUntil { listView.contains(renovationTypeField) }
                renovationTypeValue.isTextEquals(renovationType.expected)
                renovationTypeField.view.click()

                onScreen<RenovationTypeScreen> {
                    renovationTypeView(renovationType).click()
                    positiveButton.click()
                }

                submitButton.click()
            }

            waitUntil { listView.contains(offerSnippet("0")) }
            listView.scrollTo(renovationListPromoView)
            renovationListPromoSwitch.isNotChecked()
        }
    }

    @Test
    fun hideRenovationPromo() {
        configureWebServer {
            registerSearchWithDisabledPromo()
            registerSearchWithDisabledPromo()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(renovationListPromoView) }

            renovationListPromoHideButton.click()
            listView.doesNotContain(renovationListPromoView)

            switchViewModeButton.click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            switchViewModeButton.click()
        }

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }
            listView.doesNotContain(renovationListPromoView)
        }
    }

    @Test
    fun shouldNotShowRenovationPromoWhenSellApartment() {
        configureWebServer {
            registerSearchWithDisabledPromo()
            registerSearchWithDisabledPromo()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(renovationListPromoView) }

            filterButton.click()

            onScreen<FiltersScreen> {
                dealTypeSelector.click()
                dealTypePopupBuy.click()
                submitButton.click()
            }

            waitUntil { listView.contains(offerSnippet("0")) }
            listView.doesNotContain(renovationListPromoView)
        }
    }

    @Test
    fun shouldTogglePromoSwitchWhenEnabledOnFiltersScreen() {
        configureWebServer {
            registerSearchWithDisabledPromo()
            registerSearchWithEnabledPromo()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(renovationListPromoView) }
            renovationListPromoSwitch.isNotChecked()

            filterButton.click()

            onScreen<FiltersScreen> {
                waitUntil { listView.contains(renovationTypeField) }

                renovationTypeField.view.click()

                onScreen<RenovationTypeScreen> {
                    renovationTypeView(RenovationType.NON_GRANDMOTHER).click()
                    positiveButton.click()
                }

                submitButton.click()
            }

            waitUntil { listView.contains(offerSnippet("5")) }
            listView.scrollTo(renovationListPromoView)
            renovationListPromoSwitch.isChecked()
        }
    }

    private fun DispatcherRegistry.registerSearchWithDisabledPromo() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchPage0.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchWithEnabledPromo() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("renovation", "NON_GRANDMOTHER")
            },
            response {
                assetBody("offerWithSiteSearchPage1.json")
            }
        )
    }
}
