package ru.auto.ara.test.offer.complectation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ComplectationTest {
    private val offerDispatcherHolder = DispatcherHolder()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        offerDispatcherHolder
    )
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldSeeNotExpandedComplectationBlock() {
        openOfferAndScroll("1084250931-f8070529")
        waitSomething(1, TimeUnit.SECONDS)
        checkOfferCard {
            interactions.onComplectationWithName("530i xDrive M Sport").waitUntilIsCompletelyDisplayed()
            isNotExpandedComplecationBlockDisplayed("Безопасность", "7")
        }
    }

    @Test
    fun shouldSeeComplectationBlockWithNoName() {
        openOfferAndScroll("1083103750-310000")
        waitSomething(1, TimeUnit.SECONDS)
        checkOfferCard {
            interactions.onComplectationWithName("").waitUntilIsCompletelyDisplayed()
        }
    }

    @Test
    fun shouldExpandComplectationBlock() {
        openOfferAndScroll("1084250931-f8070529")
        waitSomething(1, TimeUnit.SECONDS)
        performOfferCard {
            interactions.onComplectationBlockTitle("Безопасность").waitUntilIsCompletelyDisplayed().performClick()
            waitAnimation()
        }.checkResult {
            isExpandedComplecationBlockDisplayed(
                blockTitle = "Безопасность",
                counter = "7",
                options = listOf(
                    "Антиблокировочная система (ABS)",
                    "Крепление для детского кресла",
                    "Подушка безопасности водителя",
                    "Подушка безопасности пассажира",
                    "Подушки безопасности боковые",
                    "Подушки безопасности оконные (шторки)",
                    "Система стабилизации"
                )
            )
        }
    }

    @Test
    fun shouldCollapseComplectationBlock() {
        openOfferAndScroll("1084250931-f8070529")
        waitSomething(1, TimeUnit.SECONDS)
        performOfferCard {
            interactions.onComplectationBlockTitle("Безопасность").waitUntilIsCompletelyDisplayed().performClick()
            waitAnimation()
        }.checkResult {
            isExpandedComplecationBlockDisplayed(
                blockTitle = "Безопасность",
                counter = "7",
                options = listOf(
                    "Антиблокировочная система (ABS)",
                    "Крепление для детского кресла",
                    "Подушка безопасности водителя",
                    "Подушка безопасности пассажира",
                    "Подушки безопасности боковые",
                    "Подушки безопасности оконные (шторки)",
                    "Система стабилизации"
                )
            )
        }
        performOfferCard {
            waitSomething(1, TimeUnit.SECONDS)
            interactions.onComplectationBlockTitle("Безопасность").waitUntilIsCompletelyDisplayed().performClick()
            waitAnimation()
        }.checkResult {
            isNotExpandedComplecationBlockDisplayed("Безопасность", "7")
        }
    }

    private fun openOfferAndScroll(offerId: String) {
        offerDispatcherHolder.innerDispatcher = GetOfferDispatcher.getOffer("cars", offerId)
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/$offerId")
        waitSomething(200, TimeUnit.MILLISECONDS)
        performOfferCard {
            collapseAppBar()
            scrollToText("Элементы экстерьера")
        }
    }

    private fun waitAnimation() = waitSomething(500, TimeUnit.MILLISECONDS)
}
