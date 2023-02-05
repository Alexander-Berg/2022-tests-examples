package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatCheckBox
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class CheckBoxMatcher(
    @IdRes private val id: Int,
    private val isChecked: Boolean
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("AppCompatCheckBox with id $id isChecked: $isChecked")
    }

    override fun matchesSafely(checkBox: View): Boolean {
        return (checkBox is AppCompatCheckBox) && checkBox.findViewById<AppCompatCheckBox>(
            id
        )?.isChecked == isChecked
    }
}