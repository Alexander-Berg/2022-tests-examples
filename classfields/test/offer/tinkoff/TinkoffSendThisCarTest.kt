package ru.auto.ara.test.offer.tinkoff

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.getRawReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.tinkoff.postPreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.freeReportPromoXml
import ru.auto.ara.core.mapping.ssr.getContentCell
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.robot.tinkoff.checkLoan
import ru.auto.ara.core.robot.tinkoff.performLoan
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TinkoffSendThisCarTest {
    private val OFFER_ID = "1082957054-8d55bf9a"
    private val FILE_OFFER_ID = "1093024666-aa502a2a"
    private val webServerRule = WebServerRule {
        getRawReport(
            requestOfferId = OFFER_ID,
            fileOfferId = FILE_OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
        )
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getContentCell(), freeReportPromoXml)) }
        )
        getOffer(OFFER_ID)
        userSetup()
        getPreliminary("tinkoff")
        postPreliminary("tinkoff")
    }

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        SetupTimeRule(date = "28.02.2019", localTime = "08:00", timeZoneId = "Europe/Moscow"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Before
    fun openCardAndScrollToLoan() {
        webServerRule.routing { getTinkoffClaims("wait_car") }
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")

        performOfferCard { collapseAppBar() }
        performOfferCardVin { clickShowFreeReport() }
        checkVinReport { isVinReport() }
        waitSomething(300, TimeUnit.MILLISECONDS)
        performVinReport { close() }
        checkOfferCard { isOfferCard() }
        webServerRule.routing { getTinkoffClaims("auto_verification") }
    }

    @Test
    fun shouldSeeCorrectLoanAfterRequestFromWaitCarStatus() {
        performLoan { scrollAndClickSendThisCarButton() }
        checkLoan { isThisCarWaitApprovalLoanDisplayed() }
    }
}
