package ru.auto.ara.test.offer.tinkoff

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getTinkoffClaims
import ru.auto.ara.core.dispatchers.preliminary.getPreliminary
import ru.auto.ara.core.dispatchers.tinkoff.postPreliminary
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.robot.tinkoff.checkLoan
import ru.auto.ara.core.robot.tinkoff.checkSwapCar
import ru.auto.ara.core.robot.tinkoff.performLoan
import ru.auto.ara.core.robot.tinkoff.performSwapCar
import ru.auto.ara.core.routing.watch
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
class TinkoffSendAnotherCarTest {
    private val OFFER_ID = "1082957054-8d55bf9a"

    private val webServerRule = WebServerRule {
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
        )
        getOffer(OFFER_ID)
        getOffer("1084782777-a4467715")
        userSetup()
        getPreliminary("tinkoff")
        postPreliminary("tinkoff").watch { checkRequestWasCalled() }
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
    }

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        SetupAuthRule(),
        activityTestRule,
        SetPreferencesRule(),
        SetupTimeRule("28.02.2019", "08:00"),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Before
    fun openCardAndScrollToLoan() {
        webServerRule.routing { getTinkoffClaims("second_agreement_for_swap") }
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$OFFER_ID")

        performOfferCardVin { clickShowFreeReport() }
        checkVinReport { isVinReport() }
        waitSomething(300, TimeUnit.MILLISECONDS)
        performVinReport { close() }
        checkOfferCard { isOfferCard() }
        webServerRule.routing { getTinkoffClaims("auto_verification") }
        performLoan { scrollAndClickSendThisCarButton() }
    }

    @Test
    fun shouldSendCarForSwap() {
        checkSwapCar {
            isTitleDisplayed()
            isCloseIconDisplayed()
            isDescriptionDisplayed()
            isFirstOfferPhotoDisplayed()
            isSecondOfferPhotoDisplayed()
            isButtonAffirmativeDisplayed()
            isButtonNegativeDisplayed()
        }
        performSwapCar { clickAffirmativeButton() }
        checkLoan { isThisCarWaitApprovalLoanDisplayed() }
    }

}
