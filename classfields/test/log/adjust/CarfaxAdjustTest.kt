package ru.auto.ara.test.log.adjust

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.carfax.getBoughtReportsListEmpty
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.getRawReport
import ru.auto.ara.core.dispatchers.carfax.report.getRawVinReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForSearch
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.paymentSuccess
import ru.auto.ara.core.dispatchers.payment.postProcessPaymentError
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.freeReportPromoXml
import ru.auto.ara.core.mapping.ssr.getContentCell
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.carfax.performCarfaxSearch
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.payment.checkPayment
import ru.auto.ara.core.robot.payment.performPayment
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.AppNotRunningRule
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.TrustPaymentControllerFactoryRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.TestRandomProviderRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity
import ru.auto.ara.util.statistics.MockedAdjustAnalyst
import ru.auto.feature.carfax.api.CarfaxAnalyst
import ru.auto.feature.carfax.ui.fragment.ReCarfaxReportFragment
import ru.auto.feature.carfax.ui.presenter.CarfaxReport

@RunWith(Parameterized::class)
class CarfaxAdjustTest(private val paymentSystem: PaymentSystem) {
    private val deeplinkActivityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val fullReportActivityRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()

    private val webServerRule = WebServerRule {
        userSetup()
        getOffer(category = CARS_CATEGORY, offerId = OFFER_ID)
        getRawVinReport(vinOrLicense = VIN, isBought = false)
        getParsedDeeplink(expectedResponse = DEEPLINK_PARCER_OFFER_RESPONSE)
        getBoughtReportsListEmpty()
        getRawReport(
            requestOfferId = OFFER_ID,
            fileOfferId = RAW_CARFAX_OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
        )
        makeXmlForSearch(vinOrLicense = VIN, isBought = false)
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT, fileName = RAW_CARFAX_OFFER_ID)
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            fileName = "1093024666-aa502a2d",
            mapper = { copyWithNewCarfaxResponse(listOf(getContentCell(), freeReportPromoXml)) }
        )
        paymentSuccess(paymentSystem)
        delegateDispatcher(BreadcrumbsSuggestDispatcher.carMarks())
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        deeplinkActivityRule,
        fullReportActivityRule,
        SetPreferencesRule(),
        SetupAuthRule(),
        DisableAdsRule(),
        AppNotRunningRule(),
        TestRandomProviderRule { 0 },
        TrustPaymentControllerFactoryRule(),
    )

    @Test
    fun shouldLogAdjustAfterPurchaseReportOnSearchPreview() {

        openCarfaxSearch()
        MockedAdjustAnalyst.waitLastEventAndHistorySize(SHOW_REPORT_BUTTON_TOKEN, 1)
        performCarfaxSearch { clickOnPackageButton("197 ₽") }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(SHOW_REPORT_BUTTON_TOKEN, 1)
        getBoughtReportMocks()
        performPayment { clickOnPayButtonUntilHidden() }
        performCarfaxReport { waitCarfaxReport() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_1_REPORT_TOKEN, 2)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                SHOW_REPORT_BUTTON_TOKEN,
                BUY_1_REPORT_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterPurchasePackageOnSearchPreview() {
        openCarfaxSearch()
        MockedAdjustAnalyst.waitLastEventAndHistorySize(SHOW_REPORT_BUTTON_TOKEN, 1)
        performCarfaxSearch { clickOnPackageButton("990 ₽") }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_10_REPORTS_BUTTON_TOKEN, 2)
        getBoughtReportMocks()
        performPayment { clickOnPayButtonUntilHidden() }
        performCarfaxReport { waitCarfaxReport() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_10_REPORTS_TOKEN, 3)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                SHOW_REPORT_BUTTON_TOKEN,
                CLICK_10_REPORTS_BUTTON_TOKEN,
                BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterClickSingleReportAndThenPurchasePackageOnSearchPreview() {
        openCarfaxSearch()
        MockedAdjustAnalyst.waitLastEventAndHistorySize(SHOW_REPORT_BUTTON_TOKEN, 1)
        performCarfaxSearch {
            scrollToBottom()
            clickBuyReport()
        }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 2)
        performPayment { selectPackageOf10Reports() }
        getBoughtReportMocks()
        performPayment { clickOnPayButtonUntilHidden() }
        performCarfaxReport { waitCarfaxReport() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_10_REPORTS_TOKEN, 3)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                SHOW_REPORT_BUTTON_TOKEN,
                CLICK_1_REPORT_BUTTON_TOKEN,
                BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterPurchaseReportOnCard() {
        openCard()
        performOfferCard { scrollToCarfaxBuyButton() }
        performOfferCard { clickBuyReport() }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 1)
        getBoughtReportMocks()
        performPayment { clickOnPayButtonUntilHidden() }
        performCarfaxReport { waitCarfaxReport() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_10_REPORTS_TOKEN, 2)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                CLICK_1_REPORT_BUTTON_TOKEN,
                BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterPurchaseReportErrorOnCard() {
        webServerRule.routing {
            oneOff { postProcessPaymentError(paymentSystem) }
        }
        openCard()
        performOfferCard { scrollToCarfaxBuyButton() }
        performOfferCard { clickBuyReport() }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 1)
        performPayment { clickOnPayButton() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(ERROR_BUY_10_REPORTS_TOKEN, 2)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                CLICK_1_REPORT_BUTTON_TOKEN,
                ERROR_BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterPurchaseReportCancelOnCard() {
        openCard()
        performOfferCard { scrollToCarfaxBuyButton() }
        performOfferCard { clickBuyReport() }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 1)
        checkPayment { isPayButtonCompletelyDisplayed() }
        performPayment { closePayment() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CANCEL_BUY_10_REPORTS_TOKEN, 2)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                CLICK_1_REPORT_BUTTON_TOKEN,
                CANCEL_BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterClickSingleReportAndThenPurchasePackageOnCard() {
        openCard()
        performOfferCard { scrollToCarfaxBuyButton() }
        performOfferCard { clickBuyReport() }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 1)
        performPayment { selectPackageOf10Reports() }
        getBoughtReportMocks()
        performPayment { clickOnPayButtonUntilHidden() }
        performCarfaxReport { waitCarfaxReport() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_10_REPORTS_TOKEN, 2)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                CLICK_1_REPORT_BUTTON_TOKEN,
                BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterPurchaseReportOnFullReport() {
        openFullReport()
        MockedAdjustAnalyst.waitLastEventAndHistorySize(SHOW_REPORT_BUTTON_TOKEN, 1)
        performCarfaxReport { clickBuyReport() }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 2)
        performPayment { clickOnPayButtonUntilHidden() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_10_REPORTS_TOKEN, 3)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                SHOW_REPORT_BUTTON_TOKEN,
                CLICK_1_REPORT_BUTTON_TOKEN,
                BUY_10_REPORTS_TOKEN
            )
        )
    }

    @Test
    fun shouldLogAdjustAfterClickSingleReportAndThenPurchasePackageOnFullReport() {
        openFullReport()
        MockedAdjustAnalyst.waitLastEventAndHistorySize(SHOW_REPORT_BUTTON_TOKEN, 1)
        performCarfaxReport { clickBuyReport() }
        performPayment { waitPaymentButtonDisplayed() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(CLICK_1_REPORT_BUTTON_TOKEN, 2)
        performPayment { selectPackageOf10Reports() }
        getBoughtReportMocks()
        performPayment { clickOnPayButtonUntilHidden() }
        performCarfaxReport { waitCarfaxReport() }
        MockedAdjustAnalyst.waitLastEventAndHistorySize(BUY_10_REPORTS_TOKEN, 3)

        MockedAdjustAnalyst.waitEventsExactlyTheSame(
            listOf(
                SHOW_REPORT_BUTTON_TOKEN,
                CLICK_1_REPORT_BUTTON_TOKEN,
                BUY_10_REPORTS_TOKEN
            )
        )
    }

    private fun openCarfaxSearch() {
        deeplinkActivityRule.launchDeepLinkActivity("https://auto.ru/history/$VIN")
        performCarfaxSearch { waitCarfaxSearchToLoad() }
    }

    private fun openCard() {
        deeplinkActivityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$OFFER_ID")
        checkOfferCard { isOfferCard() }
    }

    private fun openFullReport() {
        fullReportActivityRule.launchFragment<ReCarfaxReportFragment>(
            ReCarfaxReportFragment.screen(
                CarfaxReport.Args(
                    source = CarfaxReport.Source.Offer(
                        offerId = OFFER_ID,
                        metricaSource = CarfaxAnalyst.BuySource.SOURCE_CARD_FREE_REPORT
                    )
                )
            ).args
        )
        performCarfaxReport { waitCarfaxReport() }
    }

    private fun getBoughtReportMocks() {
        webServerRule.routing {
            makeXmlForReport(
                vinOrLicense = VIN,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getContentCell(), freeReportPromoXml)) }
            )
            getRawVinReport(vinOrLicense = VIN, isBought = true)
            getRawReport(
                requestOfferId = OFFER_ID,
                fileOfferId = RAW_CARFAX_OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT
            )
            makeXmlForOffer(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                fileName = RAW_CARFAX_OFFER_ID
            )
            makeXmlForReportByOfferId(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getContentCell(), freeReportPromoXml)) }
            )
        }
    }

    companion object {
        private const val VIN = "Z8T4DNFUCDM014995"
        private const val OFFER_ID = "1087439802-b6940925"
        private const val DEEPLINK_PARCER_OFFER_RESPONSE = "cars_used_sale_1087439802-b6940925"
        private const val RAW_CARFAX_OFFER_ID = "1093024666-aa502a2a"
        private const val CARS_CATEGORY = "cars"

        private const val SHOW_REPORT_BUTTON_TOKEN = "8zhxzn"

        private const val CLICK_1_REPORT_BUTTON_TOKEN = "2f2maw"
        private const val CLICK_10_REPORTS_BUTTON_TOKEN = "2bhrow"

        private const val BUY_1_REPORT_TOKEN = "4qdgsb"
        private const val BUY_10_REPORTS_TOKEN = "jm2ct5"

        private const val CANCEL_BUY_1_REPORT_TOKEN = "v6pefq"
        private const val CANCEL_BUY_10_REPORTS_TOKEN = "rhrxm9"

        //        private const val ERROR_BUY_1_REPORT_TOKEN = "x7jg1h"
        private const val ERROR_BUY_10_REPORTS_TOKEN = "gugp1v"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<PaymentSystem> = PaymentSystem.values().toList()
    }
}
