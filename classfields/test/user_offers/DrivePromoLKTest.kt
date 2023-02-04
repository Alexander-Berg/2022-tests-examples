package ru.auto.ara.test.user_offers

import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.AutoApplication
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.dealer.getDealerCampaigns
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.dispatchers.user_offers.getCommActiveUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getDealerUserOffers
import ru.auto.ara.core.dispatchers.user_offers.getMotoActiveUserOffers
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.dealeroffers.performDealerOffers
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.OffersRobot
import ru.auto.ara.core.robot.useroffers.checkOffers
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.data.PrefsDelegate
import ru.auto.ara.filter.screen.MultiGeoValue
import ru.auto.ara.presentation.presenter.offer.controller.SHOW_DRIVE_PROMO_LK
import ru.auto.data.model.geo.SuggestGeoItem

private const val PROMO_CODE = "AUTORU-rYoPF"

private const val PROMO_URL_LK_MOS = "https://6lg6.adj.st/promo/$PROMO_CODE?adjust_t=8vd4175&" +
    "adjust_campaign=RU-MOS-MOW&adjust_creative=auto_listing&adjust_adgroup=new_user&" +
    "adjust_deeplink=yandexdrive%3A%2F%2Fpromo%2F$PROMO_CODE"
private const val PROMO_URL_LK_SPB = "https://6lg6.adj.st/promo/$PROMO_CODE?adjust_t=8vd4175&" +
    "adjust_campaign=RU-SPE-SPB&adjust_creative=auto_listing&adjust_adgroup=new_user&" +
    "adjust_deeplink=yandexdrive%3A%2F%2Fpromo%2F$PROMO_CODE"
private const val PROMO_URL_LK_KAZ = "https://6lg6.adj.st/promo/$PROMO_CODE?adjust_t=8vd4175&" +
    "adjust_campaign=RU-TA-KZN&adjust_creative=auto_listing&adjust_adgroup=new_user&" +
    "adjust_deeplink=yandexdrive%3A%2F%2Fpromo%2F$PROMO_CODE"

@RunWith(AndroidJUnit4::class)
class DrivePromoLKTest {
    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val webServerRule = WebServerRule {
        stub {
            userSetup()
            delegateDispatcher(GetUserOffersDispatcher.active())
        }
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupTimeRule(date = "12.06.2020", time = "12:00"),
        SetPreferencesRule()
    )

    private fun OffersRobot.waitForOfferSnippet() {
        waitForOfferSnippets(1)
    }

    @Test
    fun shouldHidePromoAfterClose() {
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        openLK()
        performOffers {
            scrollToNextPage()
            clickOnCloseDrivePromo()
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    private fun openLK() {
        performCommon { login() }
        activityRule.launchActivity()
        performMain {
            waitLowTabsShown()
            openLowTab(R.string.offers)
        }
        performOffers {
            waitForOfferSnippet()
        }
    }

    private fun openLKDealer() {
        webServerRule.routing {
            getDealerCampaigns()
            getDealerUserOffers()
            userSetupDealer()
        }
        performCommon { login() }
        activityRule.launchActivity()
        performMain {
            waitLowTabsShown()
            openLowTab(R.string.offers)
        }
        performDealerOffers {
            waitForDealerOfferSnippets(2)
        }
    }

    @Test
    fun shouldNotShowPromoWhenFlagSet() {
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        val prefs = PrefsDelegate(PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext()))
        prefs.saveBoolean(SHOW_DRIVE_PROMO_LK, false)
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldNotShowPromoWhenNoActiveOffers() {
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        webServerRule.routing {
            delegateDispatchers(GetUserOffersDispatcher.inactive())
        }
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldNotShowPromoWhenMotoActiveOffer() {
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        webServerRule.routing {
            getMotoActiveUserOffers()
        }
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldNotShowPromoWhenCommActiveOffer() {
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        webServerRule.routing {
            getCommActiveUserOffers()
        }
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldNotShowPromoWhenNotSupportedRegion() {
        setSavedRegion(SuggestGeoItem("1", "Россия"))
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldNotShowPromoForDealer() {
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        openLKDealer()
        performDealerOffers {
            scrollToOfferAt(1)
        }
        checkOffers {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldShowPromoForMoscow() {
        Intents.init()
        setSavedRegion(SuggestGeoItem("213", "Москва"))
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoDisplayedCorrectly(PROMO_CODE)
        }
        performOffers {
            clickOnDrivePromoAction()
        }
        checkCommon {
            isBrowserIntentOpened(PROMO_URL_LK_MOS)
        }
        Intents.release()
    }

    @Test
    fun shouldShowPromoForSbp() {
        Intents.init()
        setSavedRegion(SuggestGeoItem("2", "Санкт-Петербург"))
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoDisplayedCorrectly(PROMO_CODE)
        }
        performOffers {
            clickOnDrivePromoAction()
        }
        checkCommon {
            isBrowserIntentOpened(PROMO_URL_LK_SPB)
        }
        Intents.release()
    }

    @Test
    fun shouldShowPromoForKazan() {
        Intents.init()
        setSavedRegion(SuggestGeoItem("43", "Казань"))
        openLK()
        performOffers {
            scrollToNextPage()
        }
        checkOffers {
            isDrivePromoDisplayedCorrectly(PROMO_CODE)
        }
        performOffers {
            clickOnDrivePromoAction()
        }
        checkCommon {
            isBrowserIntentOpened(PROMO_URL_LK_KAZ)
        }
        Intents.release()
    }

    private fun setSavedRegion(geoItem: SuggestGeoItem) {
        AutoApplication.COMPONENT_MANAGER.main.geoStateProvider.saveGeoState(
            MultiGeoValue(listOf(geoItem))
        )
    }

}
