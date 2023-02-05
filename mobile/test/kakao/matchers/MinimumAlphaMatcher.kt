package ru.yandex.market.test.kakao.matchers

import android.view.View
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class MinimumAlphaMatcher(private val minimumAlpha: Float) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("has minimum alpha: ").appendValue(minimumAlpha)
    }

    override fun matchesSafely(item: View): Boolean {
        return item.alpha >= minimumAlpha
    }
}