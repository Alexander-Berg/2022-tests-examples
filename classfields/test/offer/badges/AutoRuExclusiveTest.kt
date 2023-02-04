package ru.auto.ara.test.offer.badges

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView

@RunWith(AndroidJUnit4::class)
class AutoRuExclusiveTest {
    private val uri = "https://auto.ru/cars/used/sale/1084155311-742cfbff"
    private val offerId = "1084155311-742cfbff"
    private val category = "cars"
    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetOfferDispatcher.getOffer(category, offerId)
    )


    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        GrantPermissionsRule(),
        activityRule,
        DisableAdsRule(),
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityRule.launchDeepLinkActivity(uri)
    }

    @Test
    fun shouldSeeAutoRuExclusiveBadge() {
        checkOfferCard {
            isAutoRuExclusiveInformerDisplayed()
        }
    }

    @Test
    fun shouldSeeAutoRuExclusiveBottomsheetAndCloseIt() {
        performOfferCard {
            interactions.onAutoRuExclusiveInformer().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isAutoRuExclusiveBottomSheetDisplayed()
        }
        performOfferCard {
            interactions.onBottomsheetCloseIcon().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isAutoRuExclusiveInformerDisplayed()
        }
    }

    @Test
    fun shouldSeeAutoRuExclusivePromoWebView() {
        performOfferCard {
            interactions.onAutoRuExclusiveInformer().waitUntilIsCompletelyDisplayed().performClick()
        }
        watchWebView {
            performOfferCard {
                interactions.onBottomsheetMoreButton().waitUntilIsCompletelyDisplayed().performClick()
            }
        }.checkResult {
            checkTitleMatches("Пользовательское соглашение сайта «AUTO.RU»")
            checkUrlMatches("https://yandex.ru/legal/autoru_terms_of_service/?lang=ru#1_1_17")
        }
    }

}
