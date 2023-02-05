package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.LinearLayout
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.feature.productinstruction.ui.CharacteristicsTitleTextParametersView
import ru.yandex.market.uikit.text.TrimmedTextView
import ru.yandex.market.utils.children

class InstructionTrimmedTextStateMatcher(
    private val instructionIndex: Int,
    private val isCollapsed: Boolean
) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("check instructions trimmed text")
    }

    override fun matchesSafely(view: View?): Boolean {
        return (view as? CharacteristicsTitleTextParametersView?)?.let {
            isInstructionCollapsed(it)
        } ?: false
    }

    private fun isInstructionCollapsed(view: CharacteristicsTitleTextParametersView): Boolean {
        val instruction = view.children.toList()[instructionIndex] as LinearLayout
        val instructionTrimmedTextView = instruction.children.last() as TrimmedTextView
        return if (isCollapsed) {
            instructionTrimmedTextView.isCollapsed
        } else {
            instructionTrimmedTextView.isExpanded
        }
    }
}