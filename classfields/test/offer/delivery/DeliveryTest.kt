package ru.auto.ara.test.offer.delivery

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class DeliveryTest {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(category = "cars", offerId = "1090274296-54049309")
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1090274296-54049309")
    }

    @Test
    fun shouldOpenDeliveryRegions() {
        performOfferCard { scrollToText("Привод") }.checkResult {
            isDeliveryBlockDisplayed(
                title = "Доставка из Москвы",
                description = "Дилер доставит этот автомобиль в Челябинск, Среднеуральск, Урай и ещё 1. Подробности уточняйте по телефону."
            )
        }
        performOfferCard {
            interactions.onDeliveryDesc().performClickClickableSpan("ещё 1")
        }.checkResult {
            isDeliveryBottomSheetDisplayed(listOf("Челябинск", "Среднеуральск", "Урай", "Сургут"))
        }
    }
}
