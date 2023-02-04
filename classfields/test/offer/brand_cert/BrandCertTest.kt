package ru.auto.ara.test.offer.brand_cert

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.shark.getCalculatorParamsNotFound
import ru.auto.ara.core.robot.offercard.checkBrandCert
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER
import ru.auto.ara.core.testdata.OFFER_ID_WITH_CERTIFICATE_MANUFACTURER
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.data.offer.details.Advantage

@RunWith(AndroidJUnit4::class)
class BrandCertTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldShowBrandCertBlock() {
        openOfferCard(OFFER_ID_WITH_BRAND_CERT)
        performOfferCard {
            scrollToBrandCert()
        }.checkResult {
            isBrandCertScreenshotSame()
        }
    }

    @Test
    fun shouldShowBrandCertInfoFromBlock() {
        openOfferCard(OFFER_ID_WITH_BRAND_CERT)
        performOfferCard {
            scrollToBrandCert()
            clickOnBrandCert()
        }
        checkBrandCert {
            waitSomething(2) //let brand icon can be loaded
            isBottomsheetRecyclerScreenshotSame()
        }
    }

    @Test
    fun shouldShowBrandCertInfoFromAdvantage() {
        openOfferCard(OFFER_ID_WITH_ALL_ADVANTAGES_WITH_DATA_IN_OFFER)
        performOfferCard {
            scrollToAdvantages()
            val advantagePosition = Advantage.sortedAdvantages.indexOf(Advantage.CertificateManufacturer) - 2
            scrollToAdvantage(advantagePosition)
            clickOnAdvantage(advantagePosition)
        }
        checkBrandCert {
            waitSomething(4) //let brand icon can be loaded
            isBottomsheetRecyclerScreenshotSame()
        }
    }

    @Test
    fun shouldShowBrandCertInfoFromAdvantageSingle() {
        webServerRule.routing { getCalculatorParamsNotFound() }
        openOfferCard(OFFER_ID_WITH_CERTIFICATE_MANUFACTURER)
        performOfferCard {
            scrollToAdvantageSingle()
            clickOnAdvantageSingle()
        }
        checkBrandCert {
            waitSomething(2) //let brand icon can be loaded
            isBottomsheetRecyclerScreenshotSame()
        }
    }

    private fun openOfferCard(offerId: String) {
        webServerRule.routing { getOffer(offerId) }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + offerId)
    }

    companion object {
        private const val OFFER_ID_WITH_BRAND_CERT = "1097265576-4c551050"
        private const val OFFER_CARD_PATH = "https://auto.ru/cars/used/sale/"
    }
}
