package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatRadioButton
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class RadioButtonMatcher(
    @IdRes private val id: Int,
    private val isSelected: Boolean
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("AppCompatRadioButton with id $id isSelected: $isSelected")
    }

    override fun matchesSafely(radioButton: View): Boolean {
        return (radioButton is AppCompatRadioButton) && radioButton.findViewById<AppCompatRadioButton>(
            id
        )?.isSelected == isSelected
    }
}