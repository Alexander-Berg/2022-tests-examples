package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.clearExternalImagesDir
import com.yandex.mobile.realty.core.createImageOnExternalDir
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.robot.performOnUserOfferCardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author solovevai on 21.09.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class EditFormTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private var activityTestRule =
        UserOfferCardActivityTestRule(offerId = "1234", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        authorizationRule,
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        authorizationRule.setUserAuthorized()
    }

    @After
    fun clearImages() {
        clearExternalImagesDir()
    }

    @Test
    fun checkEditSellApartment() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferSellApartment.json",
                "publishForm/offerSellApartmentFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkSellApartmentFormIsFull(IMAGE_URI)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentLongApartment() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentLongApartment.json",
                "publishForm/offerRentLongApartmentFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentApartmentFormIsFull(IMAGE_URI, PaymentPeriod.PER_MONTH)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentShortApartment() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentShortApartment.json",
                "publishForm/offerRentShortApartmentFull.json"
            )
        }

        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentApartmentFormIsFull(IMAGE_URI, PaymentPeriod.PER_DAY)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditSellRoom() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferSellRoom.json",
                "publishForm/offerSellRoomFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkSellRoomFormIsFull(IMAGE_URI)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentLongRoom() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentLongRoom.json",
                "publishForm/offerRentLongRoomFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentRoomFormIsFull(IMAGE_URI, PaymentPeriod.PER_MONTH)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentShortRoom() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentShortRoom.json",
                "publishForm/offerRentShortRoomFull.json"
            )
        }

        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentRoomFormIsFull(IMAGE_URI, PaymentPeriod.PER_DAY)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditSellHouse() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferSellHouse.json",
                "publishForm/offerSellHouseFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkSellHouseFormIsFull(IMAGE_URI)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentLongHouse() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentLongHouse.json",
                "publishForm/offerRentLongHouseFull.json"
            )
        }

        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentHouseFormIsFull(IMAGE_URI, PaymentPeriod.PER_MONTH)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentShortHouse() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentShortHouse.json",
                "publishForm/offerRentShortHouseFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentHouseFormIsFull(IMAGE_URI, PaymentPeriod.PER_DAY)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditSellLot() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferSellLot.json",
                "publishForm/offerSellLotFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkSellLotFormIsFull(IMAGE_URI)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditSellGarage() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferSellGarage.json",
                "publishForm/offerSellGarageFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkSellGarageFormIsFull(IMAGE_URI)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    @Test
    fun checkEditRentLongGarage() {
        configureWebServer {
            registerRequests(
                "publishForm/editOfferRentLongGarage.json",
                "publishForm/offerRentLongGarageFull.json"
            )
        }
        createImageOnExternalDir(rColor = 0, gColor = 255, bColor = 0)
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { isImageGalleryShown() }
            checkRentGarageFormIsFull(IMAGE_URI)
            scrollToPosition(lookup.matchesPublishButton()).tapOn()
        }
        checkSuccessPublication()
    }

    private fun openOfferForEdit() {
        performOnUserOfferCardScreen {
            waitUntil { isEditButtonShown() }
            onView(lookup.matchesEditButton()).tapOn()
        }
    }

    private fun checkSuccessPublication() {
        performOnUserOfferCardScreen {
            waitUntil { isToastShown("Спасибо, изменения скоро будут опубликованы") }
            waitUntil { isEditButtonShown() }
        }
    }

    private fun DispatcherRegistry.registerRequests(
        editResponseFileName: String,
        publishResponseFileName: String
    ) {
        registerUserProfile()
        registerUserProfile()
        registerFreshUserOfferCard()
        registerEditOffer(editResponseFileName)
        registerUserProfile()
        registerFreshUserOfferCard()
        registerUserProfilePatch()
        registerPublishOffer(publishResponseFileName)
        registerFreshUserOfferCard()
        registerUserProfile()
        registerFreshUserOfferCard()
    }

    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    private fun DispatcherRegistry.registerFreshUserOfferCard() {
        val creationDate = Date()
        register(
            request {
                path("2.0/user/me/offers/1234/card")
            },
            response {
                setBody(
                    """
                        {
                            "response": {
                                "content": {
                                    "id": "1234",
                                    "uid": "402894",
                                    "publishingInfo": {
                                        "creationDate": "${sdf.format(creationDate)}",
                                        "status": "PUBLISHED"
                                    },
                                    "placement": { "free": {} },
                                    "vosLocation": {
                                        "rgid": "417899",
                                        "point": {
                                            "latitude": 59.96423,
                                            "longitude": 30.407164,
                                            "defined": true
                                        },
                                        "address": {
                                            "unifiedOneline":
                                            "Россия, Санкт-Петербург, Полюстровский проспект, 7",
                                            "apartment": "63"
                                        }
                                    },
                                    "sell": {
                                        "dealStatus": "DEAL_STATUS_SALE",
                                        "price": {
                                            "value": 4500000,
                                            "currency": "RUB",
                                            "priceType": "PER_OFFER",
                                            "pricingPeriod": "WHOLE_LIFE"
                                        }
                                    },
                                    "apartment": {
                                        "area": {
                                            "area": {
                                                "value": 50.0,
                                                "unit": "SQ_M"
                                            }
                                        }
                                    },
                                    "isFromFeed": false
                                }
                            }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private companion object {

        const val IMAGE_URI = "file:///sdcard/realty_images/test_image.jpeg"
    }
}
