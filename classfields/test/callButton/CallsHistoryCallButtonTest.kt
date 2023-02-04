package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.CallHistoryActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnCallHistoryScreen
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddCallsHistoryEntryStatement
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.domain.model.site.SitePreview
import com.yandex.mobile.realty.domain.model.site.SitePreviewImpl
import com.yandex.mobile.realty.domain.model.village.VillagePreview
import com.yandex.mobile.realty.domain.model.village.VillagePreviewImpl
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 22.04.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CallsHistoryCallButtonTest : CallButtonTest() {

    private val offer = createOfferPreview()
    private val site = createSitePreview()
    private val village = createVillagePreview()

    private val activityTestRule = CallHistoryActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        MetricaEventsRule(),
        activityTestRule,
        DatabaseRule(
            createAddCallsHistoryEntryStatement(offer),
            createAddCallsHistoryEntryStatement(site),
            createAddCallsHistoryEntryStatement(village)
        ),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "CALL_HISTORY_LISTING",
            currentScreen = "CALL_HISTORY"
        )
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на экране истории звонков"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnCallHistoryScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldStartCallWhenSiteCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "CALL_HISTORY_LISTING",
            currentScreen = "CALL_HISTORY"
        )
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = siteSnippet("на экране истории звонков"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell", "ZhkNewbuilding_Sell_Paid")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnCallHistoryScreen {
            waitUntil { containsSiteSnippet(SITE_ID) }
            performOnSiteSnippet(SITE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldStartCallWhenVillageCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "CALL_HISTORY_LISTING",
            currentScreen = "CALL_HISTORY"
        )
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = villageSnippet("на экране истории звонков"),
            categories = jsonArrayOf("Village_Sell", "Village_Sell_Paid", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnCallHistoryScreen {
            waitUntil { containsVillageSnippet(VILLAGE_ID) }
            performOnVillageSnippet(VILLAGE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    companion object {
        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"

        private fun createOfferPreview(): OfferPreview {
            val price = Price(
                value = 10_000L,
                unit = Price.Unit.PER_OFFER,
                currency = Price.Currency.RUB,
                period = Price.Period.WHOLE_LIFE
            )
            val priceInfo = PriceInfo(price, null, price, Trend.UNCHANGED)
            val deal = Sell(
                priceInfo = priceInfo,
                primarySale = null
            )
            val property = Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            )
            return OfferPreviewImpl(
                id = OFFER_ID,
                deal = deal,
                property = property,
                partnerId = null,
                author = null,
                createdAt = null,
                updateDate = null,
                images = null,
                locationInfo = null,
                active = true,
                vas = null,
                uid = null,
                excerptFreeReportAccessible = null,
                onlineShow = null,
                videoId = null,
                isFullTrustedOwner = null,
                shareUrl = null,
                isPaid = true,
                isExtended = false,
                note = null,
                virtualTour = null,
                chatInfo = null,
            )
        }

        private fun createSitePreview(): SitePreview {
            return SitePreviewImpl(
                id = SITE_ID,
                name = "Название ЖК",
                fullName = "Полное название ЖК",
                locativeFullName = null,
                developers = null,
                buildingClass = null,
                type = null,
                images = null,
                price = null,
                pricePerMeter = null,
                totalOffers = null,
                locationInfo = null,
                commissioningStatus = null,
                deliveryDates = null,
                specialProposalLabels = null,
                flatStatus = null,
                salesClosed = null,
                housingType = null,
                shareUrl = null,
                isPaid = true,
                isExtended = false,
                hasDeveloperChat = false,
                briefRoomsStats = null,
            )
        }

        private fun createVillagePreview(): VillagePreview {
            return VillagePreviewImpl(
                VILLAGE_ID,
                "Название КП",
                "Полное название КП",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            )
        }
    }
}
