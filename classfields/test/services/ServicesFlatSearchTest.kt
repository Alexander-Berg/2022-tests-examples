package com.yandex.mobile.realty.test.services

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentShowingsScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.showings.registerShowings
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 04.07.2022
 */
@LargeTest
class ServicesFlatSearchTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule
    )

    @Test
    fun shouldShowFlatSearch() {
        configureWebServer {
            registerServicesInfo(rentRole = RENT_ROLE_TENANT_CANDIDATE, showRentFlatSearch = true)
            registerRentFlats()
            registerShowings(showings = emptyList())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatSearchItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("flatSearch"))
                .click()
        }
        onScreen<RentShowingsScreen> {
            fullscreenEmptyItem.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowFlatSearch() {
        configureWebServer {
            registerServicesInfo(rentRole = RENT_ROLE_TENANT_CANDIDATE)
            registerRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
            listView.doesNotContain(rentFlatSearchItem)
        }
    }
}
