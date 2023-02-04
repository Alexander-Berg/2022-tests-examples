package ru.auto.ara.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.carfax.report.NoCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.offer_card.TinkoffDispatcher
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.cheapOffer
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.tinkoff.performLoan
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchOfferDetailsActivity
import ru.auto.ara.ui.activity.OfferDetailsActivity
import ru.auto.data.model.data.offer.CAR
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled

@RunWith(AndroidJUnit4::class)
class OfferLoanPriceTest {

    private val carfaxDispatcherHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            TinkoffDispatcher("empty"),
            carfaxDispatcherHolder
        )
        getOffer(category = CAR, offerId = OFFER_ID) { copy(offer = offer?.cheapOffer()) }
        getOffer(category = CAR, offerId = PRICE_DIFF_OFFER_ID) { copy(offer = offer?.cheapOffer()) }
        getOffer(category = CAR, offerId = DISABLED_CREDIT_OFFER_ID)
    }
    private val activityTestRule = lazyActivityScenarioRule<OfferDetailsActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule,
        DisableAdsRule(),
        SetPreferencesRule(),
        SetupAuthRule(),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Test
    fun shouldShowLoanPrice() {
        carfaxDispatcherHolder.innerDispatcher = NoCarfaxReportDispatcher()
        testPriceVisible(
            offerId = OFFER_ID,
            loanPrice = getResourceString(R.string.person_profile_loan_monthly_payment_from, "15 400")
        )
    }

    // this offer has complectation
    @Test
    fun shouldShowLoanPriceWithDiffPrice() {
        carfaxDispatcherHolder.innerDispatcher = NoCarfaxReportDispatcher()
        testPriceVisible(
            offerId = PRICE_DIFF_OFFER_ID,
            loanPrice = getResourceString(R.string.person_profile_loan_monthly_payment_from, "15 400"),
            oldPrice = "3 150 000 \u20BD"
        )
    }

    @Test
    fun shouldScrollToLoanPriceWithReport() {
        val carfaxRequestWatcher = RequestWatcher()
        carfaxDispatcherHolder.innerDispatcher = RawCarfaxOfferDispatcher(
            OFFER_ID,
            REPORT_FILE_OFFER_ID,
            RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            carfaxRequestWatcher
        )
        webServerRule.routing {
            makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
                .watch { checkRequestWasCalled() }
        }
        testPriceVisible(
            offerId = OFFER_ID,
            loanPrice = getResourceString(R.string.person_profile_loan_monthly_payment_from, "15 400"),
        )
    }

    @Test
    fun shouldNotShowLoanPriceInCaseOfLoanDisabled() {
        testPriceInvisible(DISABLED_CREDIT_OFFER_ID)
    }

    private fun testPriceVisible(
        offerId: String,
        loanPrice: String,
        oldPrice: String? = null,
    ) {
        activityTestRule.launchOfferDetailsActivity(CAR, offerId)
        performOfferCard {
            waitCardOpened()
        }.checkResult {
            if (oldPrice != null) {
                isOfferPriceWithPriceDownIconVisible()
            }
            isLoanPriceVisible(loanPrice)
        }

        performOfferCard { clickOnLoanPrice() }

        performLoan().checkResult {
            isHeadLoanBlockDisplayed()
            isHeadLoanBlockAtFirstVisiblePosition()
        }
    }

    private fun testPriceInvisible(offerId: String) {
        activityTestRule.launchOfferDetailsActivity(CAR, offerId)
        performOfferCard {
            waitCardOpened()
        }.checkResult {
            isLoanPriceGone()
        }
    }

    companion object {
        private const val OFFER_ID = "1087439802-b6940925"
        private const val PRICE_DIFF_OFFER_ID = "1080290554-5349dabf"
        private const val DISABLED_CREDIT_OFFER_ID = "1095669442-b3989724"
        private const val REPORT_FILE_OFFER_ID = "1093024666-aa502a2a"
    }
}
