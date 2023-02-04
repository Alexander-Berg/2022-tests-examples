package com.yandex.mobile.realty.test.publicationForm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.UserOfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.doesNotExist
import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.PublicationFormRobot
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.robot.performOnUserOfferCardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isEnabled
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isNotEnabled
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author solovevai on 22.09.2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class EditFormAddressAndFlatNumberTest : BasePublishFormTest() {

    private val authorizationRule = AuthorizationRule()
    private val activityTestRule =
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

    @Test
    fun checkEditOldOfferSellApartment() {
        configureWebServer {
            registerRequests("publishForm/editOfferSellApartment.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentLongApartment() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentLongApartment.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongApartmentCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentShortApartment() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentShortApartment.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortApartmentCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberDisabled()
        }
    }

    @Test
    fun checkEditOldOfferSellRoom() {
        configureWebServer {
            registerRequests("publishForm/editOfferSellRoom.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellRoomCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentLongRoom() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentLongRoom.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongRoomCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentShortRoom() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentShortRoom.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortRoomCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberDisabled()
        }
    }

    @Test
    fun checkEditOldOfferSellHouse() {
        configureWebServer {
            registerRequests("publishForm/editOfferSellHouse.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellHouseCollapsedToolbarTitle() }
            checkAddressDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentLongHouse() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentLongHouse.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongHouseCollapsedToolbarTitle() }
            checkAddressDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentShortHouse() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentShortHouse.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentShortHouseCollapsedToolbarTitle() }
            checkAddressDisabled()
        }
    }

    @Test
    fun checkEditOldOfferSellLot() {
        configureWebServer {
            registerRequests("publishForm/editOfferSellLot.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            checkAddressDisabled()
        }
    }

    @Test
    fun checkEditOldOfferSellGarage() {
        configureWebServer {
            registerRequests("publishForm/editOfferSellGarage.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellGarageCollapsedToolbarTitle() }
            checkAddressDisabled()
        }
    }

    @Test
    fun checkEditOldOfferRentLongGarage() {
        configureWebServer {
            registerRequests("publishForm/editOfferRentLongGarage.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasRentLongGarageCollapsedToolbarTitle() }
            checkAddressDisabled()
        }
    }

    @Test
    fun checkEditOldOfferNoFlatNumber() {
        configureWebServer {
            registerRequests("publishForm/editOfferSellApartmentNoFlatNumber.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            checkAddressDisabled()
            checkFlatNumberEnabled()
        }
    }

    @Test
    fun checkEditLessThan24hOldOffer() {
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerLessThan24hUserOfferCard()
            registerEditOffer("publishForm/editOfferSellApartment.json")
        }
        activityTestRule.launchActivity()

        openOfferForEdit()

        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            checkAddressEnabled()
            checkFlatNumberEnabled()
        }
    }

    private fun openOfferForEdit() {
        performOnUserOfferCardScreen {
            waitUntil { isEditButtonShown() }
            onView(lookup.matchesEditButton()).tapOn()
        }
    }

    private fun PublicationFormRobot.checkAddressDisabled() {
        onView(lookup.matchesAddressField()).check(matches(isNotEnabled()))
        onView(lookup.matchesAddressDisabledDescription()).check(matches(isCompletelyDisplayed()))
    }

    private fun PublicationFormRobot.checkAddressEnabled() {
        onView(lookup.matchesAddressField()).check(matches(isEnabled()))
        onView(lookup.matchesAddressDisabledDescription()).check(doesNotExist())
    }

    private fun PublicationFormRobot.checkFlatNumberDisabled() {
        onView(lookup.matchesApartmentNumberFieldValue()).check(matches(isNotEnabled()))
    }

    private fun PublicationFormRobot.checkFlatNumberEnabled() {
        onView(lookup.matchesApartmentNumberFieldValue()).check(matches(isEnabled()))
    }

    private fun DispatcherRegistry.registerRequests(editResponseFileName: String) {
        registerUserProfile()
        registerUserProfile()
        registerMoreThan24hUserOfferCard()
        registerEditOffer(editResponseFileName)
    }

    private fun DispatcherRegistry.registerLessThan24hUserOfferCard() {
        val creationDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1_000L + 10 * 1000)
        registerUserOfferCard(creationDate)
    }

    private fun DispatcherRegistry.registerMoreThan24hUserOfferCard() {
        val creationDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1_000L - 10 * 1000)
        registerUserOfferCard(creationDate)
    }

    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    private fun DispatcherRegistry.registerUserOfferCard(creationDate: Date) {
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
}
