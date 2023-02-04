package ru.auto.ara.test.offer.vin

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.carfax.report.RawCarfaxOfferDispatcher
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForOffer
import ru.auto.ara.core.dispatchers.carfax.report.makeXmlForReportByOfferId
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.mapping.ssr.copyWithNewCarfaxResponse
import ru.auto.ara.core.mapping.ssr.freeReportPromoXml
import ru.auto.ara.core.mapping.ssr.getContentCell
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkVinReport
import ru.auto.ara.core.robot.offercard.performOfferCardVin
import ru.auto.ara.core.robot.offercard.performVinReport
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class VinReportAuthTest {

    private val URL_PREFIX = "https://auto.ru/cars/used/sale/"
    private val OFFER_ID = "1083763087-cc26905f"
    private val FILE_OFFER_ID = "1093024666-aa502a2a"
    private val CATEGORY = "cars"
    private val PHONE = "+7 (000) 000-00-00"
    private val CODE = "0000"

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    val webServerRule = WebServerRule {
        delegateDispatchers(
            RawCarfaxOfferDispatcher(
                requestOfferId = OFFER_ID,
                fileOfferId = FILE_OFFER_ID,
                dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT
            ),
            GetOfferDispatcher.getOffer(CATEGORY, OFFER_ID)
        )
        userSetup()
        postLoginOrRegisterSuccess()
        makeXmlForOffer(offerId = OFFER_ID, dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT)
        makeXmlForReportByOfferId(
            offerId = OFFER_ID,
            dirType = RawCarfaxOfferDispatcher.DirType.NOT_BOUGHT,
            mapper = { copyWithNewCarfaxResponse(listOf(getContentCell(), freeReportPromoXml)) }
        )
    }

    @JvmField
    @Rule
    var rules = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun goToAutocodeReport() {
        activityTestRule.launchDeepLinkActivity(URL_PREFIX + OFFER_ID)
        performOfferCardVin { clickShowFreeReport() }
        performLogin { waitLogin() }
    }

    @Test
    fun onlyLoggedInAllowedViewReport() {
        checkLogin { isPhoneAuth() }
    }

    @Test
    fun shouldBackToCardFromLogin() {
        performLogin { close() }
        checkOfferCard { isOfferCard() }
    }

    @Test
    fun shouldOpenLoginAgainAfterCancelFirstLogin() {
        performLogin { close() }
        performOfferCardVin { clickShowFreeReport() }
        checkLogin { isPhoneAuth() }
    }

    @Test
    fun shouldBackToCardAfterCloseReport() {
        performLogin { loginWithPhoneAndCode(PHONE, CODE) }
        checkVinReport { isVinReport() }
        performVinReport { close() }
        checkOfferCard { isOfferCard() }
    }
}
