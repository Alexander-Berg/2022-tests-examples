package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.CallHistoryActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnCallHistoryScreen
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.domain.model.common.Price
import com.yandex.mobile.realty.domain.model.common.PriceInfo
import com.yandex.mobile.realty.domain.model.common.Trend
import com.yandex.mobile.realty.domain.model.offer.Apartment
import com.yandex.mobile.realty.domain.model.offer.OfferPreview
import com.yandex.mobile.realty.domain.model.offer.OfferPreviewImpl
import com.yandex.mobile.realty.domain.model.offer.Sell
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CallHistoryNotesTest : NotesTest() {

    private val offer = createOfferPreview()

    private val activityTestRule = CallHistoryActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        DatabaseRule(DatabaseRule.createAddCallsHistoryEntryStatement(offer))
    )

    @Test
    fun shouldAddNote() {
        configureWebServer {
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnCallHistoryScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesMenuButton())
            }
        }
        performOnOfferMenuDialog {
            isAddNoteButtonShown()
            tapOn(lookup.matchesAddNoteButton())
        }
        performOnNoteScreen {
            typeText(lookup.matchesInputView(), TEXT)
            tapOn(lookup.matchesSubmitButton())
        }
        performOnCallHistoryScreen {
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                isNoteShown(TEXT)
            }
        }
    }

    companion object {

        private const val OFFER_ID = "0"
        private const val TEXT = "some text"

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
    }
}
