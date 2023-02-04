package ru.auto.ara.test.listing.snippet

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.ListingOffersRobotChecker
import ru.auto.ara.core.robot.searchfeed.checkListingOffers
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.HAS_CARFAX_BY_VIN_TEST_DATA
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class HasCarfaxByVinInformerVisibilityTest(private val testParameter: TestParameter) {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"
    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher(testParameter.expectedSearchResponse),
        ParseDeeplinkDispatcher.carsAll()
    )

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun setupDispatchers() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToStickersOnExtendedOffer() }
    }

    @Test
    fun checkHasCarfaxByVinInformerVisibility() {
        checkListingOffers { testParameter.check(this) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = PARAMS.map { arrayOf(it) }

        private val PARAMS = HAS_CARFAX_BY_VIN_TEST_DATA.map { (snippetType, vinStatus, historyStatus) ->
            TestParameter(
                vinStatus = vinStatus,
                snippetType = snippetType,
                historyStatus = historyStatus,
                expectedSearchResponse = "informers_${snippetType}_snippet_vin_${vinStatus}_${historyStatus}_history",
                check = {
                    when (historyStatus) {
                        "no" -> when (vinStatus) {
                            "ok",
                            "unknown",
                            "error" -> isHasCarfaxByVinInformerDisplayed()
                            "invalid",
                            "untrusted",
                            "not_matched_plate" -> isHasCarfaxByVinInformerNotDisplayed()
                        }
                        "has" -> when (vinStatus) {
                            // check only one bad case
                            "invalid" -> isHasCarfaxByVinInformerDisplayed()
                        }
                    }
                }
            )
        }
    }

    data class TestParameter(
        val vinStatus: String,
        val snippetType: String,
        val historyStatus: String,
        val expectedSearchResponse: String,
        val check: ListingOffersRobotChecker.() -> Unit
    ) {
        override fun toString() = "$vinStatus snippetType=$snippetType historyStatus=$historyStatus"
    }

}
