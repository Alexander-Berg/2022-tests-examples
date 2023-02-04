package ru.auto.ara.test.login

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.interaction.auth.LoginInteractions.onLoginBtn
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.utils.activityScenarioWithFragmentRule
import ru.auto.ara.ui.activity.PhoneAuthActivity
import ru.auto.ara.ui.fragment.auth.EmailAuthFragment

@RunWith(Parameterized::class)
class EmailValidationTest(private val text: String, private val check: () -> Unit) {

    @JvmField
    @Rule
    val activityTestRule = activityScenarioWithFragmentRule<PhoneAuthActivity, EmailAuthFragment>()

    @Test
    fun shouldEmailValidation() {
        performLogin {
            input(text)
        }.checkResult {
            check()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<Any>> =
            listOf<Array<Any>>(
                arrayOf("@", { onLoginBtn().check(matches(not(isDisplayed()))) }),
                arrayOf("blah", { onLoginBtn().check(matches(not(isDisplayed()))) }),
                arrayOf("blah@", { onLoginBtn().check(matches(not(isDisplayed()))) }),
                arrayOf("blah@blah", { onLoginBtn().check(matches(isDisplayed())) }),
                arrayOf("blah@blah.ru", { onLoginBtn().check(matches(isDisplayed())) })
            )
    }
}
