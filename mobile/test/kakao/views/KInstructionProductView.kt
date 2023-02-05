package ru.yandex.market.test.kakao.views

import android.view.View
import android.widget.LinearLayout
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import ru.beru.android.R
import ru.yandex.market.test.kakao.matchers.InstructionTrimmedTextStateMatcher
import ru.yandex.market.uikit.text.TrimmedTextView
import ru.yandex.market.utils.children

open class KInstructionProductView(
    private val function: ViewBuilder.() -> Unit
) : KBaseView<KInstructionProductView>(function) {

    private val characteristicsTitleTextParametersView = KView {
        isDescendantOfA(this@KInstructionProductView.function)
        withId(R.id.characteristicsTitleTextParametersView)
    }

    fun checkInstructionState(instructionIndex: Int, isCollapsed: Boolean) {
        characteristicsTitleTextParametersView.matches {
            withMatcher(
                InstructionTrimmedTextStateMatcher(
                    instructionIndex = instructionIndex,
                    isCollapsed = isCollapsed
                )
            )
        }
    }

    fun clickOnInstructionTrimmedText(instructionIndex: Int) {
        characteristicsTitleTextParametersView.act {
            object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return Matchers.allOf(
                        ViewMatchers.isAssignableFrom(LinearLayout::class.java),
                        ViewMatchers.isDisplayed()
                    )
                }

                override fun getDescription(): String {
                    return "perform a click on instruction trimmed text"
                }

                override fun perform(uiController: UiController?, view: View?) {
                    val children = (view as LinearLayout).children.toList()
                    val trimmedTextView =
                        (children[instructionIndex] as LinearLayout).children.last() as TrimmedTextView
                    trimmedTextView.performClick()
                }
            }
        }
    }
}