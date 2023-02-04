package com.yandex.mobile.realty.test.publicationForm

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.PublicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnAddressRootScreen
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnAddressSuggestScreen
import com.yandex.mobile.realty.core.robot.performOnPublicationFormScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.UserOfferDraftRule
import com.yandex.mobile.realty.core.rule.baseChainOf
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
 * @author matek3022 on 2020-07-06.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class AddressPickerTest : BasePublishFormTest() {

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
    fun shouldAutoDetectAuroraAddressPressConfirmAndReenter() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetAuroraAddressApartment()
            registerGetMoscowAddress()
        }
        prepareAddressPickerScreen()

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
                tapOn(lookup.matchesConfirmAddressButton())
            }
        }

        performOnPublicationFormScreen {
            containsAddressField(FULL_AURORA_ADDRESS)
            tapOn(lookup.matchesAddressField())
        }

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
            }
        }
    }

    @Test
    fun shouldChangeAddressFromSuggest() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetMoscowAddress()
            registerGetGeoPointSuggestMoscow()
        }
        prepareAddressPickerScreen()

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
                tapOn(lookup.matchesAddressView())
            }
            performOnAddressSuggestScreen {
                isAddressSearchTextEquals(AURORA_ADDRESS)
                tapOn(lookup.matchesClearSearchTextButton())
                typeSearchText("Moscow")
                waitUntil { containsSuggest(MOSCOW_ADDRESS) }
                tapOn(lookup.matchesGeoPointSuggest(MOSCOW_ADDRESS))
            }
        }
        performOnPublicationFormScreen {
            containsAddressField(FULL_MOSCOW_ADDRESS)
        }
    }

    @Test
    fun shouldShownAllFieldsAndSelectAddressAndPressBack() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetMoscowAddress()
        }
        prepareAddressPickerScreen()

        performOnAddressRootScreen {
            isTitleShown()
            isSubtitleShown()
            isBackButtonShown()
            isMapShown()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
            }

            tapOn(lookup.matchesBackButton())
        }

        performOnPublicationFormScreen {
            scrollToPosition(lookup.matchesAddressField())
            containsAddressField()
        }
    }

    @Test
    fun shouldOpenSuggestAndPressSelectOnMapButton() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetAuroraAddressApartment()
            registerGetMoscowAddress()
        }
        prepareAddressPickerScreen()

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
                tapOn(lookup.matchesAddressView())
            }

            performOnAddressSuggestScreen {
                waitUntilKeyboardAppear()
                isSelectOnMapButtonShown()
                tapOn(lookup.matchesSelectOnMapButton())
            }

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
            }
        }
    }

    @Test
    fun shouldShowUserLocationWhenTapLocationButton() {
        mockLocationRule.setMockLocation(MOSCOW_LATITUDE, MOSCOW_LONGITUDE)
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerGetNearAuroraAddressApartment()
            registerGetMoscowAddress()
        }
        prepareAddressPickerScreen()

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(MOSCOW_ADDRESS)

                mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)

                tapOn(lookup.matchesLocationButton())
                waitUntil { isAddressEquals(AURORA_ADDRESS) }
            }
        }
    }

    @Test
    fun shouldOverrideGeoPointWithLot() {
        mockLocationRule.setMockLocation(NEAR_AURORA_LATITUDE, NEAR_AURORA_LONGITUDE)
        configureWebServer {
            registerUserProfile()
            registerUserProfile()
            registerGetNearAuroraAddressLot()
            registerGetNearAuroraAddressLot()
        }
        prepareAddressPickerScreenLot()

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
                tapOn(lookup.matchesConfirmAddressButton())
            }
        }

        performOnPublicationFormScreen {
            containsAddressField(FULL_AURORA_ADDRESS)
            tapOn(lookup.matchesAddressField())
        }

        performOnAddressRootScreen {
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(AURORA_ADDRESS)
            }
        }
    }

    private fun prepareAddressPickerScreen() {
        draftRule.prepareSellApartment()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellApartmentExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellApartmentCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesAddressField())
            tapOn(lookup.matchesAddressField())
        }
    }

    private fun prepareAddressPickerScreenLot() {
        draftRule.prepareSellLot()
        activityTestRule.launchActivity()
        performOnPublicationFormScreen {
            waitUntil { hasSellLotExpandedToolbarTitle() }
            collapseAppBar()
            waitUntil { hasSellLotCollapsedToolbarTitle() }
            scrollToPosition(lookup.matchesAddressField())
            tapOn(lookup.matchesAddressField())
        }
    }

    private fun DispatcherRegistry.registerGetMoscowAddress() {
        register(
            request {
                path("1.0/addressGeocoder.json")
                queryParam("latitude", MOSCOW_LATITUDE.toString())
                queryParam("longitude", MOSCOW_LONGITUDE.toString())
                queryParam("category", "APARTMENT")
            },
            response {
                assetBody("geocoderAddressMoscow.json")
            }
        )
    }

    private fun DispatcherRegistry.registerGetGeoPointSuggestMoscow() {
        register(
            request {
                path("1.0/geosuggest.json")
                queryParam("category", "APARTMENT")
                queryParam("type", "SELL")
                queryParam("target", "FLAT_AND_ROOM")
                queryParam("text", "Moscow")
            },
            response {
                assetBody("geosuggestMoscow.json")
            }
        )
    }

    companion object {
        const val MOSCOW_LATITUDE = 55.75322
        const val MOSCOW_LONGITUDE = 37.62251
        const val MOSCOW_ADDRESS = "Красная площадь"
        const val FULL_MOSCOW_ADDRESS = "Россия, Москва, Красная площадь"
    }
}
