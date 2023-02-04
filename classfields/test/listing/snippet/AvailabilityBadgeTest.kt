package ru.auto.ara.test.listing.snippet

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.search_offers.*
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.network.scala.response.OfferListingResponse

@RunWith(AndroidJUnit4::class)
class AvailabilityBadgeTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"

    private val webServerRule = WebServerRule {
        getParsedDeeplink("cars_all")
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldSeeOnOrderInformer() {
        webServerRule.routing {
            getOfferCount(count = 1)
            postSearchOffers("listing_offers/extended_availability_on_order.json",
                mapper = { mapOnePage() }
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isTechParam1Displayed(ON_ORDER_TECH_PARAM)
            }
    }

    @Test
    fun shouldSeeInStockInformer() {
        webServerRule.routing {
            getOfferCount(count = 1)
            postSearchOffers("listing_offers/extended_availability_in_stock.json",
                mapper = { mapOnePage() }
            )
        }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)

        performListingOffers { scrollToStickersOnExtendedOffer() }
            .checkResult {
                isTechParam1Displayed(IN_STOCK_TECH_PARAM)
            }
    }

    private fun OfferListingResponse.mapOnePage(): OfferListingResponse =
        this.copy(
            pagination = this.pagination?.copy(
                total_offers_count = 1,
                total_page_count = 1
            )
        )

    companion object {
        private const val ON_ORDER_TECH_PARAM = "В пути, Комфорт Плюс"
        private const val IN_STOCK_TECH_PARAM = "В наличии, Комфорт Плюс"
    }
}
