package ru.auto.ara.test.carfax

import androidx.test.espresso.Espresso
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.BodyNode.Companion.checkEventContents
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.carfax.getBoughtReportsListNormalWithOffer
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher.DirType
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForSearch
import ru.auto.ara.core.dispatchers.frontlog.checkFrontlogCommonParams
import ru.auto.ara.core.dispatchers.frontlog.postFrontLog
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.paymentSuccess
import ru.auto.ara.core.dispatchers.payment.postInitPayment
import ru.auto.ara.core.dispatchers.user.postUserPhones
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.getEmptyUserOffers
import ru.auto.ara.core.mapping.carfax.setQuota
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.freeReportPromoXml
import ru.auto.ara.core.robot.carfax.checkCarfaxList
import ru.auto.ara.core.robot.carfax.checkCarfaxReport
import ru.auto.ara.core.robot.carfax.checkCarfaxSearchScreenshot
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.carfax.performCarfaxSearch
import ru.auto.ara.core.robot.full_gallery.performFullScreenGallery
import ru.auto.ara.core.robot.offercard.checkOfferCardVin
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.payment.checkPayment
import ru.auto.ara.core.robot.payment.performPayment
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.TrustPaymentControllerFactoryRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.TestRandomProviderRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getDeepLinkIntent
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.ara.screenshotTests.carfax.CarfaxOfferTemplateTest.Companion.VAS_CLICK_EVENT
import ru.auto.ara.screenshotTests.carfax.CarfaxOfferTemplateTest.Companion.VAS_PURCHASE_EVENT
import ru.auto.ara.screenshotTests.carfax.CarfaxOfferTemplateTest.Companion.VAS_SPEND_QUOTA_EVENT

@RunWith(Parameterized::class)
class ReportPaymentTest(private val paymentSystem: PaymentSystem) {

    private val rawCarfaxOfferDispatcherHolder: DispatcherHolder = DispatcherHolder()
    private val deeplinkActivityRule: ActivityTestRule<DeeplinkActivity> =
        ActivityTestRule(DeeplinkActivity::class.java, false, false)
    private val mainActivityRule = ActivityTestRule(MainActivity::class.java, false, false)

    private val dispatchers: List<DelegateDispatcher> = listOf(
        rawCarfaxOfferDispatcherHolder,
        GetOfferDispatcher.getOffer("cars", REQUEST_OFFER_ID),
    )

    private val webServerRule = WebServerRule {
        userSetup()
        paymentSuccess(paymentSystem)
        getEmptyUserOffers("cars")
        delegateDispatchers(dispatchers)
        postUserPhones()
        makeXmlForSearch(vinOrLicense = QUOTA_VIN, isBought = false)
        makeXmlForSearch(vinOrLicense = NO_QUOTA_VIN, isBought = false)
        makeXmlForReportByOfferId(
            offerId = REQUEST_OFFER_ID,
            dirType = DirType.NOT_BOUGHT,
            fileName = "1097084632-f8004e93",
            mapper = { copyWithNewCarfaxResponse(listOf(freeReportPromoXml)) },
        )
    }

    @JvmField
    @Rule
    var rules = baseRuleChain(
        webServerRule,
        deeplinkActivityRule,
        mainActivityRule,
        TestRandomProviderRule { 0 },
        SetupAuthRule(),
        SetPreferencesRule(),
        ImmediateImageLoaderRule(),
        TrustPaymentControllerFactoryRule(),
    )

    @Test
    fun shouldBuyReportThroughSpendQuotaFromOfferCard() {
        webServerRule.routing {
            postFrontLog().watch {
                checkFrontlogCommonParams(VAS_SPEND_QUOTA_EVENT)
                checkEventContents(VAS_SPEND_QUOTA_EVENT) {
                    get("product").assertValue("reports_quota")
                    get("contextPage").assertValue("PAGE_CARD")
                    get("contextBlock").assertValue("BLOCK_REPORT_PROMO")
                    get("offerId").assertValue("1083763087-cc26905f")
                    get("category").assertValue("CARS")
                }
            }
        }
        setFreeOfferReportWithQuotaToDispatcher()
        openToOfferCard()
        performOfferCardVin { scrollToVinReport() } // ensure that carfax block was shown as not bought before set paid dispatcher
        setPaidOfferReportWithQuotaToDispatcher()
        performOfferCardVin { clickPayReport("Смотреть полный отчёт") }
        performCarfaxReport { waitCarfaxReport() }
        checkPaidReportFromOfferCard()
    }

    @Test
    fun shouldBuyReportThroughSpendQuotaFromFreeReport() {
        setFreeOfferReportWithQuotaToDispatcher()
        openToOfferCard()
        performOfferCardVin { clickShowFreeReport() }
        performCarfaxReport {
            waitCarfaxReport()
            checkCarfaxReport { isDownloadPdfButtonVisible(false) }
            setPaidOfferReportWithQuotaToDispatcher()
            clickBuyReport()
        }
        checkPaidReportFromOfferCard()
    }

    @Test
    fun shouldBuyReportWithoutQuotaFromOfferCard() {
        webServerRule.routing {
            postFrontLog().watch {
                checkFrontlogCommonParams(VAS_CLICK_EVENT)
                checkEventContents(VAS_CLICK_EVENT) {
                    get("product").assertValue("reports")
                    get("contextPage").assertValue("PAGE_CARD")
                    get("contextBlock").assertValue("BLOCK_REPORT_PROMO")
                    get("offerId").assertValue("1083763087-cc26905f")
                    get("category").assertValue("CARS")
                }
                checkFrontlogCommonParams(VAS_PURCHASE_EVENT)
                checkEventContents(VAS_PURCHASE_EVENT) {
                    get("product").assertValue("reports_1")
                    get("contextPage").assertValue("PAGE_CARD")
                    get("contextBlock").assertValue("BLOCK_CURTAIN")
                    get("offerId").assertValue("1083763087-cc26905f")
                    get("category").assertValue("CARS")
                }
            }
        }
        setOfferReportWithoutQuotaToDispatcher(isBought = false, dirType = DirType.NOT_BOUGHT)
        openToOfferCard()
        performOfferCard { collapseAppBar() }
        performOfferCardVin { scrollToVinReport() } // ensure that carfax block was shown as not bought before set paid dispatcher
        setPaymentRouting("10", "api_android_card", paymentSystem)
        setOfferReportWithoutQuotaToDispatcher(isBought = true, dirType = DirType.BOUGHT)
        performOfferCardVin {
            clickPayReport("Купить отчёт от")
        }
        performPayment { clickOnPayButton() }
        performCarfaxReport { waitCarfaxReport() }
        checkPaidReportFromOfferCard()
    }

    @Test
    fun shouldBackToCardWithoutScroll() {
        setOfferReportWithoutQuotaToDispatcher(isBought = false, dirType = DirType.NOT_BOUGHT)
        openToOfferCard()
        performOfferCard { collapseAppBar() }
        performOfferCardVin { clickShowFreeReport() }
        performCarfaxReport {
            waitCarfaxReport()
        }
        pressBack()
        checkOfferCardVin { isFreeReportButtonDisplayed() }
    }

    @Test
    fun shouldBuyReportFromGallery() {
        setOfferReportWithoutQuotaToDispatcher(isBought = false, dirType = DirType.NOT_BOUGHT)
        openToOfferCard()
        performOfferCard { clickGalleryPhoto() }
        performFullScreenGallery {
            scrollGalleryToCarfaxReportPromo()
            clickShowReportInPromo()
        }

        setOfferReportWithoutQuotaToDispatcher(isBought = true, dirType = DirType.BOUGHT)
        performCarfaxReport {
            waitCarfaxReport()
            clickBuyReport()
        }

        performPayment { clickOnPayButton() }
        checkCarfaxReport { isDownloadPdfButtonVisible(true) }
        pressBack()
        checkOfferCardVin { isOpenFullReportButtonDisplayed() }
    }

    @Test
    fun shouldBuyReportWithoutQuotaFromFreeReport() {
        setOfferReportWithoutQuotaToDispatcher(isBought = false, dirType = DirType.NOT_BOUGHT)
        openToOfferCard()
        performOfferCardVin { clickShowFreeReport() }
        performCarfaxReport {
            waitCarfaxReport()
            checkCarfaxReport { isDownloadPdfButtonVisible(false) }
            setOfferReportWithoutQuotaToDispatcher(isBought = true, dirType = DirType.BOUGHT)
        }
        setPaymentRouting("1", "api_android_card_free_report", paymentSystem)
        performCarfaxReport { clickBuyReport() }
        checkPayment { isCollapsedPaymentScreenDisplayed() }
        performPayment {
            selectSingleReport()
            clickOnPayButton()
        }
        checkPaidReportFromOfferCard()
    }

    @Test
    fun shouldBuyReportThroughSpendQuotaFromSearch() {
        webServerRule.routing {
            makeXmlForSearch(
                vinOrLicense = QUOTA_VIN,
                isBought = false,
                mapper = { setQuota(6) }
            )
        }
        setReportWithQuotaToDispatcher(isBought = false, dirType = DirType.NOT_BOUGHT)
        openCarfaxSearch(vin = QUOTA_VIN)
        setReportWithQuotaToDispatcher(isBought = true, dirType = DirType.BOUGHT)
        checkCarfaxSearchScreenshot { isSearchPreviewTheSame(filePath = "carfax/search/not_bought_preview_with_quota.png") }
        performCarfaxSearch { clickOpenBuyQuotaButton() }
        performCarfaxReport { waitCarfaxReport() }
        checkCarfaxReport { isDownloadPdfButtonVisible(true) }
    }

    @Test
    fun shouldBuyReportWithoutQuotaFromSearch() {
        mainActivityRule.launchActivity(null)
        performMain {
            openMainTab(R.string.reports)
        }.checkResult {
            isMainTabSelected(R.string.reports)
        }

        buyReportFromSearch {
            performCarfaxSearch {
                scrollToBottom()
                clickBuyReport()
            }
        }
    }

    @Test
    fun shouldBuyReportPackageFromSearch() {
        mainActivityRule.launchActivity(null)
        performMain { openMainTab(R.string.reports) }.checkResult { isMainTabSelected(R.string.reports) }
        buyReportFromSearch {
            performCarfaxSearch {
                scrollToBottom()
                clickBuyReport()
            }
        }
    }

    private fun buyReportFromSearch(buyClickAction: () -> Unit) {
        setReportWithoutQuotaToDispatcher(isBought = false, dirType = DirType.NOT_BOUGHT)
        openCarfaxSearch(vin = NO_QUOTA_VIN)
        buyClickAction()
        setReportWithoutQuotaToDispatcher(isBought = true, dirType = DirType.BOUGHT)
        webServerRule.routing { getBoughtReportsListNormalWithOffer().watch { checkRequestWasCalled() } }
        performPayment { clickOnPayButton() }
        performCarfaxReport { waitCarfaxReport() }
        checkCarfaxReport { isDownloadPdfButtonVisible(true) }
        Espresso.pressBack()
        checkCarfaxList {
            isCarfaxTitleShown()
            isCarfaxSnippetWithTitleDisplayed("Mercedes-Benz S-Класс")
        }
    }

    private fun checkPaidReportFromOfferCard() {
        checkCarfaxReport { isDownloadPdfButtonVisible(true) }
        pressBack()
        performOfferCardVin { scrollToVinReport() }
        checkOfferCardVin { isFreeReportButtonNotExists() }

    }

    private fun openCarfaxSearch(vin: String) {
        deeplinkActivityRule.launchActivity(getDeepLinkIntent("https://auto.ru/history/$vin"))
        performCarfaxSearch { waitCarfaxSearchToLoad() }
    }

    private fun openToOfferCard() {
        ActivityTestRule(DeeplinkActivity::class.java, true, false)
            .launchActivity(getDeepLinkIntent(URL_PREFIX + REQUEST_OFFER_ID))
    }

    private fun setPaidOfferReportWithQuotaToDispatcher() {
        rawCarfaxOfferDispatcherHolder.innerDispatcher = RawCarfaxOfferDispatcher(
            requestOfferId = REQUEST_OFFER_ID,
            fileOfferId = QUOTA_FILE_OFFER_ID,
            dirType = DirType.BOUGHT
        )
        webServerRule.routing {
            makeXmlForOffer(
                offerId = REQUEST_OFFER_ID,
                dirType = DirType.BOUGHT,
                fileName = QUOTA_FILE_OFFER_ID,
            )
            makeXmlForReportByOfferId(
                offerId = REQUEST_OFFER_ID,
                dirType = DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(freeReportPromoXml)) }
            )
        }
    }

    private fun setFreeOfferReportWithQuotaToDispatcher() {
        rawCarfaxOfferDispatcherHolder.innerDispatcher = RawCarfaxOfferDispatcher(
            requestOfferId = REQUEST_OFFER_ID,
            fileOfferId = QUOTA_FILE_OFFER_ID,
            dirType = DirType.NOT_BOUGHT
        )
        webServerRule.routing {
            makeXmlForOffer(
                offerId = REQUEST_OFFER_ID,
                dirType = DirType.NOT_BOUGHT,
                fileName = QUOTA_FILE_OFFER_ID,
            )
            makeXmlForReportByOfferId(
                offerId = REQUEST_OFFER_ID,
                dirType = DirType.NOT_BOUGHT,
                fileName = "1093091228-7b1c1069",
                mapper = { copyWithNewCarfaxResponse(listOf(freeReportPromoXml)) }
            )
        }
    }

    private fun setReportWithQuotaToDispatcher(isBought: Boolean, dirType: DirType) {
        rawCarfaxOfferDispatcherHolder.innerDispatcher = RawCarfaxReportDispatcher(
            vin = QUOTA_VIN,
            isBought = isBought
        )
        webServerRule.routing {
            makeXmlForOffer(
                offerId = REQUEST_OFFER_ID,
                dirType = dirType
            )
            makeXmlForReport(
                vinOrLicense = QUOTA_VIN,
                dirType = dirType,
                mapper = { copyWithNewCarfaxResponse(listOf(freeReportPromoXml)) }
            )
        }
    }

    private fun setReportWithoutQuotaToDispatcher(isBought: Boolean, dirType: DirType) {
        rawCarfaxOfferDispatcherHolder.innerDispatcher = RawCarfaxReportDispatcher(
            vin = NO_QUOTA_VIN,
            isBought = isBought
        )
        webServerRule.routing {
            makeXmlForOffer(
                offerId = REQUEST_OFFER_ID,
                dirType = dirType
            )
            makeXmlForReport(
                vinOrLicense = NO_QUOTA_VIN,
                dirType = dirType,
                mapper = { copyWithNewCarfaxResponse(listOf(freeReportPromoXml)) }
            )
        }
    }

    private fun setOfferReportWithoutQuotaToDispatcher(isBought: Boolean, dirType: DirType) {
        rawCarfaxOfferDispatcherHolder.innerDispatcher = RawCarfaxOfferDispatcher(
            requestOfferId = REQUEST_OFFER_ID,
            fileOfferId = NO_QUOTA_FILE_OFFER_ID,
            dirType = if (isBought) DirType.COMMON_BOUGHT else DirType.COMMON_NOT_BOUGHT
        )
        webServerRule.routing {
            makeXmlForOffer(
                offerId = REQUEST_OFFER_ID,
                dirType = dirType
            )
            makeXmlForReportByOfferId(
                offerId = REQUEST_OFFER_ID,
                dirType = dirType,
                fileName = if (dirType == DirType.BOUGHT) "XTA21154094778989" else "1097084632-f8004e93",
                mapper = { copyWithNewCarfaxResponse(listOf(freeReportPromoXml)) }
            )
        }
    }

    private fun setPaymentRouting(reportCount: String, from: String, paymentSystem: PaymentSystem) {
        webServerRule.routing {
            postInitPayment(paymentSystem).watch {
                checkBody {
                    asObject {
                        get("subscribe_purchase").asObject {
                            get("count").assertValue(reportCount)
                            get("category").assertValue("CARS")
                            get("section").assertValue("USED")
                            get("offer_id").assertValue(REQUEST_OFFER_ID)
                        }
                        get("product").asArray {
                            single { e -> e is BodyNode.Object }.asObject { get("name").assertValue("offers-history-reports") }
                            single { e -> e is BodyNode.Object }.asObject { get("count").assertValue("1") }
                        }
                        get("statistics_parameters").asObject {
                            get("from").assertValue(from)
                            get("platform").assertValue("PLATFORM_ANDROID")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val URL_PREFIX = "https://auto.ru/cars/used/sale/"
        private const val REQUEST_OFFER_ID = "1083763087-cc26905f"
        private const val QUOTA_FILE_OFFER_ID = "1099518850-e92afbb0"
        private const val NO_QUOTA_FILE_OFFER_ID = "1097084632-f8004e90"
        private const val QUOTA_VIN = "JSAGYB21S00350739"
        private const val NO_QUOTA_VIN = "Z8T4DNFUCDM014995"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<PaymentSystem> = PaymentSystem.values().toList()
    }

}
