package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.SwitchCompat
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class SwitchMatcher(
    @IdRes private val id: Int,
    private val isChecked: Boolean
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("SwitchCompat with id $id isChecked: $isChecked")
    }

    override fun matchesSafely(switchCompat: View): Boolean {
        return (switchCompat is SwitchCompat) && switchCompat.findViewById<SwitchCompat>(id)?.isChecked == isChecked
    }
}