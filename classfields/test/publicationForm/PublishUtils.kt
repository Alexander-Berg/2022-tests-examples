package com.yandex.mobile.realty.test.publicationForm

import com.yandex.mobile.realty.core.assertion.NamedViewAssertion.Companion.matches
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.Robot
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.allOf
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isCompletelyDisplayed
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.isEnabled
import com.yandex.mobile.realty.core.viewMatchers.NamedViewMatcher.Companion.withText

/**
 * @author andrey-bgm on 12/01/2021.
 */
fun containsInputFieldWithValue(
    robot: Robot<*>,
    content: NamedViewMatcher,
    inputField: NamedViewMatcher,
    inputFieldValue: NamedViewMatcher,
    value: String?
) {
    robot.scrollRecyclerViewTo(content, inputField)
    onView(inputField).check(matches(isCompletelyDisplayed()))
    val textMatcher = if (value.isNullOrEmpty()) hasEmptyValue() else withText(value)
    onView(inputFieldValue).check(matches(allOf(textMatcher, isEnabled())))
}

fun hasEmptyValue(): NamedViewMatcher {
    return NamedViewMatcher("не заполнено", withText(""))
}
