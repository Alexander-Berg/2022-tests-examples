package ru.auto.ara.test.offer.vin

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.getRawReport
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.freeReportPromoXml
import ru.auto.ara.core.mapping.ssr.getContentCell
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class VinReportLoggedInTest {

    private val URL_PREFIX = "https://auto.ru/cars/used/sale/"
    private val OFFER_ID = "1083763087-cc26905f"
    private val FILE_OFFER_ID = "1093024666-aa502a2a"
    private val activityScenarioRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule {
        getRawReport(
            requestOfferId = OFFER_ID,
            fileOfferId = FILE_OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
        )
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getContentCell(), freeReportPromoXml)) }
        )
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        getOffer(OFFER_ID)
        userSetup()
    }

    @JvmField
    @Rule
    var rules = baseRuleChain(
        webServerRule,
        activityScenarioRule,
        SetupAuthRule(),
        SetPreferencesRule()
    )

    @Before
    fun goToAutocodeReport() {
        activityScenarioRule.launchDeepLinkActivity(URL_PREFIX + OFFER_ID)
        performOfferCardVin { scrollToVinReport() }
    }

    @Test
    fun shouldOpenReportCloseAndOpenAgain() {
        performOfferCardVin { clickShowFreeReport() }
        checkVinReport { isVinReport() }
        pressBack()
        checkOfferCard { isOfferCard() }
        performOfferCardVin { clickShowFreeReport() }
        checkVinReport { isVinReport() }
    }

}
