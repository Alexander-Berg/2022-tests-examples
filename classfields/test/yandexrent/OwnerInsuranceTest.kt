package com.yandex.mobile.realty.test.yandexrent

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesExternalViewUrlIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentInsurancePromoScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.services.FLAT_ADDRESS
import com.yandex.mobile.realty.test.services.contractInfo
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.test.services.registerOwnerRentFlats
import com.yandex.mobile.realty.test.services.registerOwnerServicesInfo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrey-bgm on 07.04.2022
 */
@LargeTest
class OwnerInsuranceTest : BaseTest() {

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
    fun showInsuranceInfo() {
        val contractInfo = contractInfo(insuranceIsActive = true)
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(contractInfo = contractInfo)
            registerOwnerRentFlat(contractInfo = contractInfo)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        registerResultOkIntent(matchesExternalViewUrlIntent(RENT_HELP_URL), null)

        onScreen<RentInsurancePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }

            root.isViewStateMatches(getTestRelatedFilePath("insurancePromo"))
            actionButton.click()

            intended(matchesExternalViewUrlIntent(RENT_HELP_URL))
            pressBack()
        }

        onScreen<RentFlatScreen> {
            insuranceView.waitUntil { isCompletelyDisplayed() }

            toolbar.isViewStateMatches(getTestRelatedFilePath("toolbarWithInsurance"))
            insuranceView.click()
        }

        onScreen<RentInsurancePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun showInsurancePromoOnceForContract() {
        val contractInfo1 = contractInfo(contractId = CONTRACT_ID_1, insuranceIsActive = true)
        val contractInfo2 = contractInfo(contractId = CONTRACT_ID_2, insuranceIsActive = true)
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(contractInfo = contractInfo1)
            registerOwnerRentFlat(contractInfo = contractInfo1)
            registerOwnerRentFlat(contractInfo = contractInfo1)

            registerOwnerServicesInfo()
            registerOwnerRentFlats(contractInfo = contractInfo2)
            registerOwnerRentFlat(contractInfo = contractInfo2)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentInsurancePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
            pressBack()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
            pressBack()
        }

        onScreen<ServicesScreen> {
            listView.swipeDown()

            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentInsurancePromoScreen> {
            waitUntil { promoView.isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldNotShowInsuranceInfoWhenNotInsured() {
        val contractInfo = contractInfo(insuranceIsActive = false)
        configureWebServer {
            registerOwnerServicesInfo()
            registerOwnerRentFlats(contractInfo = contractInfo)
            registerOwnerRentFlat(contractInfo = contractInfo)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<ServicesScreen> {
            rentFlatHeaderItem(FLAT_ADDRESS)
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<RentFlatScreen> {
            flatHeaderItem.waitUntil { listView.contains(this) }
            insuranceView.isHidden()
        }
    }

    private companion object {

        const val RENT_HELP_URL = "https://yandex.ru/support/realty/arenda.html"
        const val CONTRACT_ID_1 = "contractId0001"
        const val CONTRACT_ID_2 = "contractId0002"
    }
}
