package ru.auto.ara.test.offer.advantages

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.carfax.report.getNoCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.preliminary.getPreliminaryEmpty
import ru.auto.ara.core.dispatchers.stats.getStatsSummary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.mapWithLastYearPrice
import ru.auto.ara.core.robot.offercard.checkAdvantageStablePrice
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ALL_ADVANTAGES
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER
import ru.auto.ara.core.testdata.OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE
import ru.auto.ara.core.testdata.OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE_WITH_DATA_IN_OFFER
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class AdvantageStablePriceTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule {
        userSetup()
        getPreliminaryEmpty("tinkoff")
        getNoCarfaxReportDispatcher()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule(),
        SetupAuthRule()
    )

    @Test
    fun shouldShowAdvantageStablePriceWithDataInOffer() {
        webServerRule.routing {
            getOffer(OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER)
            getStatsSummary().watch {
                checkStatSummaryQueryParams(
                    rid = "213",
                    mark = "MERCEDES",
                    model = "CLS_KLASSE_AMG",
                    superGen = "21321173",
                    configurationId = "21321228",
                    techParamId = "21321328",
                    complectationId = "21321394"
                )
            }
        }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER)
        openAdvantageStablePrice(STABLE_PRICE_POSITION_WITH_DATA)
        checkAdvantageStablePriceWithDataInOffer()
    }

    @Test
    fun shouldShowAdvantageStablePriceWithoutDataInOffer() {
        webServerRule.routing {
            getOffer(OFFER_ID_WITH_ALL_ADVANTAGES)
            getStatsSummary().watch {
                checkStatSummaryQueryParams(
                    rid = "213",
                    mark = "MERCEDES",
                    model = "CLS_KLASSE_AMG",
                    superGen = "21321173",
                    configurationId = "21321228",
                    techParamId = "21321328",
                    complectationId = "21321394"
                )
            }
        }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + OFFER_ID_WITH_ALL_ADVANTAGES)
        openAdvantageStablePrice(STABLE_PRICE_POSITION_WITHOUT_DATA)
        checkAdvantageStablePriceWithoutDataInOffer()
    }

    @Test
    fun shouldShowAdvantageStablePriceSingleWithDataInOffer() {
        webServerRule.routing {
            getOffer(OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE_WITH_DATA_IN_OFFER)
            getStatsSummary().watch {
                checkStatSummaryQueryParams(
                    rid = "213",
                    mark = "LAND_ROVER",
                    model = "RANGE_ROVER",
                    superGen = "21128050",
                    configurationId = "21128091",
                    techParamId = "21130587",
                    complectationId = "21325192"
                )
            }
        }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE_WITH_DATA_IN_OFFER)
        openAdvantageStablePriceSingle()
        checkAdvantageStablePriceWithDataInOffer()
    }

    @Test
    fun shouldShowAdvantageStablePriceSingleWithoutDataInOffer() {
        webServerRule.routing {
            getOffer(OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE)
            getStatsSummary().watch {
                checkStatSummaryQueryParams(
                    rid = "100591",
                    mark = "VAZ",
                    model = "GRANTA",
                    superGen = "21377296",
                    configurationId = "21377430",
                    techParamId = "21377540"
                )
            }
        }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE)
        openAdvantageStablePriceSingle()
        checkAdvantageStablePriceWithoutDataInOffer()
    }

    @Test
    fun shouldNotShowAdvantageStablePriceWithoutDeprecation() {
        webServerRule.routing {
            getOffer(
                OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE,
                mapper = { mapWithLastYearPrice(price = -4) }
            )
            getStatsSummary("stats/summary_without_deprecation.json")
        }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + OFFER_ID_WITH_STABLE_PRICE_ADVANTAGE)
        openAdvantageStablePriceSingle()
        checkAdvantageStablePrice {
            isTitleDisplayed("-4% в год")
            isSubtitleDisplayed()
        }
    }

    private fun checkAdvantageStablePriceWithDataInOffer() {
        checkAdvantageStablePrice("-3% в год")
    }

    private fun checkAdvantageStablePriceWithoutDataInOffer() {
        checkAdvantageStablePrice("-5% в год")
    }

    private fun checkAdvantageStablePrice(title: String) {
        checkAdvantageStablePrice {
            isTitleDisplayed(title)
            isSubtitleDisplayed()
            isGraphDisplayed()
            isProgressNotDisplayed()
            isErrorImageNotDisplayed()
            isErrorTextNotDisplayed()
            isRetryNotDisplayed()
        }
    }

    private fun RequestWatcher.checkStatSummaryQueryParams(
        rid: String,
        mark: String,
        model: String,
        superGen: String,
        configurationId: String,
        techParamId: String,
        complectationId: String? = null,
    ) {
        checkQueryParameters(
            listOfNotNull(
                "rid" to rid,
                "mark" to mark,
                "model" to model,
                "super_gen" to superGen,
                "configuration_id" to configurationId,
                "tech_param_id" to techParamId,
                complectationId?.let { "complectation_id" to complectationId }
            )
        )
    }

    private fun openAdvantageStablePrice(pos: Int) {
        performOfferCard {
            scrollToAdvantages()
            scrollToAdvantage(pos)
            clickOnAdvantage(pos)
        }
    }

    private fun openAdvantageStablePriceSingle() {
        performOfferCard {
            collapseAppBar()
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
    }

    companion object {
        private const val STABLE_PRICE_POSITION_WITH_DATA = 9
        private const val STABLE_PRICE_POSITION_WITHOUT_DATA = 8
        private const val OFFER_CARD_PATH = "https://auto.ru/cars/used/sale/"
    }
}
