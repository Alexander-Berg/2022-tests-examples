package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FilterPromoScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.search.Filter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 17/02/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class YandexRentListPromoTest {

    private var activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true, filter = Filter.RentApartment()),
        activityTestRule
    )

    @Test
    fun showYandexRentPromo() {
        configureWebServer {
            registerSearchWithDisabledPromo()
            registerSearchWithEnabledPromo()
            registerSearchWithDisabledPromo()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(offerSnippet("0")) }

            listView.contains(yandexRentListPromoView)
                .isViewStateMatches("/YandexRentListPromoTest/yandexRentPromoBlock")
            yandexRentListPromoSwitch.click()

            waitUntil { listView.contains(offerSnippet("5")) }
            listView.scrollTo(yandexRentListPromoView).click()

            onScreen<FilterPromoScreen> {
                waitUntil { promoView.isCompletelyDisplayed() }
                root.isViewStateMatches("/YandexRentListPromoTest/yandexRentPromoDialog")
                okButton.click()
                promoView.doesNotExist()
            }

            listView.scrollTo(yandexRentListPromoView).click()

            onScreen<FilterPromoScreen> {
                waitUntil { promoView.isCompletelyDisplayed() }
                parametersButton.click()
                promoView.doesNotExist()
            }

            onScreen<FiltersScreen> {
                waitUntil { listView.contains(yandexRentField) }
                yandexRentValue.isChecked()
                yandexRentField.view.click()
                submitButton.click()
            }

            waitUntil { listView.contains(offerSnippet("0")) }
            listView.scrollTo(yandexRentListPromoView)
            yandexRentListPromoSwitch.isNotChecked()
        }
    }

    @Test
    fun shouldNotShowYandexRentPromoWhenRentShortApartment() {
        configureWebServer {
            registerSearchWithDisabledPromo()
            registerSearchWithDisabledPromo()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            waitUntil { listView.contains(yandexRentListPromoView) }

            filterButton.click()

            onScreen<FiltersScreen> {
                rentTimeSelectorShort.click()
                submitButton.click()
            }

            waitUntil { listView.contains(offerSnippet("0")) }
            listView.doesNotContain(yandexRentListPromoView)
            listView.contains(renovationListPromoView)
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
            waitUntil { listView.contains(yandexRentListPromoView) }
            yandexRentListPromoSwitch.isNotChecked()

            filterButton.click()

            onScreen<FiltersScreen> {
                listView.scrollTo(yandexRentField).click()
                submitButton.click()
            }

            waitUntil { listView.contains(offerSnippet("5")) }
            listView.contains(yandexRentListPromoView)
            yandexRentListPromoSwitch.isChecked()
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
                queryParam("yandexRent", "YES")
            },
            response {
                assetBody("offerWithSiteSearchPage1.json")
            }
        )
    }
}
