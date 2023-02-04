package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher.Companion.NOT_FOUND_REPORT_OFFER_ID
import ru.auto.ara.core.dispatchers.carfax.report.getRawReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

/**
 * @author themishkun on 2019-04-26.
 */
@RunWith(AndroidJUnit4::class)
class OfferCardAnchoredDeeplinkTest {

    private val webServerRule = WebServerRule {
        delegateDispatcher(GetOfferDispatcher.getOffer("cars", OFFER_ID))
    }

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        DisableAdsRule(),
        SetPreferencesRule(),
    )

    @Test
    fun shouldScrollToHistoryAutomaticallyWhenOpenFromAnchorDeeplink() {
        webServerRule.routing {
            getRawReport(
                requestOfferId = OFFER_ID,
                fileOfferId = RAW_CARFAX_OFFER_OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
            )
            makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        }
        openDeepLink()
        checkOfferCard { checkCarfaxBlockDisplayed() }
    }

    @Test
    fun shouldStayAtTopWhenOpenFromAnchorDeeplinkToOfferWithNoHistory() {
        webServerRule.routing {
            getRawReport(
                requestOfferId = OFFER_ID,
                fileOfferId = NOT_FOUND_REPORT_OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_FOUND
            )
        }
        openDeepLink()
        waitSomething(2, TimeUnit.SECONDS)
        checkOfferCard { isOfferTitleExpanded() }
    }

    private fun openDeepLink() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$OFFER_ID/history")
    }

    companion object {
        private const val RAW_CARFAX_OFFER_OFFER_ID = "1093024666-aa502a2a"
        private const val OFFER_ID = "1074918427-cdd53a67"
    }
}
