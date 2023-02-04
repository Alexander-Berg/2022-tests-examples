package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 29.03.2022
 */
@RunWith(AndroidJUnit4::class)
class OfferCardDetailsTest : BaseTest() {

    private var activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowNegativeDetailsInSecondarySellApartment() {
        configureWebServer {
            registerOfferDetail("sellSecondaryApartmentOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            houseHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(houseExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isHouseSectionStateMatches(getTestRelatedFilePath("houseSection"))
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                17
            )
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInNewSellApartment() {
        configureWebServer {
            registerOfferDetail("sellNewApartmentOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            flatHeaderItem.waitUntil { listView.contains(this) }
            listView.isFlatSectionStateMatches(getTestRelatedFilePath("flatSection"), 5)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInSellRoom() {
        configureWebServer {
            registerOfferDetail("sellRoomOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            houseHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(houseExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isHouseSectionStateMatches(getTestRelatedFilePath("houseSection"))
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                17
            )
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInSellHouse() {
        configureWebServer {
            registerOfferDetail("sellHouseOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            facilitiesHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                12
            )
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 3)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInSellLot() {
        configureWebServer {
            registerOfferDetail("sellLotOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            dealHeaderItem.waitUntil { listView.contains(this) }
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInSellCommercial() {
        configureWebServer {
            registerOfferDetail("sellCommercialOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            dealHeaderItem.waitUntil { listView.contains(this) }
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
            listView.isObjectSectionStateMatches(getTestRelatedFilePath("objectSection"))
            listView.scrollTo(detailsExpanderItem(12))
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isEquipmentsSectionStateMatches(getTestRelatedFilePath("equipmentsSection"))
            listView.scrollTo(detailsExpanderItem(4))
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isComplexSectionStateMatches(getTestRelatedFilePath("complexSection"))
        }
    }

    @Test
    fun shouldShowNegativeDetailsInSellGarage() {
        configureWebServer {
            registerOfferDetail("sellGarageOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            dealHeaderItem.waitUntil { listView.contains(this) }
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 3)
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                15
            )
        }
    }

    @Test
    fun shouldShowNegativeDetailsInRentApartment() {
        configureWebServer {
            registerOfferDetail("rentApartmentOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            facilitiesHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                19
            )
            listView.scrollTo(houseExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isHouseSectionStateMatches(getTestRelatedFilePath("houseSection"))
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInLongRentRoom() {
        configureWebServer {
            registerOfferDetail("rentRoomOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            facilitiesHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                19
            )
            listView.scrollTo(houseExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isHouseSectionStateMatches(getTestRelatedFilePath("houseSection"))
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInDailyRentRoom() {
        configureWebServer {
            registerOfferDetail("dailyRentRoomOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            facilitiesHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                19
            )
            listView.isFlatSectionStateMatches(getTestRelatedFilePath("flatSection"), 3)
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInRentHouse() {
        configureWebServer {
            registerOfferDetail("rentHouseOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            facilitiesHeaderItem.waitUntil { listView.contains(this) }
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                13
            )
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 4)
        }
    }

    @Test
    fun shouldShowNegativeDetailsInRentCommercial() {
        configureWebServer {
            registerOfferDetail("rentCommercialOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            dealHeaderItem.waitUntil { listView.contains(this) }
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 7)
            listView.isObjectSectionStateMatches(getTestRelatedFilePath("objectSection"))
            listView.scrollTo(detailsExpanderItem(12))
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isEquipmentsSectionStateMatches(getTestRelatedFilePath("equipmentsSection"))
            listView.scrollTo(detailsExpanderItem(4))
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isComplexSectionStateMatches(getTestRelatedFilePath("complexSection"))
        }
    }

    @Test
    fun shouldShowNegativeDetailsInRentGarage() {
        configureWebServer {
            registerOfferDetail("rentGarageOfferDetailsNegative.json")
        }
        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            floatingCallButton.waitUntil { isCompletelyDisplayed() }
            appBar.collapse()
            dealHeaderItem.waitUntil { listView.contains(this) }
            listView.isDealSectionStateMatches(getTestRelatedFilePath("dealSection"), 6)
            listView.scrollTo(facilitiesExpanderItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isFacilitiesSectionStateMatches(
                getTestRelatedFilePath("facilitiesSection"),
                15
            )
        }
    }

    private fun DispatcherRegistry.registerOfferDetail(responseName: String) {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("OfferCardDetailsTest/$responseName")
            }
        )
    }
}
