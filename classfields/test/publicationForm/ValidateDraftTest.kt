package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.createImageAndGetUriString
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.robot.PublicationFormRobot
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 27.07.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class ValidateDraftTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormActivityTestRule(launchActivity = false)
    private val mockLocationRule = MockLocationRule()
    private val draftRule = UserOfferDraftRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        mockLocationRule,
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION),
        SetupDefaultAppStateRule(),
        activityTestRule,
        draftRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @Test
    fun validateSellApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellApartment7Rooms.json",
                "publishForm/validateSellApartmentError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllSellApartmentErrors()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllSellApartmentValues(imageUri)

            checkDoesNotContainAllSellApartmentErrors()
        }
    }

    @Test
    fun validateRentLongApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongApartment7Rooms.json",
                "publishForm/validateRentApartmentError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
        }
        draftRule.prepareRentLongApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentApartmentErrors()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentApartmentValues(imageUri, PaymentPeriod.PER_MONTH)

            checkDoesNotContainAllRentApartmentErrors()
        }
    }

    @Test
    fun validateRentShortApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentShortApartment7Rooms.json",
                "publishForm/validateRentApartmentError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressApartment()
        }
        draftRule.prepareRentShortApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentApartmentErrors()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentApartmentValues(imageUri, PaymentPeriod.PER_DAY)

            checkDoesNotContainAllRentApartmentErrors()
        }
    }

    @Test
    fun validateSellRoom() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellRoom7Rooms.json",
                "publishForm/validateSellRoomError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressRoom()
        }
        draftRule.prepareSellRoom()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllSellRoomErrors()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorFive()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllSellRoomValues(imageUri)

            checkDoesNotContainAllSellRoomErrors()
        }
    }

    @Test
    fun validateRentLongRoom() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongRoom7Rooms.json",
                "publishForm/validateRentRoomError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressRoom()
        }
        draftRule.prepareRentLongRoom()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentRoomErrors()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorFive()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentRoomValues(imageUri, PaymentPeriod.PER_MONTH)

            checkDoesNotContainAllRentRoomErrors()
        }
    }

    @Test
    fun validateRentShortRoom() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentShortRoom7Rooms.json",
                "publishForm/validateRentRoomError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressRoom()
        }
        draftRule.prepareRentShortRoom()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSeven()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentRoomErrors()

            scrollToPosition(lookup.matchesRoomsCountField())
            onView(lookup.matchesRoomsSelectorSix()).tapOn()
            scrollToPosition(lookup.matchesRoomsOfferedField())
            onView(lookup.matchesRoomsOfferedSelectorFive()).tapOn()
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentRoomValues(imageUri, PaymentPeriod.PER_DAY)

            checkDoesNotContainAllRentRoomErrors()
        }
    }

    @Test
    fun validateSellHouse() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellHouseEmpty.json",
                "publishForm/validateSellHouseError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressHouse()
        }
        draftRule.prepareSellHouse()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllSellHouseErrors()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllSellHouseValues(imageUri)

            checkDoesNotContainAllSellHouseErrors()
        }
    }

    @Test
    fun validateRentLongHouse() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongHouseEmpty.json",
                "publishForm/validateRentHouseError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressHouse()
        }
        draftRule.prepareRentLongHouse()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentHouseErrors()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentHouseValues(imageUri, PaymentPeriod.PER_MONTH)

            checkDoesNotContainAllRentHouseErrors()
        }
    }

    @Test
    fun validateRentShortHouse() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentShortHouseEmpty.json",
                "publishForm/validateRentHouseError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressHouse()
        }
        draftRule.prepareRentShortHouse()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentHouseErrors()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentHouseValues(imageUri, PaymentPeriod.PER_DAY)

            checkDoesNotContainAllRentHouseErrors()
        }
    }

    @Test
    fun validateSellLot() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellLotEmpty.json",
                "publishForm/validateSellLotError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressLot()
        }
        draftRule.prepareSellLot()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellLotExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllSellLotErrors()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllSellLotValues(imageUri)

            checkDoesNotContainAllSellLotErrors()
        }
    }

    @Test
    fun validateSellGarage() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellGarageEmpty.json",
                "publishForm/validateSellGarageError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressGarage()
        }
        draftRule.prepareSellGarage()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorParkingPlace()).tapOn()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllSellGarageErrors()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllSellGarageValues(imageUri)

            checkDoesNotContainAllSellGarageErrors()
        }
    }

    @Test
    fun validateRentLongGarage() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongGarageEmpty.json",
                "publishForm/validateRentLongGarageError.json"
            )
            registerUploadPhoto(imageUri)
            registerGetNearAuroraAddressGarage()
        }
        draftRule.prepareRentLongGarage()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongGarageCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesGarageTypeField())
            onView(lookup.matchesGarageTypeSelectorParkingPlace()).tapOn()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesAddressField()).check(matches(isCompletelyDisplayed()))
            }
            checkContainsAllRentGarageErrors()

            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            fillAllRentGarageValues(imageUri)

            checkDoesNotContainAllRentGarageErrors()
        }
    }

    @Test
    fun validateUserProfile() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerProfileValidationError()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil {
                onView(lookup.matchesContactNameField()).check(matches(isCompletelyDisplayed()))
            }
            containsValidationError(PROFILE_NAME_ERROR_TEXT)
            containsValidationError(PROFILE_EMAIL_ERROR_TEXT)
            containsValidationError(PROFILE_PHONE_ERROR_TEXT)

            scrollToPosition(lookup.matchesContactNameField())
            typeText(lookup.matchesContactNameFieldValue(), "Irina")
            scrollToPosition(lookup.matchesContactEmailField())
            typeText(lookup.matchesContactEmailFieldValue(), "mail@gmail.com")
            scrollToPosition(lookup.matchesContactPhonesField())
            tapOn(lookup.matchesContactPhoneFieldSelector("+7111*****44"))

            doesNotContainValidationError(PROFILE_NAME_ERROR_TEXT)
            doesNotContainValidationError(PROFILE_EMAIL_ERROR_TEXT)
            doesNotContainValidationError(PROFILE_PHONE_ERROR_TEXT)
        }
    }

    @Test
    fun showDialogOnUnknownOfferValidationError() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellApartmentEmpty.json",
                "publishForm/validationUnknownError.json"
            )
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        registerMarketIntent()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesImagesOrderChangeAllowedField()).tapOn()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            performOnConfirmationDialog {
                waitUntil { isUpdateRequiredDialogShown() }
                tapOn(lookup.matchesPositiveButton())
                intended(matchesMarketIntent())
            }
        }
    }

    @Test
    fun showDialogOnUnknownProfileValidationError() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerUnknownProfileValidationError()
        }
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            performOnConfirmationDialog {
                waitUntil { isUpdateRequiredDialogShown() }
            }
        }
    }

    private fun PublicationFormRobot.checkContainsAllSellApartmentErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(APARTMENT_NUMBER_ERROR_TEXT)
        containsValidationError(ROOMS_COUNT_ERROR_TEXT)
        containsValidationError(TOTAL_AREA_ERROR_TEXT)
        containsValidationError(LIVING_AREA_ERROR_TEXT)
        containsValidationError(KITCHEN_AREA_ERROR_TEXT)
        containsValidationError(ROOM_AREA_1_ERROR_TEXT)
        containsValidationError(ROOM_AREA_2_ERROR_TEXT)
        containsValidationError(ROOM_AREA_3_ERROR_TEXT)
        containsValidationError(ROOM_AREA_4_ERROR_TEXT)
        containsValidationError(ROOM_AREA_5_ERROR_TEXT)
        containsValidationError(ROOM_AREA_6_ERROR_TEXT)
        containsValidationError(ROOM_AREA_7_ERROR_TEXT)
        containsValidationError(FLOOR_ERROR_TEXT)
        containsValidationError(BATHROOM_ERROR_TEXT)
        containsValidationError(BALCONY_ERROR_TEXT)
        containsValidationError(RENOVATION_ERROR_TEXT)
        containsValidationError(WINDOW_VIEW_ERROR_TEXT)
        containsValidationError(PROPERTY_STATUS_ERROR_TEXT)
        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)
        containsValidationError(HOUSE_READY_ERROR_TEXT)
        containsValidationError(BUILD_YEAR_ERROR_TEXT)
        containsValidationError(CEILING_HEIGHT_ERROR_TEXT)
        containsValidationError(TOTAL_FLOORS_ERROR_TEXT)
        containsValidationError(PARKING_TYPE_ERROR_TEXT)
        containsValidationError(BUILDING_TYPE_ERROR_TEXT)
        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(INTERNET_AND_REFRIGERATOR_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_AIRCONDITION_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        containsValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        containsValidationError(CLOSED_TERRITORY_ERROR_TEXT)
        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        containsValidationError(DEAL_STATUS_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllSellApartmentErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(ROOMS_COUNT_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_AREA_ERROR_TEXT)
        doesNotContainValidationError(LIVING_AREA_ERROR_TEXT)
        doesNotContainValidationError(KITCHEN_AREA_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_1_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_2_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_3_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_4_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_5_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_6_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_7_ERROR_TEXT)
        doesNotContainValidationError(FLOOR_ERROR_TEXT)
        doesNotContainValidationError(BATHROOM_ERROR_TEXT)
        doesNotContainValidationError(BALCONY_ERROR_TEXT)
        doesNotContainValidationError(RENOVATION_ERROR_TEXT)
        doesNotContainValidationError(WINDOW_VIEW_ERROR_TEXT)
        doesNotContainValidationError(PROPERTY_STATUS_ERROR_TEXT)
        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_READY_ERROR_TEXT)
        doesNotContainValidationError(BUILD_YEAR_ERROR_TEXT)
        doesNotContainValidationError(CEILING_HEIGHT_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_FLOORS_ERROR_TEXT)
        doesNotContainValidationError(PARKING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(BUILDING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(INTERNET_AND_REFRIGERATOR_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_AIRCONDITION_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        doesNotContainValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        doesNotContainValidationError(CLOSED_TERRITORY_ERROR_TEXT)
        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        doesNotContainValidationError(DEAL_STATUS_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllRentApartmentErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(APARTMENT_NUMBER_ERROR_TEXT)
        containsValidationError(ROOMS_COUNT_ERROR_TEXT)
        containsValidationError(TOTAL_AREA_ERROR_TEXT)
        containsValidationError(LIVING_AREA_ERROR_TEXT)
        containsValidationError(KITCHEN_AREA_ERROR_TEXT)
        containsValidationError(ROOM_AREA_1_ERROR_TEXT)
        containsValidationError(ROOM_AREA_2_ERROR_TEXT)
        containsValidationError(ROOM_AREA_3_ERROR_TEXT)
        containsValidationError(ROOM_AREA_4_ERROR_TEXT)
        containsValidationError(ROOM_AREA_5_ERROR_TEXT)
        containsValidationError(ROOM_AREA_6_ERROR_TEXT)
        containsValidationError(ROOM_AREA_7_ERROR_TEXT)
        containsValidationError(FLOOR_ERROR_TEXT)
        containsValidationError(BATHROOM_ERROR_TEXT)
        containsValidationError(BALCONY_ERROR_TEXT)
        containsValidationError(RENOVATION_ERROR_TEXT)
        containsValidationError(WINDOW_VIEW_ERROR_TEXT)
        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)
        containsValidationError(BUILD_YEAR_ERROR_TEXT)
        containsValidationError(CEILING_HEIGHT_ERROR_TEXT)
        containsValidationError(TOTAL_FLOORS_ERROR_TEXT)
        containsValidationError(PARKING_TYPE_ERROR_TEXT)
        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(INTERNET_AND_REFRIGERATOR_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_AIRCONDITION_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        containsValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        containsValidationError(WASHING_MACHINE_AND_CLOSED_TERRITORY_ERROR_TEXT)
        containsValidationError(DISHWASHER_AND_TELEVISION_ERROR_TEXT)
        containsValidationError(CHILDREN_AND_PETS_ERROR_TEXT)
        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(PREPAYMENT_ERROR_TEXT)
        containsValidationError(AGENT_FEE_ERROR_TEXT)
        containsValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllRentApartmentErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(ROOMS_COUNT_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_AREA_ERROR_TEXT)
        doesNotContainValidationError(LIVING_AREA_ERROR_TEXT)
        doesNotContainValidationError(KITCHEN_AREA_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_1_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_2_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_3_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_4_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_5_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_6_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_7_ERROR_TEXT)
        doesNotContainValidationError(FLOOR_ERROR_TEXT)
        doesNotContainValidationError(BATHROOM_ERROR_TEXT)
        doesNotContainValidationError(BALCONY_ERROR_TEXT)
        doesNotContainValidationError(RENOVATION_ERROR_TEXT)
        doesNotContainValidationError(WINDOW_VIEW_ERROR_TEXT)
        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)
        doesNotContainValidationError(BUILD_YEAR_ERROR_TEXT)
        doesNotContainValidationError(CEILING_HEIGHT_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_FLOORS_ERROR_TEXT)
        doesNotContainValidationError(PARKING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(INTERNET_AND_REFRIGERATOR_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_AIRCONDITION_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        doesNotContainValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        doesNotContainValidationError(WASHING_MACHINE_AND_CLOSED_TERRITORY_ERROR_TEXT)
        doesNotContainValidationError(DISHWASHER_AND_TELEVISION_ERROR_TEXT)
        doesNotContainValidationError(CHILDREN_AND_PETS_ERROR_TEXT)
        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(PREPAYMENT_ERROR_TEXT)
        doesNotContainValidationError(AGENT_FEE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllSellRoomErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(APARTMENT_NUMBER_ERROR_TEXT)
        containsValidationError(ROOMS_COUNT_ERROR_TEXT)
        containsValidationError(ROOMS_OFFERED_ERROR_TEXT)

        containsValidationError(TOTAL_AREA_ERROR_TEXT)
        containsValidationError(KITCHEN_AREA_ERROR_TEXT)
        containsValidationError(ROOM_AREA_1_ERROR_TEXT)
        containsValidationError(ROOM_AREA_2_ERROR_TEXT)
        containsValidationError(ROOM_AREA_3_ERROR_TEXT)
        containsValidationError(ROOM_AREA_4_ERROR_TEXT)
        containsValidationError(ROOM_AREA_5_ERROR_TEXT)
        containsValidationError(ROOM_AREA_6_ERROR_TEXT)
        containsValidationError(FLOOR_ERROR_TEXT)
        containsValidationError(BATHROOM_ERROR_TEXT)
        containsValidationError(BALCONY_ERROR_TEXT)
        containsValidationError(FLOOR_COVERING_ERROR_TEXT)
        containsValidationError(RENOVATION_ERROR_TEXT)
        containsValidationError(WINDOW_VIEW_ERROR_TEXT)
        containsValidationError(PROPERTY_STATUS_ERROR_TEXT)
        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)
        containsValidationError(BUILD_YEAR_ERROR_TEXT)
        containsValidationError(CEILING_HEIGHT_ERROR_TEXT)
        containsValidationError(TOTAL_FLOORS_ERROR_TEXT)
        containsValidationError(PARKING_TYPE_ERROR_TEXT)
        containsValidationError(BUILDING_TYPE_ERROR_TEXT)
        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(INTERNET_AND_FURNITURE_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        containsValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        containsValidationError(CLOSED_TERRITORY_ERROR_TEXT)
        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        containsValidationError(DEAL_STATUS_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllSellRoomErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(ROOMS_COUNT_ERROR_TEXT)
        doesNotContainValidationError(ROOMS_OFFERED_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_AREA_ERROR_TEXT)
        doesNotContainValidationError(KITCHEN_AREA_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_1_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_2_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_3_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_4_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_5_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_6_ERROR_TEXT)
        doesNotContainValidationError(FLOOR_ERROR_TEXT)
        doesNotContainValidationError(BATHROOM_ERROR_TEXT)
        doesNotContainValidationError(BALCONY_ERROR_TEXT)
        doesNotContainValidationError(FLOOR_COVERING_ERROR_TEXT)
        doesNotContainValidationError(RENOVATION_ERROR_TEXT)
        doesNotContainValidationError(WINDOW_VIEW_ERROR_TEXT)
        doesNotContainValidationError(PROPERTY_STATUS_ERROR_TEXT)
        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)
        doesNotContainValidationError(BUILD_YEAR_ERROR_TEXT)
        doesNotContainValidationError(CEILING_HEIGHT_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_FLOORS_ERROR_TEXT)
        doesNotContainValidationError(PARKING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(BUILDING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(INTERNET_AND_FURNITURE_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        doesNotContainValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        doesNotContainValidationError(CLOSED_TERRITORY_ERROR_TEXT)
        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        doesNotContainValidationError(DEAL_STATUS_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllRentRoomErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(APARTMENT_NUMBER_ERROR_TEXT)
        containsValidationError(ROOMS_COUNT_ERROR_TEXT)
        containsValidationError(ROOMS_OFFERED_ERROR_TEXT)

        containsValidationError(TOTAL_AREA_ERROR_TEXT)
        containsValidationError(KITCHEN_AREA_ERROR_TEXT)
        containsValidationError(ROOM_AREA_1_ERROR_TEXT)
        containsValidationError(ROOM_AREA_2_ERROR_TEXT)
        containsValidationError(ROOM_AREA_3_ERROR_TEXT)
        containsValidationError(ROOM_AREA_4_ERROR_TEXT)
        containsValidationError(ROOM_AREA_5_ERROR_TEXT)
        containsValidationError(ROOM_AREA_6_ERROR_TEXT)
        containsValidationError(FLOOR_ERROR_TEXT)
        containsValidationError(BATHROOM_ERROR_TEXT)
        containsValidationError(BALCONY_ERROR_TEXT)
        containsValidationError(FLOOR_COVERING_ERROR_TEXT)
        containsValidationError(RENOVATION_ERROR_TEXT)
        containsValidationError(WINDOW_VIEW_ERROR_TEXT)
        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)
        containsValidationError(BUILD_YEAR_ERROR_TEXT)
        containsValidationError(CEILING_HEIGHT_ERROR_TEXT)
        containsValidationError(TOTAL_FLOORS_ERROR_TEXT)
        containsValidationError(PARKING_TYPE_ERROR_TEXT)
        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(INTERNET_AND_REFRIGERATOR_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_AIRCONDITION_ERROR_TEXT)
        containsValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        containsValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        containsValidationError(WASHING_MACHINE_AND_CLOSED_TERRITORY_ERROR_TEXT)
        containsValidationError(DISHWASHER_AND_TELEVISION_ERROR_TEXT)
        containsValidationError(CHILDREN_AND_PETS_ERROR_TEXT)
        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(PREPAYMENT_ERROR_TEXT)
        containsValidationError(AGENT_FEE_ERROR_TEXT)
        containsValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllRentRoomErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(ROOMS_COUNT_ERROR_TEXT)
        doesNotContainValidationError(ROOMS_OFFERED_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_AREA_ERROR_TEXT)
        doesNotContainValidationError(KITCHEN_AREA_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_1_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_2_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_3_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_4_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_5_ERROR_TEXT)
        doesNotContainValidationError(ROOM_AREA_6_ERROR_TEXT)
        doesNotContainValidationError(FLOOR_ERROR_TEXT)
        doesNotContainValidationError(BATHROOM_ERROR_TEXT)
        doesNotContainValidationError(BALCONY_ERROR_TEXT)
        doesNotContainValidationError(FLOOR_COVERING_ERROR_TEXT)
        doesNotContainValidationError(RENOVATION_ERROR_TEXT)
        doesNotContainValidationError(WINDOW_VIEW_ERROR_TEXT)
        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)
        doesNotContainValidationError(BUILD_YEAR_ERROR_TEXT)
        doesNotContainValidationError(CEILING_HEIGHT_ERROR_TEXT)
        doesNotContainValidationError(TOTAL_FLOORS_ERROR_TEXT)
        doesNotContainValidationError(PARKING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(INTERNET_AND_REFRIGERATOR_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_AIRCONDITION_ERROR_TEXT)
        doesNotContainValidationError(FURNITURE_AND_LIFT_ERROR_TEXT)
        doesNotContainValidationError(RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT)
        doesNotContainValidationError(WASHING_MACHINE_AND_CLOSED_TERRITORY_ERROR_TEXT)
        doesNotContainValidationError(DISHWASHER_AND_TELEVISION_ERROR_TEXT)
        doesNotContainValidationError(CHILDREN_AND_PETS_ERROR_TEXT)
        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(PREPAYMENT_ERROR_TEXT)
        doesNotContainValidationError(AGENT_FEE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllSellHouseErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(HOUSE_AREA_ERROR_TEXT)
        containsValidationError(HOUSE_FLOORS_ERROR_TEXT)
        containsValidationError(HOUSE_TYPE_ERROR_TEXT)
        containsValidationError(TOILET_ERROR_TEXT)
        containsValidationError(SHOWER_ERROR_TEXT)

        containsValidationError(LOT_AREA_UNIT_ERROR_TEXT)
        containsValidationError(LOT_AREA_ERROR_TEXT)
        containsValidationError(LOT_TYPE_ERROR_TEXT)

        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)

        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(SEWERAGE_ELECTRICITY_SUPPLY_ERROR_TEXT)
        containsValidationError(GAS_SUPPLY_BILLIARD_ERROR_TEXT)
        containsValidationError(SAUNA_POOL_ERROR_TEXT)
        containsValidationError(PMG_KITCHEN_ERROR_TEXT)
        containsValidationError(HEATING_WATER_SUPPLY_ERROR_TEXT)

        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        containsValidationError(DEAL_STATUS_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllSellHouseErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_AREA_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_FLOORS_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_TYPE_ERROR_TEXT)
        doesNotContainValidationError(TOILET_ERROR_TEXT)
        doesNotContainValidationError(SHOWER_ERROR_TEXT)

        doesNotContainValidationError(LOT_AREA_UNIT_ERROR_TEXT)
        doesNotContainValidationError(LOT_AREA_ERROR_TEXT)
        doesNotContainValidationError(LOT_TYPE_ERROR_TEXT)

        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)

        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(SEWERAGE_ELECTRICITY_SUPPLY_ERROR_TEXT)
        doesNotContainValidationError(GAS_SUPPLY_BILLIARD_ERROR_TEXT)
        doesNotContainValidationError(SAUNA_POOL_ERROR_TEXT)
        doesNotContainValidationError(PMG_KITCHEN_ERROR_TEXT)
        doesNotContainValidationError(HEATING_WATER_SUPPLY_ERROR_TEXT)

        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        doesNotContainValidationError(DEAL_STATUS_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllRentHouseErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(HOUSE_AREA_ERROR_TEXT)
        containsValidationError(HOUSE_FLOORS_ERROR_TEXT)
        containsValidationError(HOUSE_TYPE_ERROR_TEXT)
        containsValidationError(TOILET_ERROR_TEXT)
        containsValidationError(SHOWER_ERROR_TEXT)

        containsValidationError(LOT_AREA_UNIT_ERROR_TEXT)
        containsValidationError(LOT_AREA_ERROR_TEXT)
        containsValidationError(LOT_TYPE_ERROR_TEXT)

        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)

        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(SEWERAGE_ELECTRICITY_SUPPLY_ERROR_TEXT)
        containsValidationError(GAS_SUPPLY_BILLIARD_ERROR_TEXT)
        containsValidationError(SAUNA_POOL_ERROR_TEXT)
        containsValidationError(PMG_KITCHEN_ERROR_TEXT)
        containsValidationError(HEATING_WATER_SUPPLY_ERROR_TEXT)

        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(PREPAYMENT_ERROR_TEXT)
        containsValidationError(AGENT_FEE_ERROR_TEXT)
        containsValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllRentHouseErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_AREA_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_FLOORS_ERROR_TEXT)
        doesNotContainValidationError(HOUSE_TYPE_ERROR_TEXT)
        doesNotContainValidationError(TOILET_ERROR_TEXT)
        doesNotContainValidationError(SHOWER_ERROR_TEXT)

        doesNotContainValidationError(LOT_AREA_UNIT_ERROR_TEXT)
        doesNotContainValidationError(LOT_AREA_ERROR_TEXT)
        doesNotContainValidationError(LOT_TYPE_ERROR_TEXT)

        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)

        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(SEWERAGE_ELECTRICITY_SUPPLY_ERROR_TEXT)
        doesNotContainValidationError(GAS_SUPPLY_BILLIARD_ERROR_TEXT)
        doesNotContainValidationError(SAUNA_POOL_ERROR_TEXT)
        doesNotContainValidationError(PMG_KITCHEN_ERROR_TEXT)
        doesNotContainValidationError(HEATING_WATER_SUPPLY_ERROR_TEXT)

        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(PREPAYMENT_ERROR_TEXT)
        doesNotContainValidationError(AGENT_FEE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllSellLotErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)

        containsValidationError(LOT_AREA_UNIT_ERROR_TEXT)
        containsValidationError(LOT_AREA_ERROR_TEXT)
        containsValidationError(LOT_TYPE_ERROR_TEXT)

        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)

        containsValidationError(DESCRIPTION_ERROR_TEXT)

        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        containsValidationError(DEAL_STATUS_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllSellLotErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)

        doesNotContainValidationError(LOT_AREA_UNIT_ERROR_TEXT)
        doesNotContainValidationError(LOT_AREA_ERROR_TEXT)
        doesNotContainValidationError(LOT_TYPE_ERROR_TEXT)

        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)

        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)

        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_MORTGAGE_ERROR_TEXT)
        doesNotContainValidationError(DEAL_STATUS_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllSellGarageErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(GARAGE_AREA_ERROR_TEXT)
        containsValidationError(GARAGE_NAME_ERROR_TEXT)
        containsValidationError(GARAGE_TYPE_ERROR_TEXT)
        containsValidationError(GARAGE_PARKING_TYPE_ERROR_TEXT)
        containsValidationError(GARAGE_BUILDING_TYPE_ERROR_TEXT)
        containsValidationError(GARAGE_OWNERSHIP_ERROR_TEXT)

        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)

        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(FIRE_ALARM_24_7_ERROR_TEXT)
        containsValidationError(ACCESS_CONTROL_SYSTEM_HEATING_ERROR_TEXT)
        containsValidationError(WATER_ELECTRICITY_ERROR_TEXT)
        containsValidationError(AUTOMATIC_GATES_CCTV_ERROR_TEXT)
        containsValidationError(SECURITY_INSPECTION_PIT_ERROR_TEXT)
        containsValidationError(CAR_WASH_AUTO_REPAIR_ERROR_TEXT)

        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(HAGGLE_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllSellGarageErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_AREA_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_NAME_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_TYPE_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_PARKING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_BUILDING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_OWNERSHIP_ERROR_TEXT)

        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)

        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(FIRE_ALARM_24_7_ERROR_TEXT)
        doesNotContainValidationError(ACCESS_CONTROL_SYSTEM_HEATING_ERROR_TEXT)
        doesNotContainValidationError(WATER_ELECTRICITY_ERROR_TEXT)
        doesNotContainValidationError(AUTOMATIC_GATES_CCTV_ERROR_TEXT)
        doesNotContainValidationError(SECURITY_INSPECTION_PIT_ERROR_TEXT)
        doesNotContainValidationError(CAR_WASH_AUTO_REPAIR_ERROR_TEXT)

        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkContainsAllRentGarageErrors() {
        containsValidationError(ADDRESS_ERROR_TEXT)
        containsValidationError(GARAGE_AREA_ERROR_TEXT)
        containsValidationError(GARAGE_NAME_ERROR_TEXT)
        containsValidationError(GARAGE_TYPE_ERROR_TEXT)
        containsValidationError(GARAGE_PARKING_TYPE_ERROR_TEXT)
        containsValidationError(GARAGE_BUILDING_TYPE_ERROR_TEXT)
        containsValidationError(GARAGE_OWNERSHIP_ERROR_TEXT)

        containsValidationError(IMAGE_ERROR_TEXT)
        containsValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        containsValidationError(VIDEO_URL_ERROR_TEXT)

        containsValidationError(DESCRIPTION_ERROR_TEXT)
        containsValidationError(FIRE_ALARM_24_7_ERROR_TEXT)
        containsValidationError(ACCESS_CONTROL_SYSTEM_HEATING_ERROR_TEXT)
        containsValidationError(WATER_ELECTRICITY_ERROR_TEXT)
        containsValidationError(AUTOMATIC_GATES_CCTV_ERROR_TEXT)
        containsValidationError(SECURITY_INSPECTION_PIT_ERROR_TEXT)
        containsValidationError(CAR_WASH_AUTO_REPAIR_ERROR_TEXT)

        containsValidationError(PRICE_ERROR_TEXT)
        containsValidationError(PREPAYMENT_ERROR_TEXT)
        containsValidationError(AGENT_FEE_ERROR_TEXT)
        containsValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ELECTRICITY_ERROR_TEXT)
        containsValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun PublicationFormRobot.checkDoesNotContainAllRentGarageErrors() {
        doesNotContainValidationError(ADDRESS_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_AREA_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_NAME_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_TYPE_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_PARKING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_BUILDING_TYPE_ERROR_TEXT)
        doesNotContainValidationError(GARAGE_OWNERSHIP_ERROR_TEXT)

        doesNotContainValidationError(IMAGE_ERROR_TEXT)
        doesNotContainValidationError(IMAGES_ORDER_CHANGE_ALLOWED_TEXT)
        doesNotContainValidationError(VIDEO_URL_ERROR_TEXT)

        doesNotContainValidationError(DESCRIPTION_ERROR_TEXT)
        doesNotContainValidationError(FIRE_ALARM_24_7_ERROR_TEXT)
        doesNotContainValidationError(ACCESS_CONTROL_SYSTEM_HEATING_ERROR_TEXT)
        doesNotContainValidationError(WATER_ELECTRICITY_ERROR_TEXT)
        doesNotContainValidationError(AUTOMATIC_GATES_CCTV_ERROR_TEXT)
        doesNotContainValidationError(SECURITY_INSPECTION_PIT_ERROR_TEXT)
        doesNotContainValidationError(CAR_WASH_AUTO_REPAIR_ERROR_TEXT)

        doesNotContainValidationError(PRICE_ERROR_TEXT)
        doesNotContainValidationError(PREPAYMENT_ERROR_TEXT)
        doesNotContainValidationError(AGENT_FEE_ERROR_TEXT)
        doesNotContainValidationError(HAGGLE_RENT_PLEDGE_UTILITIES_ELECTRICITY_ERROR_TEXT)
        doesNotContainValidationError(ONLINE_SHOW_ERROR_TEXT)
    }

    private fun DispatcherRegistry.registerValidationError(
        requestFileName: String,
        responseFileName: String
    ) {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
                assetBody(requestFileName)
            },
            response {
                setResponseCode(400)
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerProfileValidationError() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                assetBody("publishForm/contacts.json")
            },
            response {
                setResponseCode(400)
                assetBody("publishForm/validateUserProfileError.json")
            }
        )
    }

    private fun DispatcherRegistry.registerUnknownProfileValidationError() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                assetBody("publishForm/contacts.json")
            },
            response {
                setResponseCode(400)
                assetBody("publishForm/validationUnknownError.json")
            }
        )
    }

    companion object {

        const val ADDRESS_ERROR_TEXT = "?????????????? ??????????"
        const val APARTMENT_NUMBER_ERROR_TEXT = "?????????????? ?????????? ????????????????"
        const val ROOMS_COUNT_ERROR_TEXT = "???????????????????????? ???????????????????? ????????????"
        const val ROOMS_OFFERED_ERROR_TEXT = "???????????????????????? ???????????????????? ???????????? ?? ????????????"
        const val TOTAL_AREA_ERROR_TEXT = "?????????????? ?????????? ??????????????"
        const val LIVING_AREA_ERROR_TEXT = "?????????????? ?????????? ??????????????"
        const val KITCHEN_AREA_ERROR_TEXT = "?????????????? ?????????????? ??????????"
        const val ROOM_AREA_1_ERROR_TEXT = "?????????????? ?????????????? 1 ??????????????"
        const val ROOM_AREA_2_ERROR_TEXT = "?????????????? ?????????????? 2 ??????????????"
        const val ROOM_AREA_3_ERROR_TEXT = "?????????????? ?????????????? 3 ??????????????"
        const val ROOM_AREA_4_ERROR_TEXT = "?????????????? ?????????????? 4 ??????????????"
        const val ROOM_AREA_5_ERROR_TEXT = "?????????????? ?????????????? 5 ??????????????"
        const val ROOM_AREA_6_ERROR_TEXT = "?????????????? ?????????????? 6 ??????????????"
        const val ROOM_AREA_7_ERROR_TEXT = "?????????????? ?????????????? 7 ??????????????"
        const val FLOOR_ERROR_TEXT = "?????????????? ????????"
        const val BATHROOM_ERROR_TEXT = "?????????????? ??????????????"
        const val BALCONY_ERROR_TEXT = "?????????????? ????????????"
        const val FLOOR_COVERING_ERROR_TEXT = "?????????????? ???????????????? ????????"
        const val RENOVATION_ERROR_TEXT = "?????????????? ????????????"
        const val WINDOW_VIEW_ERROR_TEXT = "?????????????? ?????? ???? ????????"
        const val PROPERTY_STATUS_ERROR_TEXT = "?????????????? ???????????? ??????????"
        const val IMAGE_ERROR_TEXT = "???????????????? ????????????????????"
        const val IMAGES_ORDER_CHANGE_ALLOWED_TEXT = "?????????????? ?????????? ???? ?????????? ????????"
        const val VIDEO_URL_ERROR_TEXT = "???????????????? ??????????"
        const val HOUSE_READY_ERROR_TEXT = "?????????????? ???????? ???? ??????"
        const val BUILD_YEAR_ERROR_TEXT = "?????????????? ?????? ??????????????????"
        const val CEILING_HEIGHT_ERROR_TEXT = "?????????????? ???????????? ????????????????"
        const val TOTAL_FLOORS_ERROR_TEXT = "?????????????? ???????????????????? ????????????"
        const val PARKING_TYPE_ERROR_TEXT = "?????????????? ?????? ????????????????"
        const val BUILDING_TYPE_ERROR_TEXT = "?????????????? ?????? ????????"
        const val DESCRIPTION_ERROR_TEXT = "?????????????? ????????????????"
        const val INTERNET_AND_REFRIGERATOR_ERROR_TEXT = "????????????????: ?????????????? ???????? ???? ????????????????\n" +
            "??????????????????????: ?????????????? ???????? ???? ??????????????????????"
        const val INTERNET_AND_FURNITURE_ERROR_TEXT = "????????????????: ?????????????? ???????? ???? ????????????????\n" +
            "???????????? ????????????????: ?????????????? ???????? ???? ???????????? ???? ??????????"
        const val FURNITURE_AND_AIRCONDITION_ERROR_TEXT = "???????????? ????????????????:" +
            " ?????????????? ???????? ???? ???????????? ???? ??????????\n??????????????????????: ?????????????? ???????? ???? ??????????????????????"
        const val FURNITURE_AND_LIFT_ERROR_TEXT = "???????????? ????????????????????:" +
            " ?????????????? ???????? ???? ???????????? ?? ????????????????\n????????: ?????????????? ???????? ???? ????????"
        const val RUBBISH_CHUTE_AND_SECURITY_ERROR_TEXT = "????????????????????????:" +
            " ?????????????? ???????? ???? ????????????????????????\n????????????????: ?????????????? ???????? ???? ????????????????"
        const val CLOSED_TERRITORY_ERROR_TEXT = "???????????????? ????????????????????:" +
            " ?????????????? ???????? ???? ???????????????? ????????????????????"
        const val WASHING_MACHINE_AND_CLOSED_TERRITORY_ERROR_TEXT = "???????????????????? ????????????:" +
            " ?????????????? ???????? ???? ???????????????????? ????????????\n" +
            "???????????????? ????????????????????: ?????????????? ???????? ???? ???????????????? ????????????????????"
        const val DISHWASHER_AND_TELEVISION_ERROR_TEXT = "??????????????????????:" +
            " ?????????????? ???????? ???? ?????????????????????????? ????????????\n??????????????????: ?????????????? ???????? ???? ??????????????????"
        const val CHILDREN_AND_PETS_ERROR_TEXT = "?????????? ??\u00A0????????????:" +
            " ?????????????? ?????????? ???? ?? ????????????\n?????????? ??\u00A0??????????????????: ?????????????? ?????????? ???? ?? ??????????????????"
        const val PRICE_ERROR_TEXT = "?????????????? ????????"
        const val PREPAYMENT_ERROR_TEXT = "?????????????? ????????????????????"
        const val AGENT_FEE_ERROR_TEXT = "?????????????? ???????????????? ????????????"
        const val HAGGLE_MORTGAGE_ERROR_TEXT = "????????: ?????????????? ???????????????? ???? ????????\n" +
            "??????????????: ?????????????? ???????????????? ???? ??????????????"
        const val HAGGLE_ERROR_TEXT = "????????: ?????????????? ???????????????? ???? ????????"
        const val HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT = "????????: ?????????????? ???????????????? ???? ????????\n" +
            "??????????: ?????????????? ?????????? ???? ??????????\n" +
            "????????. ????????????: ?????????????? ???????????????? ???? ???????????????????????? ????????????"
        const val HAGGLE_RENT_PLEDGE_UTILITIES_ELECTRICITY_ERROR_TEXT =
            "$HAGGLE_RENT_PLEDGE_UTILITIES_ERROR_TEXT\n" +
                "????????????????????????????: ?????????????? ???????????????? ???? ????????????????????????????"
        const val HOUSE_AREA_ERROR_TEXT = "?????????????? ?????????????? ????????"
        const val HOUSE_FLOORS_ERROR_TEXT = "?????????????? ???????????????????? ????????????"
        const val HOUSE_TYPE_ERROR_TEXT = "???????????????????????? ?????? ????????"
        const val TOILET_ERROR_TEXT = "?????????????? ???????????????????????? ??????????????"
        const val SHOWER_ERROR_TEXT = "?????????????? ???????????????????????? ????????"
        const val LOT_AREA_UNIT_ERROR_TEXT = "?????????????? ?????????????? ?????????????????? ?????????????? ??????????????"
        const val LOT_AREA_ERROR_TEXT = "?????????????? ?????????????? ??????????????"
        const val LOT_TYPE_ERROR_TEXT = "?????????????? ?????? ??????????????"
        const val GARAGE_AREA_ERROR_TEXT = "?????????????? ?????????????? ????????????"
        const val GARAGE_TYPE_ERROR_TEXT = "?????????????? ?????? ????????????"
        const val GARAGE_PARKING_TYPE_ERROR_TEXT = "?????????????? ?????? ????????????????"
        const val GARAGE_BUILDING_TYPE_ERROR_TEXT = "?????????????? ????????????????"
        const val GARAGE_OWNERSHIP_ERROR_TEXT = "?????????????? ????????????"
        const val GARAGE_NAME_ERROR_TEXT = "?????????????? ?????????????????? ??????"
        const val SEWERAGE_ELECTRICITY_SUPPLY_ERROR_TEXT = "??????????????????????: " +
            "?????????????? ???????? ???? ??????????????????????\n??????????????????????: ?????????????? ???????? ???? ??????????????????????????"
        const val GAS_SUPPLY_BILLIARD_ERROR_TEXT = "??????: ?????????????? ???????? ???? ??????\n" +
            "??????????????: ?????????????? ???????? ???? ??????????????"
        const val SAUNA_POOL_ERROR_TEXT = "??????????: ?????????????? ???????? ???? ??????????\n" +
            "??????????????: ?????????????? ???????? ???? ??????????????"
        const val PMG_KITCHEN_ERROR_TEXT = "?????????????????????? ??????: ?????????????? ???????? ???? ?????????????????????? ??????\n" +
            "??????????: ?????????????? ???????? ???? ??????????"
        const val HEATING_WATER_SUPPLY_ERROR_TEXT = "??????????????????: ?????????????? ???????? ???? ??????????????????\n" +
            "????????????????????: ?????????????? ???????? ???? ??????????????????????????"
        const val FIRE_ALARM_24_7_ERROR_TEXT =
            "???????????????? ????????????????????????: ?????????????? ???????? ???? ???????????????? ????????????????????????\n" +
                "???????????? ????\u00A0???????????? 24/7: ?????????????? ???????? ???? ???????????? 24/7"
        const val ACCESS_CONTROL_SYSTEM_HEATING_ERROR_TEXT =
            "???????????????????? ??????????????: ?????????????? ???????? ???? ???????????????????? ??????????????\n" +
                "??????????????????: ?????????????? ???????? ???? ??????????????????"
        const val WATER_ELECTRICITY_ERROR_TEXT = "????????????????????: ?????????????? ???????? ???? ??????????????????????????\n" +
            "??????????????????????: ?????????????? ???????? ???? ??????????????????????????"
        const val AUTOMATIC_GATES_CCTV_ERROR_TEXT =
            "???????????????????????????? ????????????: ?????????????? ???????? ???? ???????????????????????????? ????????????\n" +
                "??????????????????????????????: ?????????????? ???????? ???? ??????????????????????????????"
        const val SECURITY_INSPECTION_PIT_ERROR_TEXT = "????????????: ?????????????? ???????? ???? ????????????\n" +
            "?????????????????? ??????: ?????????????? ???????? ???? ?????????????????? ??????"
        const val CAR_WASH_AUTO_REPAIR_ERROR_TEXT = "??????????????????: ?????????????? ???????? ???? ??????????????????\n" +
            "????????????????????: ?????????????? ???????? ???? ????????????????????"
        const val DEAL_STATUS_ERROR_TEXT = "?????????????? ?????? ????????????"
        const val ONLINE_SHOW_ERROR_TEXT = "?????????????? ???????????????? ???? ???????????? ??????????"
        const val PROFILE_NAME_ERROR_TEXT = "???????????????????????? ??????"
        const val PROFILE_EMAIL_ERROR_TEXT = "???????????????????????? email"
        const val PROFILE_PHONE_ERROR_TEXT = "???????????????? ???????????? ??????????????"
    }
}
