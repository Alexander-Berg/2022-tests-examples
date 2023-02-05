package ru.yandex.market.test.kakao.matchers

import android.view.View
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.uikit.raiting.RatingBriefView


class RatingVisibleMatcher(private val reviewsCount: Int) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description?) {
        description?.appendText("is displayed on the screen to the user")
    }

    override fun matchesSafely(item: View?): Boolean {
        return (item is RatingBriefView) && item.compoundDrawables.firstOrNull()?.isVisible ?: false
                && item.text?.split(" ")?.firstOrNull()?.equals(reviewsCount.toString()) ?: false
    }
}