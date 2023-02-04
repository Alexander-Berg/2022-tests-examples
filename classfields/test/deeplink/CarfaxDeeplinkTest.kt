package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.breadcrumbs.BreadcrumbsSuggestDispatcher
import ru.auto.ara.core.dispatchers.carfax.getBoughtReportsListEmpty
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxReportDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.getPromoCodeCell
import ru.auto.ara.core.robot.carfax.checkCarfaxList
import ru.auto.ara.core.robot.carfax.checkCarfaxSearch
import ru.auto.ara.core.robot.carfax.performCarfaxReport
import ru.auto.ara.core.robot.carfax.performCarfaxSearch
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.AppNotRunningRule
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.TestRandomProviderRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.util.ASTERISK
import ru.auto.feature.carfax.interactor.CarfaxInteractor
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CarfaxDeeplinkTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val rawCarfaxReportDispatcherHolder: DispatcherHolder = DispatcherHolder()
    private val parseDeeplinkDispatcherHolder: DispatcherHolder = DispatcherHolder()

    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetOfferDispatcher.getOffer(CARS_CATEGORY, OFFER_ID),
        RawCarfaxOfferDispatcher(
            requestOfferId = OFFER_ID,
            fileOfferId = RAW_CARFAX_OFFER_OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
        ),
        RawCarfaxReportDispatcher(
            vin = CarfaxInteractor.SAMPLE_VIN,
            fileVin = VIN,
            isBought = true
        ),
        parseDeeplinkDispatcherHolder,
        rawCarfaxReportDispatcherHolder,
        BreadcrumbsSuggestDispatcher.carMarks()
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule {
            userSetup()
            delegateDispatchers(dispatchers)
            getBoughtReportsListEmpty()
            makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT)
            makeXmlForReportByOfferId(
                offerId = OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
            makeXmlForReport(
                vinOrLicense = CarfaxInteractor.SAMPLE_VIN,
                dirType = RawCarfaxOfferDispatcher.DirType.BOUGHT,
                mapper = { copyWithNewCarfaxResponse(listOf(getPromoCodeCell())) }
            )
        },
        activityRule,
        TestRandomProviderRule { 0 },
        SetPreferencesRule(),
        SetupAuthRule(),
        DisableAdsRule(),
        AppNotRunningRule(),
    )

    @Test
    fun shouldShowCarfaxSearchFromDeepLinkWithVin() {
        rawCarfaxReportDispatcherHolder.innerDispatcher = RawCarfaxReportDispatcher(vin = VIN, isBought = false)
        openCarfaxDeepLink(VIN)
        checkCarfaxSearch { checkSearchEditText(VIN) }
    }

    @Test
    fun shouldShowCarfaxSearchFromAutoruAppDeepLinkWithVin() {
        rawCarfaxReportDispatcherHolder.innerDispatcher = RawCarfaxReportDispatcher(vin = VIN, isBought = false)
        openCarfaxAutoruAppDeepLink(VIN)
        checkCarfaxSearch { checkSearchEditText(VIN) }
    }

    @Test
    fun shouldShowCarfaxSearchFromDeepLinkWithLicenceNumber() {
        rawCarfaxReportDispatcherHolder.innerDispatcher = RawCarfaxReportDispatcher(vin = LICENCE_NUMBER, isBought = false)
        openCarfaxDeepLink(LICENCE_NUMBER)
        checkCarfaxSearch { checkSearchEditText(LICENCE_NUMBER) }
    }

    @Test
    fun shouldBackToCarfaxTabFromSearchOpenedFromDeeplink() {
        rawCarfaxReportDispatcherHolder.innerDispatcher = RawCarfaxReportDispatcher(vin = LICENCE_NUMBER, isBought = false)
        openCarfaxAutoruAppDeepLink(LICENCE_NUMBER)
        checkCarfaxSearch { checkSearchEditText(LICENCE_NUMBER) }
        performCarfaxSearch { clickBackButton() }
        checkCarfaxList { isCarfaxTitleShown() }
    }

    @Test
    fun shouldShowOfferCardCarfaxFromDeepLinkAuthorized() {
        parseDeeplinkDispatcherHolder.innerDispatcher = ParseDeeplinkDispatcher(DEEPLINK_PARSER_OFFER_RESPONSE)
        openCarfaxDeepLink(OFFER_ID)
        performCarfaxReport { waitCarfaxReport() }
    }

    @Test
    fun shouldShowOfferCardCarfaxFromAutoruAppDeepLinkAuthorized() {
        parseDeeplinkDispatcherHolder.innerDispatcher = ParseDeeplinkDispatcher(DEEPLINK_PARSER_OFFER_RESPONSE)
        openCarfaxAutoruAppDeepLink(OFFER_ID)
        performCarfaxReport { waitCarfaxReport() }
    }

    @Test
    fun shouldShowOfferCardFromDeepLinkNotAuthorized() {
        parseDeeplinkDispatcherHolder.innerDispatcher = ParseDeeplinkDispatcher(DEEPLINK_PARSER_OFFER_RESPONSE)
        performCommon { logout() }
        openCarfaxDeepLink(OFFER_ID)
        checkOfferCard { checkCarfaxBlockDisplayed() }
    }

    @Test
    fun shouldShowCarfaxTabFromBadDeepLink() {
        parseDeeplinkDispatcherHolder.innerDispatcher = ParseDeeplinkDispatcher(DEEPLINK_PARSER_BAD_RESPONSE)
        openCarfaxDeepLink("1093024666-lalalala")
        checkCarfaxList { isCarfaxTitleShown() }
    }

    @Test
    fun shouldShowCarfaxTabFromDeepLink() {
        openCarfaxDeepLink(ASTERISK)
        checkCarfaxList { isCarfaxTitleShown() }
    }

    @Test
    fun shouldShowCarfaxTabFromAutoruAppDeepLink() {
        openCarfaxAutoruAppDeepLink(ASTERISK)
        checkCarfaxList { isCarfaxTitleShown() }
    }

    @Test
    fun shouldShowCarfaxTabFromProAutoDeepLinkAutoru() {
        activityRule.launchDeepLinkActivity("autoru://pro.auto.ru")
        waitSomething(5, TimeUnit.SECONDS)
        checkCarfaxList { isCarfaxTitleShown() }
    }

    @Test
    fun shouldShowCarfaxTabFromProAutoDeepLinkHttps() {
        activityRule.launchDeepLinkActivity("https://pro.auto.ru")
        waitSomething(5, TimeUnit.SECONDS)
        checkCarfaxList { isCarfaxTitleShown() }
    }

    private fun openCarfaxDeepLink(query: String) {
        activityRule.launchDeepLinkActivity("https://auto.ru/history/$query")
        waitSomething(5, TimeUnit.SECONDS)
    }

    private fun openCarfaxAutoruAppDeepLink(query: String) {
        activityRule.launchDeepLinkActivity("autoru://app/history/$query")
        waitSomething(5, TimeUnit.SECONDS)
    }

    companion object {

        private const val VIN = "Z8T4DNFUCDM014995"
        private const val LICENCE_NUMBER = "A002AA23"

        private const val OFFER_ID = "1087439802-b6940925"
        private const val DEEPLINK_PARSER_OFFER_RESPONSE = "cars_used_sale_1087439802-b6940925"
        private const val DEEPLINK_PARSER_BAD_RESPONSE = "bad_request"

        private const val RAW_CARFAX_OFFER_OFFER_ID = "1093024666-aa502a2a"

        private const val CARS_CATEGORY = "cars"
    }
}
