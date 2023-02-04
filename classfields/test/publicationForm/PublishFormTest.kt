package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.createImageAndGetUriString
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.registerGetContentIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.robot.PublicationWizardRobot
import com.yandex.mobile.realty.core.robot.performOnAddPhoneScreen
import com.yandex.mobile.realty.core.robot.performOnAddressRootScreen
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnChooseMediaScreen
import com.yandex.mobile.realty.core.robot.performOnImagesPickerScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationCompleteScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationWizardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AddOfferModeDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author solovevai on 14.06.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class PublishFormTest : BasePublishFormTest() {

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
    fun checkPublishSellApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressApartment()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerSellApartmentFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerSellApartmentFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellApartment()
            fillSellApartmentSteps(imageUri)
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            fillAllSellApartmentValues(imageUri, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentLongApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressApartment()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentLongApartmentFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentLongApartmentFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongApartment()
            fillRentApartmentSteps(imageUri, PaymentPeriod.PER_MONTH)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }
            fillAllRentApartmentValues(imageUri, PaymentPeriod.PER_MONTH, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentShortApartment() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressApartment()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentShortApartmentFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentShortApartmentFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortApartment()
            fillRentApartmentSteps(imageUri, PaymentPeriod.PER_DAY)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            fillAllRentApartmentValues(imageUri, PaymentPeriod.PER_DAY, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishSellRoom() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressRoom()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerSellRoomFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerSellRoomFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellRoom()
            fillSellRoomSteps(imageUri)
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            fillAllSellRoomValues(imageUri, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentLongRoom() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressRoom()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentLongRoomFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentLongRoomFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongRoom()
            fillRentRoomSteps(imageUri, PaymentPeriod.PER_MONTH)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            fillAllRentRoomValues(imageUri, PaymentPeriod.PER_MONTH, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentShortRoom() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressRoom()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentShortRoomFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentShortRoomFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortRoom()
            fillRentRoomSteps(imageUri, PaymentPeriod.PER_DAY)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            fillAllRentRoomValues(imageUri, PaymentPeriod.PER_DAY, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishSellHouse() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressHouse()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerSellHouseFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerSellHouseFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellHouse()
            fillSellHouseSteps(imageUri)
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            fillAllSellHouseValues(imageUri, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentLongHouse() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressHouse()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentLongHouseFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentLongHouseFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongHouse()
            fillRentHouseSteps(imageUri, PaymentPeriod.PER_MONTH)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            fillAllRentHouseValues(imageUri, PaymentPeriod.PER_MONTH, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentShortHouse() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressHouse()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentShortHouseFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentShortHouseFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortHouse()
            fillRentHouseSteps(imageUri, PaymentPeriod.PER_DAY)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            fillAllRentHouseValues(imageUri, PaymentPeriod.PER_DAY, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishSellLot() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressLot()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerSellLotFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerSellLotFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellLot()
            fillSellLotSteps(imageUri)
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellLotExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            fillAllSellLotValues(imageUri, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishSellGarage() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressGarage()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerSellGarageFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerSellGarageFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellGarage()
            fillGarageSteps(imageUri)
        }

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            fillAllSellGarageValues(imageUri, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishRentLongGarage() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        val imageUri = createImageAndGetUriString()
        configureWebServer {
            registerBeforePublish()
            registerGetNearAuroraAddressGarage()
            registerUploadPhoto(imageUri)
            registerModifiedUserProfilePatch()
            registerValidation("publishForm/offerRentLongGarageFull.json")
            registerDraft()
            registerPublishDraft("publishForm/offerRentLongGarageFull.json")
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongGarage()
            fillGarageSteps(imageUri, PaymentPeriod.PER_MONTH)
        }

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasRentLongGarageCollapsedToolbarTitle() }
            fillAllRentGarageValues(imageUri, withWizardFields = false)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }

        checkSuccessPublication()
    }

    @Test
    fun checkPublishBadKarma() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerEmptyUserOffers()
            registerUserProfile()
            registerUserProfile()
            registerUserProfilePatch()
            registerValidationWithErrorBadKarma()
        }
        activityTestRule.launchActivity()
        registerResultOkIntent(matchesBadKarmaSupportIntent(), null)

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectSellApartment()
            performOnAddressRootScreen {
                tapOn(lookup.matchesCloseButton())
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil { isBlockedUserViewShown() }
            isErrorViewStateMatches("/PublishFormTest/checkPublishBadKarma")
            tapOn(lookup.matchesContactSupportButton())
        }
        intended(matchesBadKarmaSupportIntent())
    }

    private fun matchesBadKarmaSupportIntent(): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие страницы для обеления кармы юзера",
            CoreMatchers.allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData("https://passport.yandex.ru/passport?mode=userapprove")
            )
        )
    }

    @Test
    fun checkPublishCommonError() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerEmptyUserOffers()
            registerUserProfileWithError()
            registerUserProfile()
            registerUserProfileWithError()
            registerUserProfile()
            registerUserProfilePatchWithError()
            registerUserProfilePatch()
            registerValidationSuccess()
            registerDraft()
            registerPublishDraftWithError()
            registerUserProfilePatch()
            registerValidationSuccess()
            registerDraft()
            registerPublishDraftSuccess()
            registerAfterPublish()
        }
        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            collapseAppBar()
            waitUntil { isErrorViewShown() }
            isErrorViewStateMatches("/PublishFormTest/checkPublishCommonErrorWizard")
            onView(lookup.matchesErrorRetryButton()).tapOn()
            selectSellApartment()

            performOnAddressRootScreen {
                tapOn(lookup.matchesCloseButton())
            }
        }

        selectContinueDraft()

        performOnPublicationFormScreen {
            collapseAppBar()
            waitUntil { isErrorViewShown() }
            isErrorViewStateMatches("/PublishFormTest/checkPublishCommonLoadError")
            onView(lookup.matchesErrorRetryButton()).tapOn()

            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            waitUntil { isErrorViewShown() }
            isErrorViewStateMatches("/PublishFormTest/checkPublishCommonError")
            onView(lookup.matchesErrorRetryButton()).tapOn()
            waitUntil { isErrorViewShown() }
            onView(lookup.matchesErrorRetryButton()).tapOn()
        }

        checkSuccessPublication()
    }

    private fun checkSuccessPublication() {
        performOnPublicationCompleteScreen {
            waitUntil { isPromoShown() }
            isTitleShown()
            isDescriptionShown()
            isImageShown()
            onView(lookup.matchesProceedButton()).tapOn()
        }

        onScreen<UserOffersScreen> {
            waitUntil { listView.contains(offerSnippet("1")) }
        }
    }

    private fun pickWizardImage(uri: String) {
        performOnImagesPickerScreen {
            registerGetContentIntent(uri)
            waitUntil { isLargeAddImageButtonShown() }
            tapOn(lookup.matchesLargeAddImagesButton())
            performOnChooseMediaScreen {
                tapOn(lookup.matchesGalleryButton())
            }
            waitUntil { isSuccessImageItemShown(1, uri) }
        }
    }

    private fun PublicationWizardRobot.fillSellApartmentSteps(imageUri: String) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitFlatNumberStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesApartmentNumberField())
        onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsTotalStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesRoomsCountField())
        onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

        waitApartmentAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardTotalAreaField())
        onView(lookup.matchesWizardTotalAreaFieldValue()).typeText(TOTAL_AREA)
        scrollToPosition(lookup.matchesWizardLivingAreaField())
        onView(lookup.matchesWizardLivingAreaFieldValue()).typeText(LIVING_AREA)
        scrollToPosition(lookup.matchesKitchenAreaField())
        onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
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
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitFloorStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardFloorField())
        onView(lookup.matchesWizardFloorFieldValue()).typeText(FLOOR)
        scrollToPosition(lookup.matchesFloorsTotalField())
        onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField())
        onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDealStatusStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesDealStatusField())
        onView(lookup.matchesDealStatusSelectorSale()).tapOn()

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillRentApartmentSteps(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
    ) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitFlatNumberStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesApartmentNumberField())
        onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsTotalStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesRoomsCountField())
        onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

        waitApartmentAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardTotalAreaField())
        onView(lookup.matchesWizardTotalAreaFieldValue()).typeText(TOTAL_AREA)
        scrollToPosition(lookup.matchesWizardLivingAreaField())
        onView(lookup.matchesWizardLivingAreaFieldValue()).typeText(LIVING_AREA)
        scrollToPosition(lookup.matchesKitchenAreaField())
        onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
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
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitFloorStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardFloorField())
        onView(lookup.matchesWizardFloorFieldValue()).typeText(FLOOR)
        scrollToPosition(lookup.matchesFloorsTotalField())
        onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField(paymentPeriod))
        onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillSellRoomSteps(imageUri: String) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitFlatNumberStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesApartmentNumberField())
        onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsTotalStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesRoomsCountField())
        onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

        waitSellRoomsOfferedStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesRoomsOfferedField())
        onView(lookup.matchesWizardRoomsOfferedSelectorSix()).tapOn()

        waitApartmentAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardTotalAreaField())
        onView(lookup.matchesWizardTotalAreaFieldValue()).typeText(TOTAL_AREA)
        scrollToPosition(lookup.matchesKitchenAreaField())
        onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
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
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitFloorStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardFloorField())
        onView(lookup.matchesWizardFloorFieldValue()).typeText(FLOOR)
        scrollToPosition(lookup.matchesFloorsTotalField())
        onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField())
        onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDealStatusStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesDealStatusField())
        onView(lookup.matchesDealStatusSelectorSale()).tapOn()

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillRentRoomSteps(
        imageUri: String,
        paymentPeriod: PaymentPeriod,
    ) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitFlatNumberStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesApartmentNumberField())
        onView(lookup.matchesApartmentNumberFieldValue()).typeText(APARTMENT_NUMBER)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsTotalStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesRoomsCountField())
        onView(lookup.matchesWizardRoomsSelectorSeven()).tapOn()

        waitRentRoomsOfferedStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesRoomsOfferedField())
        onView(lookup.matchesWizardRoomsOfferedSelectorSix()).tapOn()

        waitApartmentAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardTotalAreaField())
        onView(lookup.matchesWizardTotalAreaFieldValue()).typeText(TOTAL_AREA)
        scrollToPosition(lookup.matchesKitchenAreaField())
        onView(lookup.matchesKitchenAreaFieldValue()).typeText(KITCHEN_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitRoomsAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
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
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitFloorStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardFloorField())
        onView(lookup.matchesWizardFloorFieldValue()).typeText(FLOOR)
        scrollToPosition(lookup.matchesFloorsTotalField())
        onView(lookup.matchesFloorsTotalFieldValue()).typeText(TOTAL_FLOORS)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField(paymentPeriod))
        onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillSellHouseSteps(imageUri: String) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitHouseAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesHouseAreaField())
        onView(lookup.matchesHouseAreaFieldValue()).typeText(HOUSE_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitLotAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesLotAreaUnitField())
        onView(lookup.matchesLotAreaUnitSelectorHectare()).tapOn()
        scrollToPosition(lookup.matchesLotAreaField())
        onView(lookup.matchesLotAreaFieldValue()).typeText(LOT_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField())
        onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDealStatusStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesDealStatusField())
        onView(lookup.matchesDealStatusSelectorSale()).tapOn()

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillRentHouseSteps(
        imageUri: String,
        paymentPeriod: PaymentPeriod? = null,
    ) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitHouseAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesHouseAreaField())
        onView(lookup.matchesHouseAreaFieldValue()).typeText(HOUSE_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitLotAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesLotAreaUnitField())
        onView(lookup.matchesLotAreaUnitSelectorHectare()).tapOn()
        scrollToPosition(lookup.matchesLotAreaField())
        onView(lookup.matchesLotAreaFieldValue()).typeText(LOT_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField(paymentPeriod))
        onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillSellLotSteps(imageUri: String) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitLotAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesLotAreaUnitField())
        onView(lookup.matchesLotAreaUnitSelectorHectare()).tapOn()
        scrollToPosition(lookup.matchesLotAreaField())
        onView(lookup.matchesLotAreaFieldValue()).typeText(LOT_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField())
        onView(lookup.matchesPriceFieldValue()).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDealStatusStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesDealStatusField())
        onView(lookup.matchesDealStatusSelectorSale()).tapOn()

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.fillGarageSteps(
        imageUri: String,
        paymentPeriod: PaymentPeriod? = null,
    ) {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }

        waitGarageTypeStep()
        doesNotContainProceedButton()
        scrollToPosition(lookup.matchesGarageTypeField())
        onView(lookup.matchesGarageTypeSelectorBox()).tapOn()

        waitGarageAreaStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesGarageAreaField())
        onView(lookup.matchesGarageAreaFieldValue()).typeText(GARAGE_AREA)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitGarageOwnershipStep()
        scrollToPosition(lookup.matchesGarageOwnershipField())
        onView(lookup.matchesGarageOwnershipSelectorCooperative()).tapOn()

        waitPhotosStep()
        waitUntil { containsNotActivatedProceedButton() }
        pickWizardImage(imageUri)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPriceStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesPriceField(paymentPeriod))
        onView(lookup.matchesPriceFieldValue(paymentPeriod)).typeText(PRICE)
        waitUntil { containsActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardHaggleField()).tapOn()
        tapOn(lookup.matchesWizardProceedButton())

        waitDescriptionStep()
        waitUntil { containsNotActivatedProceedButton() }
        scrollToPosition(lookup.matchesWizardDescriptionField())
        onView(lookup.matchesWizardDescriptionField()).typeText(DESCRIPTION)
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitPhonesStep()
        addPhone()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())

        waitContactsStep()
        changeContacts()
        waitUntil { containsActivatedProceedButton() }
        tapOn(lookup.matchesWizardProceedButton())
    }

    private fun PublicationWizardRobot.addPhone() {
        tapOn(lookup.matchesContactPhoneFieldSelector("+7111*****44"))
        scrollToPosition(lookup.matchesAddContactPhoneButton()).tapOn()

        waitNewPhoneStep()
        performOnAddPhoneScreen {
            tapOn(lookup.matchesPhoneNumberClearButton())
            typeText(lookup.matchesPhoneNumberFieldValue(), NEW_PHONE)
            tapOn(lookup.matchesSubmitPhoneButton())
        }

        waitConfirmationCodeStep()
        performOnAddPhoneScreen {
            typeText(
                viewMatcher = lookup.matchesConfirmCodeFieldValue(),
                text = CONFIRMATION_CODE,
                closeKeyboard = false
            )
        }

        waitPhonesStep()
    }

    private fun PublicationWizardRobot.changeContacts() {
        tapOn(lookup.matchesContactNameField())
        tapOn(lookup.matchesContactNameClearButton())
        tapOn(lookup.matchesContactEmailField())
        tapOn(lookup.matchesContactEmailClearButton())
        waitUntil { containsNotActivatedProceedButton() }
        typeText(lookup.matchesContactNameFieldValue(), NEW_NAME)
        typeText(lookup.matchesContactEmailFieldValue(), NEW_EMAIL)
    }

    private fun selectContinueDraft() {
        onScreen<UserOffersScreen> {
            emptyViewAddButton
                .waitUntil { isCompletelyDisplayed() }
                .click()

            onScreen<AddOfferModeDialogScreen> {
                titleView.waitUntil { isCompletelyDisplayed() }
                draftButton.click()
            }
        }
    }

    private fun DispatcherRegistry.registerUserOfferCard() {
        register(
            request {
                method("GET")
                path("2.0/user/me/offers/1234/card")
            },
            response {
                assetBody("userOffer/userOfferPublishedNoVasFree.json")
            }
        )
    }

    private fun DispatcherRegistry.registerBeforePublish() {
        registerNoRequiredFeatures()
        registerUserProfile()
        registerUserProfile()
        registerEmptyUserOffers()
        registerPassportPhoneBind(NEW_PHONE)
        registerPassportPhoneConfirm(CONFIRMATION_CODE)
        registerUserProfileWithNewPhone()
        registerUserProfileWithNewPhone()
    }

    private fun DispatcherRegistry.registerAfterPublish() {
        registerUserOfferCard()
        registerNoRequiredFeatures()
        registerUserProfile()
        registerUserProfile()
        registerUserOffersOneOffer()
    }

    private fun DispatcherRegistry.registerModifiedUserProfilePatch() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                assetBody("publishForm/contactsModified.json")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfileWithNewPhone() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("publishForm/userOwnerWithNewPhone.json")
            }
        )
    }

    private fun DispatcherRegistry.registerValidationWithErrorBadKarma() {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
            },
            response {
                setResponseCode(400)
                setBody(
                    """{
                                            "error": {
                                                "codename": "USER_LOOKS_LIKE_A_SPAMMER"
                                            }
                                        }"""
                )
            }
        )
    }

    private fun DispatcherRegistry.registerValidationSuccess() {
        register(
            request {
                method("POST")
                path("1.0/user/offers/validation")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerPublishDraftSuccess() {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/draft/1234")
            },
            response {
                setResponseCode(200)
            }
        )
    }

    private fun DispatcherRegistry.registerPublishDraftWithError() {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/draft/1234")
            },
            response {
                setResponseCode(400)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfilePatchWithError() {
        register(
            request {
                method("PATCH")
                path("1.0/user")
                assetBody("publishForm/contacts.json")
            },
            response {
                setResponseCode(400)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfileWithError() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                setResponseCode(400)
            }
        )
    }
}
