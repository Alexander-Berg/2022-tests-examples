package com.yandex.mobile.realty.test.publicationForm

import androidx.annotation.StringRes
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.matchers.PublicationFormLookup
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.robot.*
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest

/**
 * @author solovevai on 19.07.2020.
 */
abstract class BasePublishFormTest : BaseTest() {

    enum class PaymentPeriod { PER_DAY, PER_MONTH }

    protected fun PublicationWizardRobot.selectSellApartment() {
        waitTypeStep()
        selectSell()
        waitPropertyStep()
        selectApartment()
    }

    protected fun PublicationWizardRobot.selectRentLongApartment() {
        waitTypeStep()
        selectRentLong()
        waitPropertyStep()
        selectApartment()
    }

    protected fun PublicationWizardRobot.selectRentShortApartment() {
        waitTypeStep()
        selectRentShort()
        waitPropertyStep()
        selectApartment()
    }

    protected fun PublicationWizardRobot.selectSellRoom() {
        waitTypeStep()
        selectSell()
        waitPropertyStep()
        selectRoom()
    }

    protected fun PublicationWizardRobot.selectRentLongRoom() {
        waitTypeStep()
        selectRentLong()
        waitPropertyStep()
        selectRoom()
    }

    protected fun PublicationWizardRobot.selectRentShortRoom() {
        waitTypeStep()
        selectRentShort()
        waitPropertyStep()
        selectRoom()
    }

    protected fun PublicationWizardRobot.selectSellHouse() {
        waitTypeStep()
        selectSell()
        waitPropertyStep()
        selectHouse()
    }

    protected fun PublicationWizardRobot.selectRentLongHouse() {
        waitTypeStep()
        selectRentLong()
        waitPropertyStep()
        selectHouse()
    }

    protected fun PublicationWizardRobot.selectRentShortHouse() {
        waitTypeStep()
        selectRentShort()
        waitPropertyStep()
        selectHouse()
    }

    protected fun PublicationWizardRobot.selectSellLot() {
        waitTypeStep()
        selectSell()
        waitPropertyStep()
        selectLot()
    }

    protected fun PublicationWizardRobot.selectSellGarage() {
        waitTypeStep()
        selectSell()
        waitPropertyStep()
        selectGarage()
    }

    protected fun PublicationWizardRobot.selectRentLongGarage() {
        waitTypeStep()
        selectRentLong()
        waitPropertyStep()
        selectGarage()
    }

    protected fun PublicationWizardRobot.waitTypeStep() {
        waitCollapsedToolbar(R.string.add_offer_title)
    }

    protected fun PublicationWizardRobot.waitPropertyStep() {
        waitCollapsedToolbar(R.string.add_offer_property_title)
    }

    protected fun PublicationWizardRobot.waitFlatNumberStep() {
        waitCollapsedToolbar(R.string.add_offer_apartment)
    }

    protected fun PublicationWizardRobot.waitRoomsTotalStep() {
        waitCollapsedToolbar(R.string.wizard_rooms_total_title)
    }

    protected fun PublicationWizardRobot.waitApartmentAreaStep() {
        waitCollapsedToolbar(R.string.wizard_apartment_area_title)
    }

    protected fun PublicationWizardRobot.waitRoomsAreaStep() {
        waitCollapsedToolbar(R.string.wizard_rooms_area_title)
    }

    protected fun PublicationWizardRobot.waitFloorStep() {
        waitCollapsedToolbar(R.string.wizard_floor_title)
    }

    protected fun PublicationWizardRobot.waitHouseAreaStep() {
        waitCollapsedToolbar(R.string.wizard_house_area_title)
    }

    protected fun PublicationWizardRobot.waitLotAreaStep() {
        waitCollapsedToolbar(R.string.wizard_lot_area_title)
    }

    protected fun PublicationWizardRobot.waitGarageTypeStep() {
        waitCollapsedToolbar(R.string.add_offer_garage_type)
    }

    protected fun PublicationWizardRobot.waitGarageOwnershipStep() {
        waitCollapsedToolbar(R.string.add_offer_garage_ownership)
    }

    protected fun PublicationWizardRobot.waitGarageAreaStep() {
        waitCollapsedToolbar(R.string.add_offer_garage_area)
    }

    protected fun PublicationWizardRobot.waitPhotosStep() {
        waitCollapsedToolbar(R.string.photos)
    }

    protected fun PublicationWizardRobot.waitPriceStep() {
        waitCollapsedToolbar(R.string.wizard_price_title)
    }

    protected fun PublicationWizardRobot.waitDealStatusStep() {
        waitCollapsedToolbar(R.string.wizard_deal_status_title)
    }

    protected fun PublicationWizardRobot.waitDescriptionStep() {
        waitCollapsedToolbar(R.string.add_offer_description_title)
    }

    protected fun PublicationWizardRobot.waitPhonesStep() {
        waitCollapsedToolbar(R.string.wizard_phone_title)
    }

    protected fun PublicationWizardRobot.waitNewPhoneStep() {
        waitCollapsedToolbar(R.string.add_phone_new_phone)
    }

    protected fun PublicationWizardRobot.waitConfirmationCodeStep() {
        waitCollapsedToolbar(R.string.add_phone_confirmation_code)
    }

    protected fun PublicationWizardRobot.waitContactsStep() {
        waitCollapsedToolbar(R.string.add_offer_contacts_section_title)
    }

    protected fun PublicationWizardRobot.waitSellRoomsOfferedStep() {
        waitCollapsedToolbar(R.string.wizard_sell_rooms_offered_title)
    }

    protected fun PublicationWizardRobot.waitRentRoomsOfferedStep() {
        waitCollapsedToolbar(R.string.wizard_rent_rooms_offered_title)
    }

    private fun PublicationWizardRobot.waitCollapsedToolbar(@StringRes titleRes: Int) {
        val title = getResourceString(titleRes)
        waitUntil { isExpandedToolbarTitleEquals(title) }
        collapseAppBar()
        waitUntil { isCollapsedToolbarTitleEquals(title) }
    }

    protected fun PublicationFormRobot.pickAddress() {
        scrollToPosition(lookup.matchesAddressField())
        tapOn(lookup.matchesAddressField())
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }
    }

    protected fun PublicationFormRobot.pickImage(uri: String) {
        scrollToPosition(lookup.matchesAddImagesButton())
        tapOn(lookup.matchesAddImagesButton())
        performOnImagesPickerScreen {
            registerGetContentIntent(uri)
            waitUntil { isLargeAddImageButtonShown() }
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, uri) }
            tapOn(lookup.matchesDoneButton())
        }
    }

    protected fun PublicationFormRobot.pickDescription() {
        scrollToPosition(lookup.matchesDescriptionField())
        tapOn(lookup.matchesDescriptionField())
        performOnDescriptionInputScreen {
            typeText(lookup.matchesInputView(), DESCRIPTION)
            tapOn(lookup.matchesDoneButton())
        }
    }

    protected fun PublicationFormRobot.fillAllSellApartmentValues(
        imageUri: String,
        withWizardFields: Boolean = true
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesTotalAreaField())
            onView(lookup.matchesTotalAreaFieldValue()).typeText(TOTAL_AREA)
            scrollToPosition(lookup.matchesLivingAreaField())
            onView(lookup.matchesLivingAreaFieldValue()).typeText(LIVING_AREA)
            scrollToPosition(lookup.matchesKitchenAreaField())
            onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            scrollToPosition((lookup.matchesRoom2AreaField()))
            onView(lookup.matchesRoom2AreaFieldValue()).typeText(ROOM_2_AREA)
            scrollToPosition((lookup.matchesRoom3AreaField()))
            onView(lookup.matchesRoom3AreaFieldValue()).typeText(ROOM_3_AREA)
            scrollToPosition((lookup.matchesRoom4AreaField()))
            onView(lookup.matchesRoom4AreaFieldValue()).typeText(ROOM_4_AREA)
            scrollToPosition((lookup.matchesRoom5AreaField()))
            onView(lookup.matchesRoom5AreaFieldValue()).typeText(ROOM_5_AREA)
            scrollToPosition((lookup.matchesRoom6AreaField()))
            onView(lookup.matchesRoom6AreaFieldValue()).typeText(ROOM_6_AREA)
            scrollToPosition((lookup.matchesRoom7AreaField()))
            onView(lookup.matchesRoom7AreaFieldValue()).typeText(ROOM_7_AREA)
            scrollToPosition(lookup.matchesFloorField())
            onView(lookup.matchesFloorFieldValue()).typeText(FLOOR)
            scrollToPosition(lookup.matchesFloorsTotalField())
            onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
            scrollToPosition(lookup.matchesPriceField())
            onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()
        }

        scrollToPosition(lookup.matchesBathroomField())
        onView(lookup.matchesBathroomSelectorMatched()).tapOn()
        scrollToPosition(lookup.matchesBalconyField())
        onView(lookup.matchesBalconySelectorBalcony()).tapOn()
        scrollToPosition(lookup.matchesRenovationField())
        onView(lookup.matchesRenovationSelectorEuro()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorStreet()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorYard()).tapOn()
        scrollToPosition(lookup.matchesPropertyStatusField())
        onView(lookup.matchesPropertyStatusSelectorHousingStock()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)

        scrollToPosition(lookup.matchesHouseReadyStatusField())
        onView(lookup.matchesHouseReadyStatusSelectorSecondary()).tapOn()
        scrollToPosition(lookup.matchesBuildingYearField())
        onView(lookup.matchesBuildingYearFieldValue()).typeText(BUILD_YEAR)
        scrollToPosition(lookup.matchesCeilingHeightField())
        onView(lookup.matchesCeilingHeightFieldValue()).typeText(CEILING_HEIGHT)
        scrollToPosition(lookup.matchesParkingTypeField())
        onView(lookup.matchesParkingTypeSelectorOpen()).tapOn()
        scrollToPosition(lookup.matchesBuildingTypeField())
        onView(lookup.matchesBuildingTypeSelectorMonolitBrick()).tapOn()

        scrollToPosition(lookup.matchesFacilitiesSelectorInternet()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRefrigerator()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorKitchenFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAircondition()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRoomFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorLift()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRubbishChute()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorConcierge()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPassBy()).tapOn()
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorMortgage()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllRentApartmentValues(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
        withWizardFields: Boolean = true,
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesTotalAreaField())
            onView(lookup.matchesTotalAreaFieldValue()).typeText(TOTAL_AREA)
            scrollToPosition(lookup.matchesLivingAreaField())
            onView(lookup.matchesLivingAreaFieldValue()).typeText(LIVING_AREA)
            scrollToPosition(lookup.matchesKitchenAreaField())
            onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            scrollToPosition((lookup.matchesRoom2AreaField()))
            onView(lookup.matchesRoom2AreaFieldValue()).typeText(ROOM_2_AREA)
            scrollToPosition((lookup.matchesRoom3AreaField()))
            onView(lookup.matchesRoom3AreaFieldValue()).typeText(ROOM_3_AREA)
            scrollToPosition((lookup.matchesRoom4AreaField()))
            onView(lookup.matchesRoom4AreaFieldValue()).typeText(ROOM_4_AREA)
            scrollToPosition((lookup.matchesRoom5AreaField()))
            onView(lookup.matchesRoom5AreaFieldValue()).typeText(ROOM_5_AREA)
            scrollToPosition((lookup.matchesRoom6AreaField()))
            onView(lookup.matchesRoom6AreaFieldValue()).typeText(ROOM_6_AREA)
            scrollToPosition((lookup.matchesRoom7AreaField()))
            onView(lookup.matchesRoom7AreaFieldValue()).typeText(ROOM_7_AREA)
            scrollToPosition(lookup.matchesFloorField())
            onView(lookup.matchesFloorFieldValue()).typeText(FLOOR)
            scrollToPosition(lookup.matchesFloorsTotalField())
            onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
            scrollToPosition(lookup.matchesPriceField(paymentPeriod))
            onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
        }

        scrollToPosition(lookup.matchesBathroomField())
        onView(lookup.matchesBathroomSelectorMatched()).tapOn()
        scrollToPosition(lookup.matchesBalconyField())
        onView(lookup.matchesBalconySelectorBalcony()).tapOn()
        scrollToPosition(lookup.matchesRenovationField())
        onView(lookup.matchesRenovationSelectorEuro()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorStreet()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorYard()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)

        scrollToPosition(lookup.matchesBuildingYearField())
        onView(lookup.matchesBuildingYearFieldValue()).typeText(BUILD_YEAR)
        scrollToPosition(lookup.matchesCeilingHeightField())
        onView(lookup.matchesCeilingHeightFieldValue()).typeText(CEILING_HEIGHT)
        scrollToPosition(lookup.matchesParkingTypeField())
        onView(lookup.matchesParkingTypeSelectorOpen()).tapOn()

        scrollToPosition(lookup.matchesFacilitiesSelectorInternet()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRefrigerator()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorKitchenFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAircondition()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRoomFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorLift()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRubbishChute()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorConcierge()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWashingMachine()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPassBy()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorDishwasher()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorTelevision()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWithChildren()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWithPets()).tapOn()

        scrollToPosition(lookup.matchesPrepaymentField())
        onView(lookup.matchesPrepaymentFieldValue()).typeText(PREPAYMENT)
        scrollToPosition(lookup.matchesAgentFeeField())
        onView(lookup.matchesAgentFeeFieldValue()).typeText(AGENT_FEE)

        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorRentPledge()).tapOn()
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorUtilitiesIncluded()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllSellRoomValues(
        imageUri: String,
        withWizardFields: Boolean = true
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesTotalAreaField())
            onView(lookup.matchesTotalAreaFieldValue()).typeText(TOTAL_AREA)
            scrollToPosition(lookup.matchesKitchenAreaField())
            onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            scrollToPosition((lookup.matchesRoom2AreaField()))
            onView(lookup.matchesRoom2AreaFieldValue()).typeText(ROOM_2_AREA)
            scrollToPosition((lookup.matchesRoom3AreaField()))
            onView(lookup.matchesRoom3AreaFieldValue()).typeText(ROOM_3_AREA)
            scrollToPosition((lookup.matchesRoom4AreaField()))
            onView(lookup.matchesRoom4AreaFieldValue()).typeText(ROOM_4_AREA)
            scrollToPosition((lookup.matchesRoom5AreaField()))
            onView(lookup.matchesRoom5AreaFieldValue()).typeText(ROOM_5_AREA)
            scrollToPosition((lookup.matchesRoom6AreaField()))
            onView(lookup.matchesRoom6AreaFieldValue()).typeText(ROOM_6_AREA)
            scrollToPosition(lookup.matchesFloorField())
            onView(lookup.matchesFloorFieldValue()).typeText(FLOOR)
            scrollToPosition(lookup.matchesFloorsTotalField())
            onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
            scrollToPosition(lookup.matchesPriceField())
            onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()
        }

        scrollToPosition(lookup.matchesBathroomField())
        onView(lookup.matchesBathroomSelectorMatched()).tapOn()
        scrollToPosition(lookup.matchesBalconyField())
        onView(lookup.matchesBalconySelectorBalcony()).tapOn()
        scrollToPosition(lookup.matchesFloorCoveringField())
        onView(lookup.matchesFloorCoveringSelectorLinoleum()).tapOn()
        scrollToPosition(lookup.matchesRenovationField())
        onView(lookup.matchesRenovationSelectorEuro()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorStreet()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorYard()).tapOn()
        scrollToPosition(lookup.matchesPropertyStatusField())
        onView(lookup.matchesPropertyStatusSelectorHousingStock()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)

        scrollToPosition(lookup.matchesBuildingYearField())
        onView(lookup.matchesBuildingYearFieldValue()).typeText(BUILD_YEAR)
        scrollToPosition(lookup.matchesCeilingHeightField())
        onView(lookup.matchesCeilingHeightFieldValue()).typeText(CEILING_HEIGHT)
        scrollToPosition(lookup.matchesParkingTypeField())
        onView(lookup.matchesParkingTypeSelectorOpen()).tapOn()
        scrollToPosition(lookup.matchesBuildingTypeField())
        onView(lookup.matchesBuildingTypeSelectorMonolitBrick()).tapOn()

        scrollToPosition(lookup.matchesFacilitiesSelectorInternet()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorKitchenFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRoomFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorLift()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRubbishChute()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorConcierge()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPassBy()).tapOn()

        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorMortgage()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllRentRoomValues(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
        withWizardFields: Boolean = true,
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesApartmentNumberField())
            onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesTotalAreaField())
            onView(lookup.matchesTotalAreaFieldValue()).typeText(TOTAL_AREA)
            scrollToPosition(lookup.matchesKitchenAreaField())
            onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
            scrollToPosition((lookup.matchesRoom1AreaField()))
            onView(lookup.matchesRoom1AreaFieldValue()).typeText(ROOM_1_AREA)
            scrollToPosition((lookup.matchesRoom2AreaField()))
            onView(lookup.matchesRoom2AreaFieldValue()).typeText(ROOM_2_AREA)
            scrollToPosition((lookup.matchesRoom3AreaField()))
            onView(lookup.matchesRoom3AreaFieldValue()).typeText(ROOM_3_AREA)
            scrollToPosition((lookup.matchesRoom4AreaField()))
            onView(lookup.matchesRoom4AreaFieldValue()).typeText(ROOM_4_AREA)
            scrollToPosition((lookup.matchesRoom5AreaField()))
            onView(lookup.matchesRoom5AreaFieldValue()).typeText(ROOM_5_AREA)
            scrollToPosition((lookup.matchesRoom6AreaField()))
            onView(lookup.matchesRoom6AreaFieldValue()).typeText(ROOM_6_AREA)
            scrollToPosition(lookup.matchesFloorField())
            onView(lookup.matchesFloorFieldValue()).typeText(FLOOR)
            scrollToPosition(lookup.matchesFloorsTotalField())
            onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
            scrollToPosition(lookup.matchesPriceField(paymentPeriod))
            onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
        }

        scrollToPosition(lookup.matchesBathroomField())
        onView(lookup.matchesBathroomSelectorMatched()).tapOn()
        scrollToPosition(lookup.matchesBalconyField())
        onView(lookup.matchesBalconySelectorBalcony()).tapOn()
        scrollToPosition(lookup.matchesFloorCoveringField())
        onView(lookup.matchesFloorCoveringSelectorLinoleum()).tapOn()
        scrollToPosition(lookup.matchesRenovationField())
        onView(lookup.matchesRenovationSelectorEuro()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorStreet()).tapOn()
        scrollToPosition(lookup.matchesWindowsField())
        onView(lookup.matchesWindowsSelectorYard()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)

        scrollToPosition(lookup.matchesBuildingYearField())
        onView(lookup.matchesBuildingYearFieldValue()).typeText(BUILD_YEAR)
        scrollToPosition(lookup.matchesCeilingHeightField())
        onView(lookup.matchesCeilingHeightFieldValue()).typeText(CEILING_HEIGHT)
        scrollToPosition(lookup.matchesParkingTypeField())
        onView(lookup.matchesParkingTypeSelectorOpen()).tapOn()

        scrollToPosition(lookup.matchesFacilitiesSelectorInternet()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRefrigerator()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorKitchenFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAircondition()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRoomFurniture()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorLift()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorRubbishChute()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorConcierge()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWashingMachine()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPassBy()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorDishwasher()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorTelevision()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWithChildren()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWithPets()).tapOn()

        scrollToPosition(lookup.matchesPrepaymentField())
        onView(lookup.matchesPrepaymentFieldValue()).typeText(PREPAYMENT)
        scrollToPosition(lookup.matchesAgentFeeField())
        onView(lookup.matchesAgentFeeFieldValue()).typeText(AGENT_FEE)

        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorRentPledge()).tapOn()
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorUtilitiesIncluded()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllSellHouseValues(
        imageUri: String,
        withWizardFields: Boolean = true
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesHouseAreaField())
            onView(lookup.matchesHouseAreaFieldValue()).typeText(HOUSE_AREA)
            scrollToPosition(lookup.matchesLotAreaUnitField())
            onView(lookup.matchesLotAreaUnitSelectorHectare()).tapOn()
            scrollToPosition(lookup.matchesLotAreaField())
            onView(lookup.matchesLotAreaFieldValue()).typeText(LOT_AREA)
            scrollToPosition(lookup.matchesPriceField())
            onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()
        }

        scrollToPosition(lookup.matchesHouseFloorsField())
        onView(lookup.matchesHouseFloorsFieldValue()).typeText(HOUSE_FLOORS)
        scrollToPosition(lookup.matchesHouseTypeField())
        onView(lookup.matchesHouseTypeSelectorTownhouse()).tapOn()
        scrollToPosition(lookup.matchesHouseBuildingTypeField())
        onView(lookup.matchesHouseBuildingTypeSelectorWood()).tapOn()
        scrollToPosition(lookup.matchesToiletField())
        onView(lookup.matchesToiletSelectorInside()).tapOn()
        scrollToPosition(lookup.matchesShowerField())
        onView(lookup.matchesShowerSelectorInside()).tapOn()
        scrollToPosition(lookup.matchesLotTypeField())
        onView(lookup.matchesLotTypeSelectorGarden()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)

        scrollToPosition(lookup.matchesFacilitiesSelectorSewerageSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorElectricitySupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorGasSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorBilliard()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorSauna()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPool()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPMG()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorKitchen()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorHeatingSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWaterSupply()).tapOn()

        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorMortgage()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllRentHouseValues(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
        withWizardFields: Boolean = true,
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesHouseAreaField())
            onView(lookup.matchesHouseAreaFieldValue()).typeText(HOUSE_AREA)
            scrollToPosition(lookup.matchesLotAreaUnitField())
            onView(lookup.matchesLotAreaUnitSelectorHectare()).tapOn()
            scrollToPosition(lookup.matchesLotAreaField())
            onView(lookup.matchesLotAreaFieldValue()).typeText(LOT_AREA)
            scrollToPosition(lookup.matchesPriceField(paymentPeriod))
            onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
        }

        scrollToPosition(lookup.matchesHouseFloorsField())
        onView(lookup.matchesHouseFloorsFieldValue()).typeText(HOUSE_FLOORS)
        scrollToPosition(lookup.matchesHouseTypeField())
        onView(lookup.matchesHouseTypeSelectorTownhouse()).tapOn()
        scrollToPosition(lookup.matchesHouseBuildingTypeField())
        onView(lookup.matchesHouseBuildingTypeSelectorWood()).tapOn()
        scrollToPosition(lookup.matchesToiletField())
        onView(lookup.matchesToiletSelectorInside()).tapOn()
        scrollToPosition(lookup.matchesShowerField())
        onView(lookup.matchesShowerSelectorInside()).tapOn()
        scrollToPosition(lookup.matchesLotTypeField())
        onView(lookup.matchesLotTypeSelectorGarden()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)

        scrollToPosition(lookup.matchesFacilitiesSelectorSewerageSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorElectricitySupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorGasSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorBilliard()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorSauna()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPool()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorPMG()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorKitchen()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorHeatingSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWaterSupply()).tapOn()

        scrollToPosition(lookup.matchesPrepaymentField())
        onView(lookup.matchesPrepaymentFieldValue()).typeText(PREPAYMENT)
        scrollToPosition(lookup.matchesAgentFeeField())
        onView(lookup.matchesAgentFeeFieldValue()).typeText(AGENT_FEE)

        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorRentPledge()).tapOn()
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorUtilitiesIncluded()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllSellLotValues(
        imageUri: String,
        withWizardFields: Boolean = true
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesLotAreaUnitField())
            onView(lookup.matchesLotAreaUnitSelectorHectare()).tapOn()
            scrollToPosition(lookup.matchesLotAreaField())
            onView(lookup.matchesLotAreaFieldValue()).typeText(LOT_AREA)
            scrollToPosition(lookup.matchesPriceField())
            onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
            scrollToPosition(lookup.matchesDealStatusField())
            onView(lookup.matchesDealStatusSelectorSale()).tapOn()
        }

        scrollToPosition(lookup.matchesLotTypeField())
        onView(lookup.matchesLotTypeSelectorGarden()).tapOn()
        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorMortgage()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllSellGarageValues(
        imageUri: String,
        withWizardFields: Boolean = true
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorBox()).tapOn()
            scrollToPosition(lookup.matchesGarageAreaField())
            onView(lookup.matchesGarageAreaFieldValue()).typeText(GARAGE_AREA)
            scrollToPosition(lookup.matchesGarageOwnershipField())
            onView(lookup.matchesGarageOwnershipSelectorCooperative()).tapOn()
            scrollToPosition(lookup.matchesPriceField())
            onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
        }

        scrollToPosition(lookup.matchesGarageParkingTypeField())
        onView(lookup.matchesGarageParkingTypeSelectorMultilevel()).tapOn()
        scrollToPosition(lookup.matchesGarageBuildingTypeField())
        onView(lookup.matchesGarageBuildingTypeSelectorFerroconcrete()).tapOn()
        scrollToPosition(lookup.matchesGarageNameField())
        onView(lookup.matchesGarageNameFieldValue()).typeText(GARAGE_NAME)

        scrollToPosition(lookup.matchesFacilitiesSelectorFireAlarm()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorTwentyFourSeven()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAccessControlSystem()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorHeatingSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWaterSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorElectricitySupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAutomaticGates()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorCctv()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorSecurity()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorInspectionPit()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorCarWash()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAutoRepair()).tapOn()

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.fillAllRentGarageValues(
        imageUri: String,
        withWizardFields: Boolean = true
    ) {
        if (withWizardFields) {
            pickAddress()
            pickImage(imageUri)
            pickDescription()

            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorBox()).tapOn()
            scrollToPosition(lookup.matchesGarageAreaField())
            onView(lookup.matchesGarageAreaFieldValue()).typeText(GARAGE_AREA)
            scrollToPosition(lookup.matchesGarageOwnershipField())
            onView(lookup.matchesGarageOwnershipSelectorCooperative()).tapOn()
            scrollToPosition(lookup.matchesPricePerMonthField())
            onView(lookup.matchesPricePerMonthFieldValue()).typeText(PRICE)
            scrollToPosition(lookup.matchesAdditionTermsField())
            onView(lookup.matchesAdditionTermsSelectorHaggle()).tapOn()
        }

        scrollToPosition(lookup.matchesGarageParkingTypeField())
        onView(lookup.matchesGarageParkingTypeSelectorMultilevel()).tapOn()
        scrollToPosition(lookup.matchesGarageBuildingTypeField())
        onView(lookup.matchesGarageBuildingTypeSelectorFerroconcrete()).tapOn()
        scrollToPosition(lookup.matchesGarageNameField())
        onView(lookup.matchesGarageNameFieldValue()).typeText(GARAGE_NAME)

        scrollToPosition(lookup.matchesFacilitiesSelectorFireAlarm()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorTwentyFourSeven()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAccessControlSystem()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorHeatingSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorWaterSupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorElectricitySupply()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAutomaticGates()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorCctv()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorSecurity()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorInspectionPit()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorCarWash()).tapOn()
        scrollToPosition(lookup.matchesFacilitiesSelectorAutoRepair()).tapOn()

        scrollToPosition(lookup.matchesPrepaymentField())
        onView(lookup.matchesPrepaymentFieldValue()).typeText(PREPAYMENT)
        scrollToPosition(lookup.matchesAgentFeeField())
        onView(lookup.matchesAgentFeeFieldValue()).typeText(AGENT_FEE)

        scrollToPosition(lookup.matchesVideoUrlField())
        onView(lookup.matchesVideoUrlFieldValue()).typeText(VIDEO_URL)
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorRentPledge()).tapOn()
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorUtilitiesIncluded()).tapOn()
        scrollToPosition(lookup.matchesAdditionTermsField())
        onView(lookup.matchesAdditionTermsSelectorElectricityIncluded()).tapOn()
        scrollToPosition(lookup.matchesOnlineShowField()).tapOn()
    }

    protected fun PublicationFormRobot.checkSellApartmentFormIsFull(imageUri: String) {
        containsAddressField(FULL_AURORA_ADDRESS)
        containsApartmentNumberField(APARTMENT_NUMBER)

        containsRoomsCountApartmentField(lookup.matchesRoomsSelectorSeven())

        containsTotalAreaField(TOTAL_AREA)
        containsLivingAreaField(LIVING_AREA)
        containsKitchenAreaField(KITCHEN_AREA)
        containsRoom1AreaField(ROOM_1_AREA)
        containsRoom2AreaField(ROOM_2_AREA)
        containsRoom3AreaField(ROOM_3_AREA)
        containsRoom4AreaField(ROOM_4_AREA)
        containsRoom5AreaField(ROOM_5_AREA)
        containsRoom6AreaField(ROOM_6_AREA)
        containsRoom7AreaField(ROOM_7_AREA)
        containsFloorField(FLOOR)
        containsBathroomField(lookup.matchesBathroomSelectorMatched())
        containsBalconyField(lookup.matchesBalconySelectorBalcony())
        containsRenovationField(lookup.matchesRenovationSelectorEuro())
        containsWindowsField(
            listOf(
                lookup.matchesWindowsSelectorStreet(),
                lookup.matchesWindowsSelectorYard()
            )
        )
        containsPropertyStatusField(lookup.matchesPropertyStatusSelectorHousingStock())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsHouseReadyStatusField(lookup.matchesHouseReadyStatusSelectorSecondary())
        containsBuildingYearField(BUILD_YEAR)
        containsCeilingHeightField(CEILING_HEIGHT)
        containsFloorsTotalField(TOTAL_FLOORS)
        containsParkingTypeField(lookup.matchesParkingTypeSelectorOpen())
        containsBuildingTypeFiled(lookup.matchesBuildingTypeSelectorMonolitBrick())

        containsDescriptionField(DESCRIPTION)
        containsInternetField(true)
        containsRefrigeratorField(true)
        containsKitchenFurnitureField(true)
        containsAirconditionField(true)
        containsRoomFurnitureField(true)
        containsLiftField(true)
        containsRubbishChuteField(true)
        containsConciergeField(true)
        containsPassByField(true)

        containsPriceField(PRICE_FORMATTED)
        containsSellAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorMortgage()
            )
        )
        containsDealStatusApartmentField(lookup.matchesDealStatusSelectorSale())
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkRentApartmentFormIsFull(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
    ) {
        containsAddressField(FULL_AURORA_ADDRESS)
        containsApartmentNumberField(APARTMENT_NUMBER)

        containsRoomsCountApartmentField(lookup.matchesRoomsSelectorSeven())

        containsTotalAreaField(TOTAL_AREA)
        containsLivingAreaField(LIVING_AREA)
        containsKitchenAreaField(KITCHEN_AREA)
        containsRoom1AreaField(ROOM_1_AREA)
        containsRoom2AreaField(ROOM_2_AREA)
        containsRoom3AreaField(ROOM_3_AREA)
        containsRoom4AreaField(ROOM_4_AREA)
        containsRoom5AreaField(ROOM_5_AREA)
        containsRoom6AreaField(ROOM_6_AREA)
        containsRoom7AreaField(ROOM_7_AREA)
        containsFloorField(FLOOR)
        containsBathroomField(lookup.matchesBathroomSelectorMatched())
        containsBalconyField(lookup.matchesBalconySelectorBalcony())
        containsRenovationField(lookup.matchesRenovationSelectorEuro())
        containsWindowsField(
            listOf(
                lookup.matchesWindowsSelectorStreet(),
                lookup.matchesWindowsSelectorYard()
            )
        )

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsBuildingYearField(BUILD_YEAR)
        containsCeilingHeightField(CEILING_HEIGHT)
        containsFloorsTotalField(TOTAL_FLOORS)
        containsParkingTypeField(lookup.matchesParkingTypeSelectorOpen())

        containsDescriptionField(DESCRIPTION)
        containsInternetField(true)
        containsRefrigeratorField(true)
        containsKitchenFurnitureField(true)
        containsAirconditionField(true)
        containsRoomFurnitureField(true)
        containsLiftField(true)
        containsRubbishChuteField(true)
        containsConciergeField(true)
        containsWashingMachineField(true)
        containsPassByField(true)
        containsDishwasherField(true)
        containsTelevisionField(true)
        containsWithChildrenField(true)
        containsWithPetsField(true)

        containsPriceField(PRICE_FORMATTED, paymentPeriod)
        containsPrepaymentField(PREPAYMENT)
        containsAgentFeeField(AGENT_FEE)
        containsRentAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorRentPledge(),
                lookup.matchesAdditionTermsSelectorUtilitiesIncluded()
            )
        )
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkSellRoomFormIsFull(imageUri: String) {
        containsAddressField(FULL_AURORA_ADDRESS)
        containsApartmentNumberField(APARTMENT_NUMBER)

        containsRoomsCountRoomField(lookup.matchesRoomsSelectorSeven())
        containsRoomsOfferedField(
            lookup.matchesRoomsSelectorSix(),
            lookup.matchesRoomsSelectorSix()
        )

        containsTotalAreaField(TOTAL_AREA)
        containsKitchenAreaField(KITCHEN_AREA)
        containsRoom1AreaField(ROOM_1_AREA)
        containsRoom2AreaField(ROOM_2_AREA)
        containsRoom3AreaField(ROOM_3_AREA)
        containsRoom4AreaField(ROOM_4_AREA)
        containsRoom5AreaField(ROOM_5_AREA)
        containsRoom6AreaField(ROOM_6_AREA)
        containsFloorField(FLOOR)
        containsBathroomField(lookup.matchesBathroomSelectorMatched())
        containsBalconyField(lookup.matchesBalconySelectorBalcony())
        containsFloorCoveringField(lookup.matchesFloorCoveringSelectorLinoleum())
        containsRenovationField(lookup.matchesRenovationSelectorEuro())
        containsWindowsField(
            listOf(
                lookup.matchesWindowsSelectorStreet(),
                lookup.matchesWindowsSelectorYard()
            )
        )
        containsPropertyStatusField(lookup.matchesPropertyStatusSelectorHousingStock())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsBuildingYearField(BUILD_YEAR)
        containsCeilingHeightField(CEILING_HEIGHT)
        containsFloorsTotalField(TOTAL_FLOORS)
        containsParkingTypeField(lookup.matchesParkingTypeSelectorOpen())
        containsBuildingTypeFiled(lookup.matchesBuildingTypeSelectorMonolitBrick())

        containsDescriptionField(DESCRIPTION)
        containsInternetField(true)
        containsKitchenFurnitureField(true)
        containsRoomFurnitureField(true)
        containsLiftField(true)
        containsRubbishChuteField(true)
        containsConciergeField(true)
        containsPassByField(true)

        containsPriceField(PRICE_FORMATTED)
        containsSellAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorMortgage()
            )
        )
        containsDealStatusField(lookup.matchesDealStatusSelectorSale())
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkRentRoomFormIsFull(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
    ) {
        containsAddressField(FULL_AURORA_ADDRESS)
        containsApartmentNumberField(APARTMENT_NUMBER)

        containsRoomsCountRoomField(lookup.matchesRoomsSelectorSeven())
        containsRoomsOfferedField(
            lookup.matchesRoomsSelectorSix(),
            lookup.matchesRoomsSelectorSix()
        )

        containsTotalAreaField(TOTAL_AREA)
        containsKitchenAreaField(KITCHEN_AREA)
        containsRoom1AreaField(ROOM_1_AREA)
        containsRoom2AreaField(ROOM_2_AREA)
        containsRoom3AreaField(ROOM_3_AREA)
        containsRoom4AreaField(ROOM_4_AREA)
        containsRoom5AreaField(ROOM_5_AREA)
        containsRoom6AreaField(ROOM_6_AREA)
        containsFloorField(FLOOR)
        containsBathroomField(lookup.matchesBathroomSelectorMatched())
        containsBalconyField(lookup.matchesBalconySelectorBalcony())
        containsFloorCoveringField(lookup.matchesFloorCoveringSelectorLinoleum())
        containsRenovationField(lookup.matchesRenovationSelectorEuro())
        containsWindowsField(
            listOf(
                lookup.matchesWindowsSelectorStreet(),
                lookup.matchesWindowsSelectorYard()
            )
        )

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsBuildingYearField(BUILD_YEAR)
        containsCeilingHeightField(CEILING_HEIGHT)
        containsFloorsTotalField(TOTAL_FLOORS)
        containsParkingTypeField(lookup.matchesParkingTypeSelectorOpen())

        containsDescriptionField(DESCRIPTION)
        containsInternetField(true)
        containsRefrigeratorField(true)
        containsKitchenFurnitureField(true)
        containsAirconditionField(true)
        containsRoomFurnitureField(true)
        containsLiftField(true)
        containsRubbishChuteField(true)
        containsConciergeField(true)
        containsWashingMachineField(true)
        containsPassByField(true)
        containsDishwasherField(true)
        containsTelevisionField(true)
        containsWithChildrenField(true)
        containsWithPetsField(true)

        containsPriceField(PRICE_FORMATTED, paymentPeriod)
        containsPrepaymentField(PREPAYMENT)
        containsAgentFeeField(AGENT_FEE)
        containsRentAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorRentPledge(),
                lookup.matchesAdditionTermsSelectorUtilitiesIncluded()
            )
        )
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkSellHouseFormIsFull(imageUri: String) {
        containsAddressField(FULL_AURORA_ADDRESS)
        containsHouseAreaField(HOUSE_AREA)
        containsHouseFloorsField(HOUSE_FLOORS)
        containsHouseTypeField(lookup.matchesHouseTypeSelectorTownhouse())
        containsHouseBuildingTypeField(lookup.matchesHouseBuildingTypeSelectorWood())
        containsToiletField(lookup.matchesToiletSelectorInside())
        containsShowerField(lookup.matchesShowerSelectorInside())

        containsLotAreaUnitField(lookup.matchesLotAreaUnitSelectorHectare())
        containsLotAreaField(LOT_AREA)
        containsLotTypeField(lookup.matchesLotTypeSelectorGarden())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsDescriptionField(DESCRIPTION)
        containsSewerageSupplyField(true)
        containsElectricitySupplyField(true)
        containsGasSupplyField(true)
        containsBilliardField(true)
        containsSaunaField(true)
        containsPoolField(true)
        containsPMGField(true)
        containsKitchenField(true)
        containsHeatingSupplyField(true)
        containsWaterSupplyField(true)

        containsPriceField(PRICE_FORMATTED)
        containsSellAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorMortgage()
            )
        )
        containsDealStatusField(lookup.matchesDealStatusSelectorSale())
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkRentHouseFormIsFull(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
    ) {
        containsAddressField(FULL_AURORA_ADDRESS)
        containsHouseAreaField(HOUSE_AREA)
        containsHouseFloorsField(HOUSE_FLOORS)
        containsHouseTypeField(lookup.matchesHouseTypeSelectorTownhouse())
        containsHouseBuildingTypeField(lookup.matchesHouseBuildingTypeSelectorWood())
        containsToiletField(lookup.matchesToiletSelectorInside())
        containsShowerField(lookup.matchesShowerSelectorInside())

        containsLotAreaUnitField(lookup.matchesLotAreaUnitSelectorHectare())
        containsLotAreaField(LOT_AREA)
        containsLotTypeField(lookup.matchesLotTypeSelectorGarden())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsDescriptionField(DESCRIPTION)
        containsSewerageSupplyField(true)
        containsElectricitySupplyField(true)
        containsGasSupplyField(true)
        containsBilliardField(true)
        containsSaunaField(true)
        containsPoolField(true)
        containsPMGField(true)
        containsKitchenField(true)
        containsHeatingSupplyField(true)
        containsWaterSupplyField(true)

        containsPriceField(PRICE_FORMATTED, paymentPeriod)
        containsPrepaymentField(PREPAYMENT)
        containsAgentFeeField(AGENT_FEE)
        containsRentAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorRentPledge(),
                lookup.matchesAdditionTermsSelectorUtilitiesIncluded()
            )
        )
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkSellLotFormIsFull(imageUri: String) {
        containsAddressField(FULL_AURORA_ADDRESS)

        containsLotAreaUnitField(lookup.matchesLotAreaUnitSelectorHectare())
        containsLotAreaField(LOT_AREA)
        containsLotTypeField(lookup.matchesLotTypeSelectorGarden())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsDescriptionField(DESCRIPTION)

        containsPriceField(PRICE_FORMATTED)
        containsSellAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorMortgage()
            )
        )
        containsDealStatusField(lookup.matchesDealStatusSelectorSale())
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkSellGarageFormIsFull(imageUri: String) {
        containsAddressField(FULL_AURORA_ADDRESS)

        containsGarageAreaField(GARAGE_AREA)
        containsGarageNameField(GARAGE_NAME)
        containsGarageTypeField(lookup.matchesGarageTypeSelectorBox())
        containsGarageParkingTypeField(lookup.matchesGarageParkingTypeSelectorMultilevel())
        containsGarageBuildingTypeField(lookup.matchesGarageBuildingTypeSelectorFerroconcrete())
        containsGarageOwnershipField(lookup.matchesGarageOwnershipSelectorCooperative())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsDescriptionField(DESCRIPTION)
        containsFireAlarmField(true)
        containsTwentyFourSevenField(true)
        containsAccessControlSystemField(true)
        containsHeatingSupplyField(true)
        containsWaterSupplyField(true)
        containsElectricitySupplyField(true)
        containsAutomaticGatesField(true)
        containsCctvField(true)
        containsSecurityField(true)
        containsInspectionPitField(true)
        containsCarWashField(true)
        containsAutoRepairField(true)

        containsPriceField(PRICE_FORMATTED)
        containsGarageSellAdditionTermsField(listOf(lookup.matchesAdditionTermsSelectorHaggle()))
        containsOnlineShowField(true)
    }

    protected fun PublicationFormRobot.checkRentGarageFormIsFull(imageUri: String) {
        containsAddressField(FULL_AURORA_ADDRESS)

        containsGarageAreaField(GARAGE_AREA)
        containsGarageNameField(GARAGE_NAME)
        containsGarageTypeField(lookup.matchesGarageTypeSelectorBox())
        containsGarageParkingTypeField(lookup.matchesGarageParkingTypeSelectorMultilevel())
        containsGarageBuildingTypeField(lookup.matchesGarageBuildingTypeSelectorFerroconcrete())
        containsGarageOwnershipField(lookup.matchesGarageOwnershipSelectorCooperative())

        isImageShownInGallery(imageUri)
        containsImagesOrderChangeAllowedField(true)
        containsVideoUrlField(VIDEO_URL)

        containsDescriptionField(DESCRIPTION)
        containsFireAlarmField(true)
        containsTwentyFourSevenField(true)
        containsAccessControlSystemField(true)
        containsHeatingSupplyField(true)
        containsWaterSupplyField(true)
        containsElectricitySupplyField(true)
        containsAutomaticGatesField(true)
        containsCctvField(true)
        containsSecurityField(true)
        containsInspectionPitField(true)
        containsCarWashField(true)
        containsAutoRepairField(true)

        containsPricePerMonthField(PRICE_FORMATTED)
        containsPrepaymentField(PREPAYMENT)
        containsAgentFeeField(AGENT_FEE)
        containsGarageRentAdditionTermsField(
            listOf(
                lookup.matchesAdditionTermsSelectorHaggle(),
                lookup.matchesAdditionTermsSelectorRentPledge(),
                lookup.matchesAdditionTermsSelectorUtilitiesIncluded(),
                lookup.matchesAdditionTermsSelectorElectricityIncluded()
            )
        )
        containsOnlineShowField(true)
    }

    protected fun ConfirmationDialogRobot.isUpdateRequiredDialogShown() {
        isTitleEquals(getResourceString(R.string.uo_update_app_dialog_title))
        isMessageEquals(getResourceString(R.string.uo_update_app_dialog_message))
        isNegativeButtonTextEquals(getResourceString(R.string.cancel))
        isPositiveButtonTextEquals(getResourceString(R.string.uo_need_update_action))
    }

    private fun PublicationFormRobot.containsPriceField(
        text: String? = null,
        paymentPeriod: PaymentPeriod? = null,
    ) {
        when (paymentPeriod) {
            PaymentPeriod.PER_DAY -> containsPricePerDayField(text)
            PaymentPeriod.PER_MONTH -> containsPricePerMonthField(text)
            null -> containsPriceField(text)
        }
    }

    protected fun PublicationFormLookup.matchesPriceField(
        paymentPeriod: PaymentPeriod? = null,
    ): NamedViewMatcher {
        return when (paymentPeriod) {
            PaymentPeriod.PER_DAY -> matchesPricePerDayField()
            PaymentPeriod.PER_MONTH -> matchesPricePerMonthField()
            null -> matchesPriceField()
        }
    }

    protected fun PublicationFormLookup.matchesPriceFieldValue(
        paymentPeriod: PaymentPeriod? = null,
    ): NamedViewMatcher {
        return when (paymentPeriod) {
            PaymentPeriod.PER_DAY -> matchesPricePerDayFieldValue()
            PaymentPeriod.PER_MONTH -> matchesPricePerMonthFieldValue()
            null -> matchesPriceFieldValue()
        }
    }

    protected fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("publishForm/userOwnerNoMosRu.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerBlockedUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("user/userBlocked.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerUserProfilePatch() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                assetBody("publishForm/contacts.json")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    protected fun DispatcherRegistry.registerJuridicUserProfilePatch() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                assetBody("publishForm/contactsJuridic.json")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    protected fun DispatcherRegistry.registerPassportPhoneBind(
        phone: String,
        error: String? = null
    ) {
        register(
            request {
                method("POST")
                path("2.0/passport/phone/bind")
                body("""{ "phone": "$phone" }""")
            },
            response {
                if (error == null) {
                    setBody(
                        """{ "response":
                                {
                                    "trackId": "b5255d3a-ca7c-11ea-87d0-0242ac130003",
                                    "codeLength": 6
                                }
                            }"""
                    )
                } else {
                    setResponseCode(400)
                    setBody(
                        """{
                                "error": {
                                  "code": "$error",
                                  "message": "error message"
                                }
                            }"""
                    )
                }
            }
        )
    }

    protected fun DispatcherRegistry.registerPassportPhoneConfirm(
        code: String,
        error: String? = null
    ) {
        register(
            request {
                method("POST")
                path("2.0/passport/phone/confirm")
                body(
                    """{
                            "trackId": "b5255d3a-ca7c-11ea-87d0-0242ac130003",
                            "code": "$code"
                        }"""
                )
            },
            response {
                if (error == null) {
                    setResponseCode(200)
                } else {
                    setResponseCode(400)
                    setBody(
                        """{
                                "error": {
                                  "code": "$error",
                                  "message": "error message"
                                }
                            }"""
                    )
                }
            }
        )
    }

    protected fun DispatcherRegistry.registerNoRequiredFeatures() {
        register(
            request {
                method("GET")
                path("1.0/device/requiredFeature")
            },
            response {
                setBody("{\"response\": {\"userOffers\": []}}")
            }
        )
    }

    protected fun DispatcherRegistry.registerUserOffersOneOffer() {
        register(
            request {
                method("GET")
                path("2.0/user/me/offers")
                queryParam("pageSize", "10")
                queryParam("page", "0")
            },
            response {
                assetBody("userOffers/userOffersPublishedFree.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerUploadPhoto(
        path: String,
        imageId: String = "imageId"
    ) {
        register(
            request {
                path("1.0/photo.json")
            },
            response {
                setBody(
                    """{"response":
                        {
                            "path": "$imageId",
                            "sizes": [
                                {
                                    "path": "$path",
                                    "variant": "large"
                                }
                            ]

                        }
                        }""".trimMargin()
                )
            }
        )
    }

    protected fun DispatcherRegistry.registerValidation(fileName: String) {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
                assetBody(fileName)
            },
            response {
                setResponseCode(200)
            }
        )
    }

    protected fun DispatcherRegistry.registerDraft() {
        register(
            request {
                method("POST")
                path("1.0/user/offers/draft")
            },
            response {
                setBody("{\"response\":{ \"id\": \"1234\" }}")
            }
        )
    }

    protected fun DispatcherRegistry.registerPublishDraft(fileName: String) {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/draft/1234")
                queryParam("publish", "true")
                assetBody(fileName)
            },
            response {
                setResponseCode(200)
            }
        )
    }

    protected fun DispatcherRegistry.registerPublishOffer(fileName: String) {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/1234")
                assetBody(fileName)
            },
            response {
                setResponseCode(200)
            }
        )
    }

    protected fun DispatcherRegistry.registerEditOffer(fileName: String) {
        register(
            request {
                method("GET")
                path("1.0/user/offers/1234/edit")
            },
            response {
                assetBody(fileName)
            }
        )
    }

    protected fun DispatcherRegistry.registerEmptyUserOffers() {
        register(
            request {
                method("GET")
                path("2.0/user/me/offers")
                queryParam("pageSize", "10")
                queryParam("page", "0")
            },
            response {
                assetBody("userOffers/userOffersEmpty.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerGetNearAuroraAddressApartment() {
        registerGetNearAuroraAddress("APARTMENT")
    }

    protected fun DispatcherRegistry.registerGetNearAuroraAddressRoom() {
        registerGetNearAuroraAddress("ROOM")
    }

    protected fun DispatcherRegistry.registerGetNearAuroraAddressHouse() {
        registerGetNearAuroraAddress("HOUSE")
    }

    protected fun DispatcherRegistry.registerGetNearAuroraAddressLot() {
        registerGetNearAuroraAddress("LOT")
    }

    protected fun DispatcherRegistry.registerGetNearAuroraAddressGarage() {
        registerGetNearAuroraAddress("GARAGE")
    }

    private fun DispatcherRegistry.registerGetNearAuroraAddress(category: String) {
        register(
            request {
                path("1.0/addressGeocoder.json")
                queryParam("latitude", NEAR_AURORA_LATITUDE.toString())
                queryParam("longitude", NEAR_AURORA_LONGITUDE.toString())
                queryParam("category", category)
            },
            response {
                assetBody("geocoderAddressAurora.json")
            }
        )
    }

    protected fun DispatcherRegistry.registerGetAuroraAddressApartment() {
        register(
            request {
                path("1.0/addressGeocoder.json")
                queryParam("latitude", AURORA_LATITUDE.toString())
                queryParam("longitude", AURORA_LONGITUDE.toString())
                queryParam("category", "APARTMENT")
            },
            response {
                assetBody("geocoderAddressAurora.json")
            }
        )
    }

    companion object {
        const val NEAR_AURORA_LATITUDE = 55.734655
        const val NEAR_AURORA_LONGITUDE = 37.642313
        const val AURORA_LATITUDE = 55.73552
        const val AURORA_LONGITUDE = 37.642475
        const val AURORA_ADDRESS = "Садовническая улица, 82с2"
        const val AURORA_ADDRESS_WITHOUT_COUNTRY = "Москва, Садовническая улица, 82с2"
        const val FULL_AURORA_ADDRESS = "Россия, Москва, Садовническая улица, 82с2"
        const val DESCRIPTION = "test text"
        const val APARTMENT_NUMBER = "123"
        const val TOTAL_AREA = "50"
        const val LIVING_AREA = "30"
        const val KITCHEN_AREA = "12"
        const val ROOM_1_AREA = "5"
        const val ROOM_2_AREA = "6"
        const val ROOM_3_AREA = "7"
        const val ROOM_4_AREA = "8"
        const val ROOM_5_AREA = "9"
        const val ROOM_6_AREA = "10"
        const val ROOM_7_AREA = "11"
        const val FLOOR = "5"
        const val BUILD_YEAR = "2016"
        const val CEILING_HEIGHT = "2,7"
        const val TOTAL_FLOORS = "17"
        const val PRICE = "5000000"
        const val PREPAYMENT = "50"
        const val AGENT_FEE = "100"
        const val PRICE_FORMATTED = "5 000 000"
        const val VIDEO_URL = "https://www.youtube.com/watch?v=Sa-l76EUn74"
        const val HOUSE_AREA = "150"
        const val HOUSE_FLOORS = "2"
        const val LOT_AREA = "3"
        const val GARAGE_AREA = "11"
        const val GARAGE_NAME = "GSK West"
        const val NEW_PHONE = "+79376666666"
        const val CONFIRMATION_CODE = "410060"
        const val NEW_NAME = "Peter"
        const val NEW_EMAIL = "peter@gmail.com"
    }
}
