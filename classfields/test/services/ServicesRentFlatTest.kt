package com.yandex.mobile.realty.test.services

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatFormScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 01/12/2021.
 */
@LargeTest
class ServicesRentFlatTest : BaseTest() {

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
    fun showAddRentFlatButtonWhenStatusConfirmed() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "CONFIRMED")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.click() }
        }

        onScreen<RentFlatFormScreen> {
            waitUntil { contentView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowAddRentFlatButtonWhenStatusDraft() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "DRAFT")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.isHidden() }
        }
    }

    @Test
    fun shouldNotShowAddRentFlatButtonWhenStatusWaitingForConfirmation() {
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(status = "WAITING_FOR_CONFIRMATION")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.isHidden() }
        }
    }

    @Test
    fun shouldNotShowAddRentFlatButtonForTenant() {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlats(status = "RENTED")
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatsHeaderItem()
                .waitUntil { listView.contains(this) }
                .invoke { addButton.isHidden() }
        }
    }

    @Test
    fun shouldOpenRentFlat() {
        configureWebServer {
            registerTenantServicesInfo()
            registerTenantRentFlats()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }
        onScreen<RentFlatScreen> {
            flatHeaderItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("header"))
        }
    }
}
