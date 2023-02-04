package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.*
import com.yandex.mobile.realty.core.rule.*
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 11/01/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class WizardTest : BasePublishFormTest() {

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
    fun checkSellSteps() {
        prepareApartmentWizard()

        performOnPublicationWizardScreen {
            waitTypeStep()
            isContentMatches("/WizardTest/checkSellStep/typeContent")
            selectSell()

            waitPropertyStep()
            isContentMatches("/WizardTest/checkSellStep/propertyContent")
        }
    }

    @Test
    fun checkRentLongSteps() {
        prepareApartmentWizard()

        performOnPublicationWizardScreen {
            waitTypeStep()
            selectRentLong()

            waitPropertyStep()
            isContentMatches("/WizardTest/checkRentLongStep/propertyContent")
        }
    }

    @Test
    fun checkRentShortSteps() {
        prepareApartmentWizard()

        performOnPublicationWizardScreen {
            waitTypeStep()
            selectRentShort()

            waitPropertyStep()
            isContentMatches("/WizardTest/checkRentShortStep/propertyContent")
        }
    }

    @Test
    fun checkSellApartmentSteps() {
        prepareApartmentWizard()

        performOnPublicationWizardScreen {
            selectSellApartment()
            selectAddress()

            waitFlatNumberStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/flatNumberContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            isContentMatches("/WizardTest/checkSellApartmentSteps/roomsTotalContent")
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitApartmentAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/apartmentAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/roomsAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/floorContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkSellApartmentSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDealStatusStep()
            isContentMatches("/WizardTest/checkSellApartmentSteps/dealStatusContent")
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkSellApartmentSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellApartmentSteps/contactsContent")
        }
    }

    @Test
    fun checkRentLongApartmentSteps() {
        prepareApartmentWizard()

        performOnPublicationWizardScreen {
            selectRentLongApartment()
            selectAddress()

            waitFlatNumberStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/flatNumberContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/roomsTotalContent")
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitApartmentAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/apartmentAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/roomsAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/floorContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongApartmentSteps/contactsContent")
        }
    }

    @Test
    fun checkRentShortApartmentSteps() {
        prepareApartmentWizard()

        performOnPublicationWizardScreen {
            selectRentShortApartment()
            selectAddress()

            waitFlatNumberStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/flatNumberContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/roomsTotalContent")
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitApartmentAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/apartmentAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/roomsAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/floorContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortApartmentSteps/contactsContent")
        }
    }

    @Test
    fun checkSellRoomSteps() {
        prepareRoomWizard()

        performOnPublicationWizardScreen {
            selectSellRoom()
            selectAddress()

            waitFlatNumberStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/flatNumberContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            isContentMatches("/WizardTest/checkSellRoomSteps/roomsTotalContent")
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitSellRoomsOfferedStep()
            isContentMatches("/WizardTest/checkSellRoomSteps/roomsOfferedContent")
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesWizardRoomsOfferedSelectorSix()).tapOn()

            waitApartmentAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/apartmentAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/roomsAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/floorContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkSellRoomSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDealStatusStep()
            isContentMatches("/WizardTest/checkSellRoomSteps/dealStatusContent")
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkSellRoomSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellRoomSteps/contactsContent")
        }
    }

    @Test
    fun checkRentLongRoomSteps() {
        prepareRoomWizard()

        performOnPublicationWizardScreen {
            selectRentLongRoom()
            selectAddress()

            waitFlatNumberStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/flatNumberContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/roomsTotalContent")
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitRentRoomsOfferedStep()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/roomsOfferedContent")
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesWizardRoomsOfferedSelectorSix()).tapOn()

            waitApartmentAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/apartmentAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/roomsAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/floorContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongRoomSteps/contactsContent")
        }
    }

    @Test
    fun checkRentShortRoomSteps() {
        prepareRoomWizard()

        performOnPublicationWizardScreen {
            selectRentShortRoom()
            selectAddress()

            waitFlatNumberStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/flatNumberContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/roomsTotalContent")
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitRentRoomsOfferedStep()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/roomsOfferedContent")
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesWizardRoomsOfferedSelectorSix()).tapOn()

            waitApartmentAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/apartmentAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/roomsAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/floorContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortRoomSteps/contactsContent")
        }
    }

    @Test
    fun checkSellHouseSteps() {
        prepareHouseWizard()

        performOnPublicationWizardScreen {
            selectSellHouse()
            selectAddress()

            waitHouseAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellHouseSteps/houseAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitLotAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellHouseSteps/lotAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkSellHouseSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellHouseSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDealStatusStep()
            isContentMatches("/WizardTest/checkSellHouseSteps/dealStatusContent")
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellHouseSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkSellHouseSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellHouseSteps/contactsContent")
        }
    }

    @Test
    fun checkRentLongHouseSteps() {
        prepareHouseWizard()

        performOnPublicationWizardScreen {
            selectRentLongHouse()
            selectAddress()

            waitHouseAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/houseAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitLotAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/lotAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongHouseSteps/contactsContent")
        }
    }

    @Test
    fun checkRentShortHouseSteps() {
        prepareHouseWizard()

        performOnPublicationWizardScreen {
            selectRentShortHouse()
            selectAddress()

            waitHouseAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/houseAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitLotAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/lotAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentShortHouseSteps/contactsContent")
        }
    }

    @Test
    fun checkSellLotSteps() {
        prepareLotWizard()

        performOnPublicationWizardScreen {
            selectSellLot()
            selectAddress()

            waitLotAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellLotSteps/lotAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            isContentMatches("/WizardTest/checkSellLotSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellLotSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDealStatusStep()
            isContentMatches("/WizardTest/checkSellLotSteps/dealStatusContent")
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellLotSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkSellLotSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellLotSteps/contactsContent")
        }
    }

    @Test
    fun checkSellGarageSteps() {
        prepareGarageWizard()

        performOnPublicationWizardScreen {
            selectSellGarage()
            selectAddress()

            waitGarageTypeStep()
            isContentMatches("/WizardTest/checkSellGarageSteps/garageTypeContent")
            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorParkingPlace()).tapOn()

            waitGarageAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellGarageSteps/garageAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitGarageOwnershipStep()
            isContentMatches("/WizardTest/checkSellGarageSteps/garageOwnershipContent")
            scrollToPosition(lookup.matchesGarageOwnershipField())
            onView(lookup.matchesGarageOwnershipSelectorCooperative()).tapOn()

            waitPhotosStep()
            isContentMatches("/WizardTest/checkSellGarageSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellGarageSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellGarageSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkSellGarageSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkSellGarageSteps/contactsContent")
        }
    }

    @Test
    fun checkRentLongGarageSteps() {
        prepareGarageWizard()

        performOnPublicationWizardScreen {
            selectRentLongGarage()
            selectAddress()

            waitGarageTypeStep()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/garageTypeContent")
            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorParkingPlace()).tapOn()

            waitGarageAreaStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/garageAreaContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitGarageOwnershipStep()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/garageOwnershipContent")
            scrollToPosition(lookup.matchesGarageOwnershipField())
            onView(lookup.matchesGarageOwnershipSelectorCooperative()).tapOn()

            waitPhotosStep()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/photosContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/priceContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitDescriptionStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/descriptionContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/phonesContent")
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            clearFocus()
            isContentMatches("/WizardTest/checkRentLongGarageSteps/contactsContent")
        }
    }

    @Test
    fun pressBackOnEmptyWizard() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerUserProfile()
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            selectSellApartment()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                pressBack()
            }

            waitPropertyStep()
            pressBack()

            waitTypeStep()
            pressBack()
        }

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            waitTypeStep()
        }
    }

    @Test
    fun pressBackOnNotEmptyWizardThenDiscardChanges() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetAuroraAddressApartment()
            registerGetNearAuroraAddressApartment()
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            selectSellApartment()
            selectAddress()

            waitFlatNumberStep()
            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            pressBack()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
                pressBack()
                performOnConfirmationDialog {
                    waitUntil { isDiscardPublishFromChangesDialogContentShown() }
                    confirm()
                }
            }

            waitPropertyStep()
            selectApartment()
            selectAddress()

            waitFlatNumberStep()
            containsApartmentNumberField(text = "")
        }
    }

    @Test
    fun pressBackOnNotEmptyWizardThenCancelDiscardChanges() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetAuroraAddressApartment()
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            selectSellApartment()
            selectAddress()

            waitFlatNumberStep()
            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            pressBack()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                pressBack()
                performOnConfirmationDialog {
                    waitUntil { isDiscardPublishFromChangesDialogContentShown() }
                    tapOn(lookup.matchesNegativeButton())
                }
                tapOn(lookup.matchesConfirmAddressButton())
            }

            waitFlatNumberStep()
            containsApartmentNumberField(APARTMENT_NUMBER)
        }
    }

    @Test
    fun fillStepsThenGoBackAndForward() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            selectSellApartment()
            selectAddress()

            waitFlatNumberStep()
            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitApartmentAreaStep()
            scrollToPosition(lookup.matchesWizardTotalAreaField())
            onView(lookup.matchesWizardTotalAreaFieldValue()).typeText(TOTAL_AREA)
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            pressBack()

            waitApartmentAreaStep()
            clearFocus()
            pressBack()

            waitRoomsTotalStep()
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitApartmentAreaStep()
            containsTotalAreaField(TOTAL_AREA)
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            containsRoom1AreaField(ROOM_1_AREA)
        }
    }

    @Test
    fun shouldSkipRoomsOfferedStepWhenTwoRoomsInApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressRoom()
            registerUserProfile()
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            selectSellRoom()
            selectAddress()

            waitFlatNumberStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorTwo()).tapOn()

            waitApartmentAreaStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitDealStatusStep()
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()

            waitDescriptionStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            tapOn(lookup.matchesWizardProceedButton())
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()

            containsRoomsOfferedField(
                lookup.matchesRoomsOfferedSelectorOne(),
                lookup.matchesRoomsOfferedSelectorOne()
            )

            containsRoom1AreaField(ROOM_1_AREA)
        }
    }

    @Test
    fun shouldSkipRoomsOfferedStepAfterChangeRoomsCountToTwo() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressRoom()
            registerUserProfile()
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }

        performOnPublicationWizardScreen {
            selectSellRoom()
            selectAddress()

            waitFlatNumberStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsTotalStep()
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

            waitSellRoomsOfferedStep()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesWizardRoomsOfferedSelectorSix()).tapOn()

            waitApartmentAreaStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            pressBack()

            waitApartmentAreaStep()
            clearFocus()
            pressBack()

            waitSellRoomsOfferedStep()
            pressBack()

            waitRoomsTotalStep()
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesWizardRoomsSelectorTwo()).tapOn()

            waitApartmentAreaStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitRoomsAreaStep()
            containsRoom1AreaField(ROOM_1_AREA)
            tapOn(lookup.matchesWizardProceedButton())

            waitFloorStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitPhotosStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitPriceStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitDealStatusStep()
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()

            waitDescriptionStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitPhonesStep()
            tapOn(lookup.matchesWizardProceedButton())

            waitContactsStep()
            tapOn(lookup.matchesWizardProceedButton())
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()

            containsRoomsOfferedField(
                lookup.matchesRoomsOfferedSelectorOne(),
                lookup.matchesRoomsOfferedSelectorOne()
            )

            containsRoom1AreaField(ROOM_1_AREA)
        }
    }

    private fun ConfirmationDialogRobot.isDiscardPublishFromChangesDialogContentShown() {
        isTitleEquals(getResourceString(R.string.reject_changes_confirmation_dialog_title))
        isMessageEquals(getResourceString(R.string.reject_changes_confirmation_dialog_message))
        isNegativeButtonTextEquals(getResourceString(R.string.no))
        isPositiveButtonTextEquals(getResourceString(R.string.yes))
    }

    private fun prepareApartmentWizard() {
        prepareWizard { dispatcher -> dispatcher.registerGetNearAuroraAddressApartment() }
    }

    private fun prepareRoomWizard() {
        prepareWizard { dispatcher -> dispatcher.registerGetNearAuroraAddressRoom() }
    }

    private fun prepareHouseWizard() {
        prepareWizard { dispatcher -> dispatcher.registerGetNearAuroraAddressHouse() }
    }

    private fun prepareLotWizard() {
        prepareWizard { dispatcher -> dispatcher.registerGetNearAuroraAddressLot() }
    }

    private fun prepareGarageWizard() {
        prepareWizard { dispatcher -> dispatcher.registerGetNearAuroraAddressGarage() }
    }

    private fun prepareWizard(configureAddress: (DispatcherRegistry) -> Unit) {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            configureAddress(this)
        }

        activityTestRule.launchActivity()

        performOnUserOffersScreen {
            waitUntil { isAddOfferActionShown() }
            tapOn(lookup.matchesToolbarAddOffersAction())
        }
    }

    private fun selectAddress() {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }
    }
}
