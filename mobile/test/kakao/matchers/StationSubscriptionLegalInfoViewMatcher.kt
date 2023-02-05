package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class StationSubscriptionLegalInfoViewMatcher(
    private val title: String,
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText(
            "legal info contains text $title"
        )
    }

    override fun matchesSafely(item: View?): Boolean {
        return (item as? ViewGroup)?.let {
            (item.getChildAt(0) as? TextView)?.text.toString() == title
        } ?: false
    }
}