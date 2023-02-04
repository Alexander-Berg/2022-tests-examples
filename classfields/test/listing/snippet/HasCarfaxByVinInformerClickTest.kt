package ru.auto.ara.test.listing.snippet

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class HasCarfaxByVinInformerClickTest(fileName: String) {

    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher(fileName),
        GetOfferDispatcher.getOffer(CARS_CATEGORY, OFFER_ID),
        RawCarfaxOfferDispatcher(
            requestOfferId = OFFER_ID,
            fileOfferId = RAW_CARFAX_OFFER_OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
        ),
        ParseDeeplinkDispatcher.carsAll()
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule {
            delegateDispatchers(dispatchers)
            makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        },
        DisableAdsRule(),
        SetPreferencesRule()
    )

    @Test
    fun checkOnHasCarfaxByVinInformerSnippetClick() {
        lazyActivityScenarioRule<DeeplinkActivity>().apply {
            launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        }.requireScenario().use {
            performListingOffers {
                scrollToStickersOnExtendedOffer()
                clickHasCarfaxByVinInformer()
            }
            checkOfferCard { checkCarfaxBlockDisplayed() }
        }
    }

    companion object {
        private const val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
        private const val CARS_CATEGORY = "cars"
        private const val OFFER_ID = "1087439802-b6940925"
        private const val RAW_CARFAX_OFFER_OFFER_ID = "1093024666-aa502a2a"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = listOf(
            arrayOf("informers_extended_snippet_vin_ok_no_history"),
            arrayOf("informers_common_snippet_vin_ok_no_history")
        )
    }

}
