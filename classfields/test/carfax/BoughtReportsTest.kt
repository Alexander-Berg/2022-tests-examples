package ru.auto.ara.test.carfax

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.carfax.getBoughtReports
import ru.auto.ara.core.dispatchers.carfax.getBoughtReportsListEmpty
import ru.auto.ara.core.dispatchers.carfax.getBoughtReportsListNormalWithOffer
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxReportDispatcher.Companion.sampleReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForSearch
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.payment.PaymentSystem
import ru.auto.ara.core.dispatchers.payment.getPaymentClosed
import ru.auto.ara.core.dispatchers.payment.postInitPayment
import ru.auto.ara.core.dispatchers.payment.postProcessPayment
import ru.auto.ara.core.dispatchers.payment.postStartPayment
import ru.auto.ara.core.dispatchers.stub.StubGetCatalogAllDictionariesDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.carfax.updateReport
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.carfax.checkCarfaxList
import ru.auto.ara.core.robot.carfax.checkCarfaxSearch
import ru.auto.ara.core.robot.carfax.performCarfaxList
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.carfax.performCarfaxSearch
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.payment.checkPayment
import ru.auto.ara.core.robot.payment.performPayment
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.TrustPaymentControllerFactoryRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.pressBack
import ru.auto.data.model.network.scala.autocode.NWSourcesBlock
import ru.auto.feature.carfax.interactor.CarfaxInteractor
import ru.auto.feature.carfax.interactor.CarfaxInteractor.Companion.SAMPLE_VIN

@RunWith(AndroidJUnit4::class)
class BoughtReportsTest {

    private val CARS_CATEGORY = "cars"
    private val OFFER_ID = "1092718938-48ec2434"
    private val VIN = "WBAWY31010L526779"

    private val reportDispatcherHolder = DispatcherHolder()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        reportDispatcherHolder,
        GetOfferDispatcher.getOffer(CARS_CATEGORY, OFFER_ID),
        BreadcrumbsSuggestDispatcher.carMarks(),
        StubGetCatalogAllDictionariesDispatcher,
        RawCarfaxReportDispatcher(vin = "Z8T4DNFUCDM014995", isBought = false),
    )

    private val webServerRule = WebServerRule {
        delegateDispatchers(dispatchers)
        sampleReport()
        userSetup()
    }

    private class TestHistoryBlock(
        titleRes: Int,
        val imageRes: Int,
        descriptionRes: Int,
    ) {
        val title = getResourceString(titleRes)
        val description = getResourceString(descriptionRes)
    }

    private val historyPromoBlocks = listOf(
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_mileages_title,
            imageRes = R.drawable.image_carfax_mileages_light,
            descriptionRes = R.string.carfax_promo_mileages_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_car_score_title,
            imageRes = R.drawable.image_carfax_score_light,
            descriptionRes = R.string.carfax_promo_car_score_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_autoru_offers_title,
            imageRes = R.drawable.image_carfax_autoru_offers_light,
            descriptionRes = R.string.carfax_promo_autoru_offers_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_legal_title,
            imageRes = R.drawable.image_carfax_legal_light,
            descriptionRes = R.string.carfax_promo_legal_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_taxi_title,
            imageRes = R.drawable.image_carfax_taxi_light,
            descriptionRes = R.string.carfax_promo_taxi_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_history_title,
            imageRes = R.drawable.image_carfax_history_light,
            descriptionRes = R.string.carfax_promo_history_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_repair_calculation_title,
            imageRes = R.drawable.image_carfax_repair_calculation_light,
            descriptionRes = R.string.carfax_promo_repair_calculation_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_total_auction_title,
            imageRes = R.drawable.image_carfax_total_auction_light,
            descriptionRes = R.string.carfax_promo_total_auction_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_certification_title,
            imageRes = R.drawable.image_carfax_certification_light,
            descriptionRes = R.string.carfax_promo_certification_description
        ),
        TestHistoryBlock(
            titleRes = R.string.carfax_promo_recalls_title,
            imageRes = R.drawable.image_carfax_recalls,
            descriptionRes = R.string.carfax_promo_recalls_description
        )
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetPreferencesRule(),
        SetupAuthRule(),
        activityScenarioRule<MainActivity>(),
        TrustPaymentControllerFactoryRule(),
    )

    @Test
    fun shouldSeePromo() {
        webServerRule.routing {
            getBoughtReportsListEmpty()
            makeXmlForReport(
                vinOrLicense = CarfaxInteractor.SAMPLE_VIN,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
                fileName = CarfaxInteractor.SAMPLE_VIN,
            ).watch { isRequestCalled() }
        }
        performMain { openReportsTab() }
        checkCarfaxList {
            isCarfaxTitleShown()
            isCarfaxBlockImageShownAt(2, R.drawable.image_carfax_light)
            isCarfaxBlockDescriptionShownAt(2, getResourceString(R.string.carfax_promo1_description))
            isCarfaxTitleNotSnownAt(2)
            isHeaderShownAt(3, getResourceString(R.string.carfax_promo_title))
            historyPromoBlocks.forEachIndexed { index, block ->
                // 4 -> header + divider + promo without title + separate title
                isCarfaxBlockShownAt(index + 4, block.title, block.imageRes, block.description)
            }
        }
    }

    @Test
    fun shouldSeeInProgressBoughtReportInList() {
        webServerRule.routing {
            getBoughtReports(
                assetPath = "carfax/bought-reports/bought_carfax_reports.json",
                mapper = {
                    updateReport(
                        vin = "WDD2229851A278094",
                        mapper = {
                            copy(
                                sources = NWSourcesBlock(
                                    sources_count = 7,
                                    ready_count = 6,
                                    text = "Опрошено 6 из 7 источников.\nНайдено 12 записей.",
                                    records_count = 12
                                )
                            )
                        }
                    )
                }
            )
        }
        performMain { openReportsTab() }
        checkCarfaxList {
            isCarfaxTitleShown()
            isNotificationShown()
            interactions.onNotificationBlockTitle()
                .waitUntilIsCompletelyDisplayed()
                .checkWithClearText(R.string.carfax_report_in_progress_title)
            interactions.onNotificationBlockDescription()
                .waitUntilIsCompletelyDisplayed()
                .checkWithClearText("Опрошено 6 из 7 источников.\nНайдено 12 записей.")
            interactions.onNotificationBlockProgress().waitUntilIsCompletelyDisplayed()
        }
    }

    @Test
    fun shouldSeeBoughtReportInListWithOffer() {
        webServerRule.routing { getBoughtReports(assetPath = "carfax/bought-reports/bought_carfax_reports.json") }
        performMain { openReportsTab() }
        checkCarfaxList {
            isCarfaxTitleShown()
            isNotificationNotShown()
            isCarfaxImageShown()
            with(interactions) {
                checkParam("Дата проверки", "23 марта 2021")
                checkParam("VIN", "WDD2229851A278094")
                checkParam("Двигатель", "4,7 л / 456 л.с.")
                checkParam("Цвет", "ЧЕРНО-КРАСНЫЙ")
                onSnippetTitle().waitUntilIsCompletelyDisplayed().checkWithClearText("Mercedes-Benz S-Класс")
                onSnippetSubtitle().waitUntilIsCompletelyDisplayed().checkWithClearText("2016 г.")
                onSnippetShowOfferButton().waitUntilIsCompletelyDisplayed().checkWithClearText(R.string.offer)
            }
            isShowCarfaxReportButtonDisplayed()
            isHowItsWorksGone()
        }
    }

    @Test
    fun shouldSeeBoughtReportInList() {
        webServerRule.routing {
            getBoughtReports(
                assetPath = "carfax/bought-reports/bought_carfax_reports.json",
                mapper = { updateReport(vin = "WDD2229851A278094", mapper = { copy(offer_id = null) }) }
            )
        }
        performMain { openReportsTab() }
        checkCarfaxList {
            isShowCarfaxReportButtonDisplayed()
            with(interactions) {
                onSnippetShowOfferButton().checkIsGone()
            }
        }
    }

    @Test
    fun shouldOpenReportOnClickShowReport() {
        webServerRule.routing {
            getBoughtReports(
                assetPath = "carfax/bought-reports/bought_carfax_reports.json",
                mapper = { updateReport(vin = "WDD2229851A278094", mapper = { copy(vin = VIN) }) }
            )
            makeXmlForReport(
                vinOrLicense = VIN,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
        }
        performMain { openReportsTab() }
        performCarfaxList {
            interactions.onSnippetTitle().waitUntilIsCompletelyDisplayed()
            interactions.onSnippetShowReportButton().performClick()
        }
        performCarfaxReport { waitCarfaxReport() }
    }


    @Test
    fun shouldOpenOfferOnClickShowOffer() {
        webServerRule.routing {
            getBoughtReports(
                assetPath = "carfax/bought-reports/bought_carfax_reports.json",
                mapper = { updateReport(vin = "WDD2229851A278094", mapper = { copy(offer_id = OFFER_ID) }) }
            )
        }
        performMain { openReportsTab() }
        performCarfaxList {
            interactions.onSnippetTitle().waitUntilIsCompletelyDisplayed()
            interactions.onSnippetShowOfferButton().performClick()
        }
        checkOfferCard { isOfferCard() }
    }

    @Test
    fun shouldSelectAndCopyVinOnCarfaxSnippet() {
        webServerRule.routing {
            getBoughtReports(
                assetPath = "carfax/bought-reports/bought_carfax_reports.json",
                mapper = { updateReport(vin = "WDD2229851A278094", mapper = { copy(vin = VIN) }) }
            )
        }
        performCommon { login() }
        performMain { openReportsTab() }
        performCarfaxList { longClickOnSnippetVin(VIN) }
        performCommon { pressCopyInSystemDialog() }
        checkCommon { isClipboardText(VIN) }
    }

    @Test
    fun shouldUpdateReportsListAfterBuyReportInSearchScreenByTrust() {
        shouldUpdateReportsListAfterBuyReportInSearchScreen(PaymentSystem.TRUST)
    }

    @Test
    fun shouldUpdateReportsListAfterBuyReportInSearchScreenByYandexkassa() {
        shouldUpdateReportsListAfterBuyReportInSearchScreen(PaymentSystem.YANDEXKASSA)
    }

    private fun shouldUpdateReportsListAfterBuyReportInSearchScreen(paymentSystem: PaymentSystem) {
        val vin = "Z8T4DNFUCDM014995"
        webServerRule.routing {
            postInitPayment(paymentSystem)
            postStartPayment(paymentSystem)
            postProcessPayment(paymentSystem)
            getPaymentClosed(paymentSystem)
            getBoughtReportsListEmpty()
            makeXmlForSearch(vinOrLicense = vin, isBought = false)
            makeXmlForReport(
                vinOrLicense = vin,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                fileName = vin,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
            makeXmlForReport(
                vinOrLicense = SAMPLE_VIN,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
        }
        performMain { openReportsTab() }
        performCarfaxList { clickCarfaxInput() }
        performCarfaxSearch {
            inputVin(vin)
            clickSearchReport()
            waitCarfaxSearchToLoad()
        }
        performCarfaxSearch { scrollToBottom() }
        checkCarfaxSearch { checkBuyButton() }
        performCarfaxSearch { clickBuyReport() }
        checkPayment { isPaymentMethodDisplayed("MASTERCARD **** 4444") }
        performPayment { clickOnPayButton() }
        performCarfaxReport { waitCarfaxReport() }
        webServerRule.routing {
            getBoughtReportsListNormalWithOffer().watch { checkRequestWasCalled() }
        }
        pressBack()
        checkCarfaxList {
            isCarfaxTitleShown()
            isCarfaxSnippetWithTitleDisplayed("Mercedes-Benz S-Класс")
        }
    }

}
