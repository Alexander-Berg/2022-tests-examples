package ru.auto.ara.test.offers

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.TestGeoRepository
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.GeoSuggestRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(AndroidJUnit4::class)
class OffersTest {

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        WebServerRule(),
        GeoSuggestRule(TestGeoRepository.GeoArgs.moscow300()),
        GrantPermissionsRule(),
        activityScenarioRule<MainActivity>()
    )

    @Before
    fun openOffers() {
        performMain {
            openLowTab(R.string.add_offer)
        }
    }

    @Test
    fun shouldOpenLogin() {
        performOffers {
            interactions.onLogin().checkIsCompletelyDisplayed().performClick()
        }
        performLogin {}.checkResult { isPhoneAuth() }
    }

    @Test
    fun shouldSeeBottomSheetContent() {
        performOffers {
            interactions.onAddAdvFree().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isBottomSheetContent()
        }
    }

    @Test
    fun shouldCloseBottomSheetContent() {
        performOffers {
            interactions.onAddAdvFree().waitUntilIsCompletelyDisplayed().performClick()
            interactions.onClose().waitUntilIsCompletelyDisplayed().performClick()
        }.checkResult {
            isEmptyUnauthorized()
        }
    }
}
