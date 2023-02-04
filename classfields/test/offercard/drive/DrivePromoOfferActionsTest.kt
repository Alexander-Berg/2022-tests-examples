package ru.auto.ara.test.offercard.drive

import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.LOCATION_MOS
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher.Companion.getGenericFeed
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

private const val PRIVATE_SELLER_CAR_OFFER = "1087439802-b6940925"
private const val PRIVATE_SELLER_MOTO_OFFER = "1894128-d229"
private const val PRIVATE_SELLER_COMM_OFFER = "10448426-ce654669"
private const val PROMO_CODE = "AUTORU-rYoPF"
private const val PROMO_URL_CARD_MOS = "https://6lg6.adj.st/promo/$PROMO_CODE?adjust_t=8vd4175&" +
    "adjust_campaign=RU-MOS-MOW&adjust_creative=auto_card&adjust_adgroup=new_user&" +
    "adjust_deeplink=yandexdrive%3A%2F%2Fpromo%2F$PROMO_CODE"

@RunWith(AndroidJUnit4::class)
class DrivePromoOfferActionsTest {
    private val webServerRule = WebServerRule {
        getOffer("cars", PRIVATE_SELLER_CAR_OFFER, location = LOCATION_MOS)
        getOffer(category = "moto", offerId = PRIVATE_SELLER_MOTO_OFFER)
        getOffer(category = "trucks", offerId = PRIVATE_SELLER_COMM_OFFER)
        delegateDispatcher(getGenericFeed())
    }
    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        SetPreferencesRule(),
        activityRule
    )

    @Test
    fun shouldHidePromoAfterClose() {
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$PRIVATE_SELLER_CAR_OFFER")
        scrollToPromoCar()
        performOfferCard {
            clickOnCloseDrivePromo()
        }
        checkOfferCard {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldNotShowPromoWhenReopeningCardAfterHide() {
        activityRule.launchDeepLinkActivity("https://auto.ru/rossiya/cars/used/?mark_model_nameplate=BMW%233ER%23%237744658")
        performSearchFeed { waitSearchFeed() }

        performSearchFeed { clickSnippetAtPosition(0) }
        scrollToPromoCar()
        performOfferCard { clickOnCloseDrivePromo() }
        checkOfferCard { isDrivePromoNotDisplayed() }

        Espresso.pressBack()
        performSearchFeed { waitSearchFeed() }
        performSearchFeed { clickSnippetAtPosition(0) }
        scrollToPromoCar()
        checkOfferCard {
            isDrivePromoNotDisplayed()
        }
    }

    @Test
    fun shouldOpenCorrectLinkOnPromo() {
        Intents.init()
        activityRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$PRIVATE_SELLER_CAR_OFFER")
        scrollToPromoCar()
        performOfferCard {
            clickOnDrivePromoAction()
        }
        checkCommon {
            isBrowserIntentOpened(PROMO_URL_CARD_MOS)
        }
        Intents.release()
    }

    @Test
    fun shouldNotShowPromoOnMotoCard() {
        activityRule.launchDeepLinkActivity("https://auto.ru/moto/used/sale/$PRIVATE_SELLER_MOTO_OFFER")
        scrollToComplain()
        checkOfferCard { isDrivePromoNotDisplayed() }
    }

    @Test
    fun shouldNotShowPromoOnCommCard() {
        activityRule.launchDeepLinkActivity("https://auto.ru/trucks/used/sale/$PRIVATE_SELLER_COMM_OFFER")
        scrollToComplain()
        checkOfferCard { isDrivePromoNotDisplayed() }
    }

    private fun scrollToPromoCar() {
        performOfferCard {
            waitCardOpened()
            collapseAppBar()
            scrollToComplain()
            scrollToManual()
        }
    }

    private fun scrollToComplain() {
        performOfferCard {
            waitCardOpened()
            collapseAppBar()
            scrollToCardBottom()
        }
    }

}
