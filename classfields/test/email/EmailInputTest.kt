package com.yandex.mobile.realty.test.email

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.EmailInputActivityTestRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.EmailInputScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 4/5/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class EmailInputTest {

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        EmailInputActivityTestRule()
    )

    @Test
    fun inputEmail() {
        onScreen<EmailInputScreen> {
            waitUntil { proceedButton.isCompletelyDisplayed() }
            closeKeyboard()

            isViewStateMatches("EmailInputTest/inputEmail/emptyScreen")
            proceedButton.isNotEnabled()

            emailInput.typeText(INVALID_EMAIL)

            isViewStateMatches("EmailInputTest/inputEmail/invalidEmail")
            proceedButton.isNotEnabled()

            emailInput.clearText()

            isViewStateMatches("EmailInputTest/inputEmail/emptyScreen")
            proceedButton.isNotEnabled()

            emailInput.typeText(VALID_EMAIL)

            isViewStateMatches("EmailInputTest/inputEmail/validEmail")
            proceedButton.isEnabled()

            emailInput.clearText()

            isViewStateMatches("EmailInputTest/inputEmail/emptyScreen")
            proceedButton.isNotEnabled()
        }
    }

    companion object {

        private const val VALID_EMAIL = "test@test.ru"
        private const val INVALID_EMAIL = "test"
    }
}
