package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.PublicationFormEditActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author solovevai on 18.10.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ValidateOfferTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule = PublicationFormEditActivityTestRule(
        offerId = "1234",
        createTime = Date(),
        launchActivity = false
    )

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun validateSellApartment() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellApartment.json")
            registerNoTrustedStatus()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellApartmentFull.json",
                "publishForm/validateSellApartmentError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            waitUntil { doesNotContainMosRuBlock() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateSellApartment/content")
        }
    }

    @Test
    fun validateRentLongApartment() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentLongApartment.json")
            registerNoTrustedStatus()
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongApartmentFull.json",
                "publishForm/validateRentApartmentError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            waitUntil { doesNotContainMosRuBlock() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentLongApartment/content")
        }
    }

    @Test
    fun validateRentShortApartment() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentShortApartment.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentShortApartmentFull.json",
                "publishForm/validateRentApartmentError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentShortApartment/content")
        }
    }

    @Test
    fun validateSellRoom() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellRoom.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellRoomFull.json",
                "publishForm/validateSellRoomError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateSellRoom/content")
        }
    }

    @Test
    fun validateRentLongRoom() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentLongRoom.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongRoomFull.json",
                "publishForm/validateRentRoomError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentLongRoom/content")
        }
    }

    @Test
    fun validateRentShortRoom() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentShortRoom.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentShortRoomFull.json",
                "publishForm/validateRentRoomError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentShortRoom/content")
        }
    }

    @Test
    fun validateSellHouse() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellHouse.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellHouseFull.json",
                "publishForm/validateSellHouseError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateSellHouse/content")
        }
    }

    @Test
    fun validateRentLongHouse() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentLongHouse.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongHouseFull.json",
                "publishForm/validateRentHouseError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentLongHouse/content")
        }
    }

    @Test
    fun validateRentShortHouse() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentShortHouse.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentShortHouseFull.json",
                "publishForm/validateRentHouseError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentShortHouse/content")
        }
    }

    @Test
    fun validateSellLot() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellLot.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellLotFull.json",
                "publishForm/validateSellLotError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateSellLot/content")
        }
    }

    @Test
    fun validateSellGarage() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellGarage.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellGarageFull.json",
                "publishForm/validateSellGarageError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateSellGarage/content")
        }
    }

    @Test
    fun validateRentLongGarage() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferRentLongGarage.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerRentLongGarageFull.json",
                "publishForm/validateRentLongGarageError.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
            waitUntil { isImageGalleryShown() }
            scrollToTop()
            isContentMatches("ValidateOfferTest/validateRentLongGarage/content")
        }
    }

    @Test
    fun showDialogOnUnknownValidationError() {
        configureWebServer {
            registerUserProfile()
            registerEditOffer("publishForm/editOfferSellApartment.json")
            registerUserProfilePatch()
            registerValidationError(
                "publishForm/offerSellApartmentFull.json",
                "publishForm/validationUnknownError.json"
            )
        }
        activityTestRule.launchActivity()
        registerMarketIntent()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            collapseAppBar()
            scrollToPosition(lookup.matchesPublishButton()).tapOn()

            performOnConfirmationDialog {
                waitUntil { isUpdateRequiredDialogShown() }
                tapOn(lookup.matchesPositiveButton())
                intended(matchesMarketIntent())
            }
        }
    }

    private fun DispatcherRegistry.registerValidationError(
        requestFileName: String,
        responseFileName: String
    ) {
        register(
            request {
                method("PUT")
                path("1.0/user/offers/1234")
                assetBody(requestFileName)
            },
            response {
                setResponseCode(400)
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerNoTrustedStatus() {
        val body = """
        {
          "response": {
            "content": {
              "trustedOfferInfo": {
                "ownerTrustedStatus": null
              }
            }
          }
        }
        """.trimIndent()

        register(
            request {
                method("GET")
                path("2.0/user/me/offers/1234/card")
            },
            response {
                setBody(body)
            }
        )
    }
}
