package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createImageAndGetUriString
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationWizardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AddOfferModeDialogScreen
import com.yandex.mobile.realty.core.screen.AddressRootScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.configureWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 16.07.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class DraftTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = UserOffersActivityTestRule(launchActivity = false)
    private val mockLocationRule = MockLocationRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        mockLocationRule,
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION),
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun fillSellApartmentFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
            registerGetNearAuroraAddressApartment()
            registerUserProfile()
            registerUserProfile()
        }

        prepareEmptySellApartmentForm()

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            fillAllSellApartmentValues(imageUri)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            checkSellApartmentFormIsFull(imageUri)
        }
    }

    @Test
    fun fillRentLongApartmentFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
            registerGetNearAuroraAddressApartment()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongApartment()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }
            fillAllRentApartmentValues(imageUri, PaymentPeriod.PER_MONTH)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            checkRentApartmentFormIsFull(imageUri, PaymentPeriod.PER_MONTH)
        }
    }

    @Test
    fun fillRentShortApartmentFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
            registerGetNearAuroraAddressApartment()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortApartment()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            fillAllRentApartmentValues(imageUri, PaymentPeriod.PER_DAY)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            checkRentApartmentFormIsFull(imageUri, PaymentPeriod.PER_DAY)
        }
    }

    @Test
    fun fillSellRoomFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressRoom()
            registerGetNearAuroraAddressRoom()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellRoom()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            fillAllSellRoomValues(imageUri)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()
            checkSellRoomFormIsFull(imageUri)
        }
    }

    @Test
    fun fillRentLongRoomFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressRoom()
            registerGetNearAuroraAddressRoom()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongRoom()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            fillAllRentRoomValues(imageUri, PaymentPeriod.PER_MONTH)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomExpandedToolbarTitle() }
            collapseAppBar()
            checkRentRoomFormIsFull(imageUri, PaymentPeriod.PER_MONTH)
        }
    }

    @Test
    fun fillRentShortRoomFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressRoom()
            registerGetNearAuroraAddressRoom()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortRoom()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            fillAllRentRoomValues(imageUri, PaymentPeriod.PER_DAY)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomExpandedToolbarTitle() }
            collapseAppBar()
            checkRentRoomFormIsFull(imageUri, PaymentPeriod.PER_DAY)
        }
    }

    @Test
    fun fillSellHouseFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressHouse()
            registerGetNearAuroraAddressHouse()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellHouse()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            fillAllSellHouseValues(imageUri)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseExpandedToolbarTitle() }
            collapseAppBar()
            checkSellHouseFormIsFull(imageUri)
        }
    }

    @Test
    fun fillRentLongHouseFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressHouse()
            registerGetNearAuroraAddressHouse()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongHouse()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            fillAllRentHouseValues(imageUri, PaymentPeriod.PER_MONTH)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseExpandedToolbarTitle() }
            collapseAppBar()
            checkRentHouseFormIsFull(imageUri, PaymentPeriod.PER_MONTH)
        }
    }

    @Test
    fun fillRentShortHouseFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressHouse()
            registerGetNearAuroraAddressHouse()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortHouse()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            fillAllRentHouseValues(imageUri, PaymentPeriod.PER_DAY)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseExpandedToolbarTitle() }
            collapseAppBar()
            checkRentHouseFormIsFull(imageUri, PaymentPeriod.PER_DAY)
        }
    }

    @Test
    fun fillSellLotFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressLot()
            registerGetNearAuroraAddressLot()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellLot()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellLotExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            fillAllSellLotValues(imageUri)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellLotExpandedToolbarTitle() }
            collapseAppBar()
            checkSellLotFormIsFull(imageUri)
        }
    }

    @Test
    fun fillSellGarageFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressGarage()
            registerGetNearAuroraAddressGarage()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellGarage()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            fillAllSellGarageValues(imageUri)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            checkSellGarageFormIsFull(imageUri)
        }
    }

    @Test
    fun fillRentLongGarageFormThenPublishLaterAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressGarage()
            registerGetNearAuroraAddressGarage()
            registerUserProfile()
            registerUserProfile()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongGarage()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongGarageCollapsedToolbarTitle() }
            fillAllRentGarageValues(imageUri)
            scrollToPosition(lookup.matchesPublishLaterButton()).tapOn()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageExpandedToolbarTitle() }
            collapseAppBar()
            checkRentGarageFormIsFull(imageUri)
        }
    }

    @Test
    fun fillFormThenCloseAndOpenDraft() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
            registerGetNearAuroraAddressApartment()
            registerUserProfile()
            registerUserProfile()
        }

        prepareEmptySellApartmentForm()

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            fillAllSellApartmentValues(imageUri)
            pressBack()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            checkSellApartmentFormIsFull(imageUri)
        }
    }

    @Test
    fun fillFormAndTurnOffImagesOrderThenCloseAndOpenDraft() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUserProfile()
            registerUserProfile()
        }

        prepareEmptySellApartmentForm()

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            pressBack()
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            containsImagesOrderChangeAllowedField(false)
        }
    }

    @Test
    fun fillFormThenCloseAndStartNewForm() {
        val imageUri = createImageAndGetUriString()
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
            registerGetNearAuroraAddressApartment()
            registerUserProfile()
            registerUserProfile()
        }

        prepareEmptySellApartmentForm()

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            fillAllSellApartmentValues(imageUri)
            pressBack()
        }

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            onScreen<AddOfferModeDialogScreen> {
                titleView.waitUntil { isCompletelyDisplayed() }
                newOfferButton.click()
            }
        }

        performOnPublicationWizardScreen {
            waitUntil { isExpandedToolbarTitleEquals(getResourceString(R.string.add_offer_title)) }
        }
    }

    private fun prepareEmptySellApartmentForm() {
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellApartment()
            onScreen<AddressRootScreen> {
                mapView.waitUntil { isCompletelyDisplayed() }
                closeButton.click()
            }
        }
    }

    private fun selectContinueDraft() {
        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            onScreen<AddOfferModeDialogScreen> {
                titleView.waitUntil { isCompletelyDisplayed() }
                draftButton.click()
            }
        }
    }
}
