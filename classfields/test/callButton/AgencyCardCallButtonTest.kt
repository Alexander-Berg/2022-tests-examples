package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.AgencyCardActivityTestRule
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnAgencyCardScreen
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.Screen
import com.yandex.mobile.realty.domain.model.ScreenReferrer
import com.yandex.mobile.realty.domain.model.agency.AgencyContext
import com.yandex.mobile.realty.domain.model.agency.AgencyProfilePreview
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.ui.model.AgencyCardParams
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.*

/**
 * @author andrey-bgm on 12/11/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AgencyCardCallButtonTest : CallButtonTest() {

    private val activityTestRule = AgencyCardActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value),
        activityTestRule
    )

    @Test
    fun startCallWhenFloatingCallButtonPressed() {
        configureWebServer {
            registerAgencyCard()
            registerAgencyPhone(UID)
        }

        launchAgencyActivity()
        registerCallIntent()

        performOnAgencyCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isCallStarted() }
        }
    }

    @Test
    fun startCallWhenContentCallButtonPressed() {
        configureWebServer {
            registerAgencyCard()
            registerAgencyPhone(UID)
            registerOffers()
        }

        launchAgencyActivity()
        registerCallIntent()

        performOnAgencyCardScreen {
            waitUntil { containsTotalOffersSubtitle() }
            collapseAppBar()

            scrollToPosition(lookup.matchesCallButton())
            scrollByFloatingButtonHeight()
            tapOn(lookup.matchesCallButton())

            waitUntil { isCallStarted() }
        }
    }

    @Test
    fun showCallErrorWhenEmptyResponseReceived() {
        configureWebServer {
            registerAgencyCard()
            registerEmptyAgencyPhones(UID)
        }

        launchAgencyActivity()

        performOnAgencyCardScreen {
            waitUntil { isFloatingCallButtonShown() }

            tapOn(lookup.matchesFloatingCallButton())

            waitUntil { isToastShown("Не\u00a0удалось позвонить. Попробуйте позже.") }
        }
    }

    @Test
    fun startCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "AGENCY_CARD_LISTING",
            currentScreen = "AGENCY_CARD"
        )
        dispatcher.registerAgencyCard()
        dispatcher.registerOffers()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на карточке агентства"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        launchAgencyActivity()
        registerCallIntent()

        performOnAgencyCardScreen {
            waitUntil { containsTotalOffersSubtitle() }
            collapseAppBar()

            waitUntil { containsOfferSnippet(OFFER_ID) }
            scrollByFloatingButtonHeight()

            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    private fun launchAgencyActivity() {
        val agencyContext = AgencyContext.Sell(
            regionId = 587_795,
            agencyPreview = AgencyProfilePreview(
                uid = UID,
                creationDate = Calendar.getInstance().run {
                    set(2020, 5, 22)
                    time
                },
                userType = AgencyProfilePreview.UserType.AGENCY,
                name = "Этажи",
                photo = "file:///sdcard/realty_images/test_image.jpeg"
            ),
            property = Filter.Property.APARTMENT
        )

        val params = AgencyCardParams(
            agencyContext = agencyContext,
            screenReferrer = ScreenReferrer.valueOf(Screen.OFFER_DETAILS)
        )

        activityTestRule.launchActivity(AgencyCardActivityTestRule.createIntent(params))
    }

    private fun DispatcherRegistry.registerAgencyCard() {
        register(
            request {
                path("2.0/agencies/active/user/uid:$UID")
            },
            response {
                assetBody("agencyTest/agency.json")
            }
        )
        register(
            request {
                path("1.0/dynamicBoundingBox")
            },
            response {
                assetBody("agencyTest/agencyAllOffersBoundingBox.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
            },
            response {
                assetBody("callButtonTest/offerWithSiteSearch.json")
            }
        )
    }

    private companion object {

        const val UID = "1"
        const val OFFER_ID = "0"
    }
}
