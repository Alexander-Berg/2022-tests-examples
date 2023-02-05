package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class PositionMatcher(private val position: Int) : TypeSafeMatcher<View>() {

    override fun describeTo(desc: Description) {
        desc.appendText("view at position $position")
    }

    public override fun matchesSafely(view: View): Boolean {
        val parent = view.parent
        return when (parent) {
            is ViewGroup -> parent.childCount > position && parent.getChildAt(position) == view
            else -> false
        }
    }
}
