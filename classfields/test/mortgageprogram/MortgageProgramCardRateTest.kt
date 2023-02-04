package com.yandex.mobile.realty.test.mortgageprogram

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramCardActivityTestRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.InfoDialogScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.input.createStandardProgram
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author shpigun on 29.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageProgramCardRateTest {

    private val activityTestRule = MortgageProgramCardActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(activityTestRule)

    @Test
    fun shouldShowRateWithDiscount() {
        val startIntent = MortgageProgramCardActivityTestRule.createIntent(
            createStandardProgram()
                .copy(
                    rateDescription = null,
                    rateWithDiscount = Range.LowerBound(7.59)
                )
        )
        activityTestRule.launchActivity(startIntent)
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(rateDetailItem)
                .isViewStateMatches(
                    "MortgageProgramCardRateTest/shouldShowRateWithDiscount/rateItem"
                )
        }
    }

    @Test
    fun shouldShowRateWithInfo() {
        val startIntent = MortgageProgramCardActivityTestRule.createIntent(createStandardProgram())
        activityTestRule.launchActivity(startIntent)
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(rateDetailItem)
                .isViewStateMatches(
                    "MortgageProgramCardRateTest/shouldShowRateWithInfo/rateItem"
                )
                .click()
        }
        onScreen<InfoDialogScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches("MortgageProgramCardRateTest/shouldShowRateWithInfo/infoDialog")
        }
    }

    @Test
    fun shouldShowRateWithInfoAndDiscount() {
        val startIntent = MortgageProgramCardActivityTestRule.createIntent(
            createStandardProgram()
                .copy(rateWithDiscount = Range.LowerBound(7.59))
        )
        activityTestRule.launchActivity(startIntent)
        onScreen<MortgageProgramCardScreen> {
            listView.scrollTo(rateDetailItem)
                .isViewStateMatches(
                    "MortgageProgramCardRateTest/shouldShowRateWithInfoAndDiscount/rateItem"
                )
                .click()
        }
        onScreen<InfoDialogScreen> {
            titleView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(
                "MortgageProgramCardRateTest/shouldShowRateWithInfoAndDiscount/infoDialog"
            )
        }
    }
}
