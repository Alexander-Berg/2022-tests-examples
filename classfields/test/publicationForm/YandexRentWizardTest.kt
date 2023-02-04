package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.UserOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationWizardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.RentFlatFormScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.screen.WizardRentOutScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author andrey-bgm on 27/05/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class YandexRentWizardTest : BasePublishFormTest() {

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
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
    }

    @Test
    fun shouldNavigateToAndFromRentOutStep() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerYandexRentAvailable()

            registerNoRequiredFeatures()
            registerRentFormDraft()
            registerRentUserInfo()
            registerNotificationsConfiguration()
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongApartment()
            selectAddress()

            onScreen<WizardRentOutScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                root.isViewStateMatches(getTestRelatedFilePath("rentOutVariantsContent"))
                rejectButton.click()
            }

            waitFlatNumberStep()
            typeText(lookup.matchesApartmentNumberFieldValue(), APARTMENT_NUMBER)
            pressBack()

            onScreen<WizardRentOutScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
                acceptButton.click()
            }

            onScreen<RentFlatFormScreen> {
                waitUntil { listView.contains(addressItem) }
                addressItem.view.isTextEquals(AURORA_ADDRESS_WITHOUT_COUNTRY)
                pressBack()
            }

            onScreen<WizardRentOutScreen> {
                waitUntil { titleView.isCompletelyDisplayed() }
            }
        }
    }

    @Test
    fun shouldNotShowYandexRentStepWhenYandexRentNotAvailableAtGeo() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerYandexRentAvailable(false)
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongApartment()
            selectAddress()

            waitFlatNumberStep()
        }
    }

    @Test
    fun shouldNotShowYandexRentStepWhenRentShort() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerYandexRentAvailable()
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentShortApartment()
            selectAddress()

            waitFlatNumberStep()
        }
    }

    @Test
    fun shouldSkipYandexRentStepWhenGeoAvailableError() {
        configureWebServer {
            registerNoRequiredFeatures()
            registerUserProfile()
            registerUserProfile()
            registerUserOffersOneOffer()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerYandexRentAvailableError()
        }

        activityTestRule.launchActivity()

        onScreen<UserOffersScreen> {
            toolbarAddOfferButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnPublicationWizardScreen {
            selectRentLongApartment()
            selectAddress()

            waitFlatNumberStep()
        }
    }

    private fun DispatcherRegistry.registerYandexRentAvailable(
        available: Boolean = true
    ) {
        register(
            request {
                method("GET")
                path("2.0/rent/is-point-rent")
                queryParam("latitude", AURORA_LATITUDE.toString())
                queryParam("longitude", AURORA_LONGITUDE.toString())
            },
            response {
                setBody("{\"response\":{\"isPointInsidePolygon\":$available}}")
            }
        )
    }

    private fun DispatcherRegistry.registerYandexRentAvailableError() {
        register(
            request {
                method("GET")
                path("2.0/rent/is-point-rent")
                queryParam("latitude", AURORA_LATITUDE.toString())
                queryParam("longitude", AURORA_LONGITUDE.toString())
            },
            response {
                setResponseCode(400)
            }
        )
    }

    private fun selectAddress() {
        performOnAddressSelectScreen {
            waitUntil { isAddressContainerShown() }
            isAddressEquals(AURORA_ADDRESS)
            tapOn(lookup.matchesConfirmAddressButton())
        }
    }

    private fun DispatcherRegistry.registerRentFormDraft() {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me/flats/draft")
            },
            response {
                jsonBody {
                    "response" to jsonObject {}
                }
            }
        )
    }

    private fun DispatcherRegistry.registerRentUserInfo() {
        register(
            request {
                method("GET")
                path("2.0/rent/user/me")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "user" to jsonObject {
                            "person" to jsonObject {
                                "name" to FIRST_NAME
                                "surname" to LAST_NAME
                                "patronymic" to MIDDLE_NAME
                            }
                            "phone" to PHONE
                            "email" to EMAIL
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerNotificationsConfiguration() {
        register(
            request {
                method("GET")
                path("1.0/user/notifications")
                queryParam("deliveryTypes", "sms")
                queryParam("deliveryTypes", "email")
            },
            response {
                jsonBody {
                    "response" to jsonObject {
                        "values" to jsonArrayOf(
                            jsonObject {
                                "id" to "rent_marketing_campaigns"
                                "methods" to jsonArrayOf(
                                    jsonObject {
                                        "deliveryType" to "DELIVERY_TYPE_EMAIL"
                                        "enabled" to false
                                    },
                                    jsonObject {
                                        "deliveryType" to "DELIVERY_TYPE_SMS"
                                        "enabled" to false
                                    }
                                )
                            }
                        )
                    }
                }
            }
        )
    }

    companion object {
        const val FIRST_NAME = "John"
        const val LAST_NAME = "Smith"
        const val MIDDLE_NAME = "Great"
        const val PHONE = "+79112223344"
        const val EMAIL = "john@gmail.com"
    }
}
