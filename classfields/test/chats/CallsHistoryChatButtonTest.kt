package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.CallHistoryActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddCallsHistoryEntryStatement
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.CallHistoryScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.domain.model.common.Author
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.ChatInfo
import com.yandex.mobile.realty.domain.model.offer.House
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Sell
import com.yandex.mobile.realty.domain.model.site.SitePreview
import com.yandex.mobile.realty.domain.model.site.SitePreviewImpl
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 15/03/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CallsHistoryChatButtonTest : ChatButtonTest() {

    private val offer = createOfferPreview(SNIPPET_OFFER_ID, ChatInfo.Owner("4046750527"))
    private val siteOffer = createOfferPreview(SNIPPET_SITE_OFFER_ID, ChatInfo.Developer("46511"))
    private val site = createSitePreview()

    private val activityTestRule = CallHistoryActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        DatabaseRule(createAddCallsHistoryEntryStatement(offer)),
        DatabaseRule(createAddCallsHistoryEntryStatement(siteOffer)),
        DatabaseRule(createAddCallsHistoryEntryStatement(site)),
        authorizationRule
    )

    @Test
    fun shouldStartChatWhenOfferChatButtonPressed() {
        configureWebServer {
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<CallHistoryScreen> {
            offerSnippet(SNIPPET_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenSiteOfferChatButtonPressed() {
        configureWebServer {
            registerSnippetSiteOfferChat()
            registerSnippetSiteOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<CallHistoryScreen> {
            offerSnippet(SNIPPET_SITE_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenSiteChatButtonPressed() {
        configureWebServer {
            registerSnippetSiteChat()
            registerSnippetSiteChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<CallHistoryScreen> {
            siteSnippet(SNIPPET_SITE_ID)
                .waitUntil { listView.contains(this) }
                .chatButton
                .click()
        }

        checkSnippetSiteChatViewState()
    }

    private companion object {

        private fun createOfferPreview(id: String, chatInfo: ChatInfo): OfferPreview {
            val price = Price(
                value = 3_608_556L,
                unit = Price.Unit.PER_OFFER,
                currency = Price.Currency.RUB,
                period = Price.Period.WHOLE_LIFE
            )
            val priceInfo = PriceInfo(price, null, price, Trend.UNCHANGED)
            val deal = Sell(
                priceInfo = priceInfo,
                primarySale = null
            )
            val property = House(
                null,
                null,
                null,
                null,
                null
            )
            return OfferPreviewImpl(
                id = id,
                deal = deal,
                property = property,
                partnerId = null,
                author = Author(
                    Author.Category.OWNER,
                    "продавец",
                    null,
                    null,
                    null,
                    null
                ),
                createdAt = null,
                updateDate = null,
                images = null,
                locationInfo = null,
                active = true,
                vas = null,
                uid = "4046750527",
                excerptFreeReportAccessible = null,
                onlineShow = null,
                videoId = null,
                isFullTrustedOwner = null,
                shareUrl = null,
                isPaid = true,
                isExtended = false,
                note = null,
                virtualTour = null,
                chatInfo = chatInfo,
            )
        }

        private fun createSitePreview(): SitePreview {
            return SitePreviewImpl(
                id = SNIPPET_SITE_ID,
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
                hasDeveloperChat = true,
                briefRoomsStats = null,
            )
        }
    }
}
