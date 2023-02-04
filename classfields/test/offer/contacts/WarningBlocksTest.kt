package ru.auto.ara.test.offer.contacts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.actions.ViewActions.setAppBarExpandedState
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
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class WarningBlocksTest {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1082957054-8d55bf9a")
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
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")
        performOfferCard {
            interactions.onAppBar().waitUntilIsCompletelyDisplayed().perform(setAppBarExpandedState(false))
            scrollToComplain()
        }
    }

    @Test
    fun shouldSeeWarningBlocks() {
        checkOfferCard {
            isWarningBlockDisplayed(
                parentId = R.id.prepayment_warning,
                title = R.string.attention_title,
                description = R.string.prepayment_warning,
                icon = R.drawable.ic_thief
            )
            isWarningBlockDisplayed(
                parentId = R.id.phone_guardian_hint,
                title = R.string.phone_guardian_title,
                description = R.string.phone_guardian_hint,
                icon = R.drawable.ic_phone_guardian
            )
            isWarningBlockDisplayed(
                parentId = R.id.owner_driver_hint,
                title = R.string.owner_driver_title,
                description = R.string.owner_driver_hint,
                icon = R.drawable.ic_owner_driver
            )
        }
    }
}
