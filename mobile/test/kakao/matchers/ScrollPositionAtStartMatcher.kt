package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.ScrollView
import androidx.core.view.ScrollingView
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class ScrollPositionAtStartMatcher : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("scrollY = 0")
    }

    override fun matchesSafely(item: View?): Boolean = when (item) {
        is ScrollView, is RecyclerView -> item.scrollY == 0
        is ScrollingView -> item.computeVerticalScrollOffset() == 0
        else -> false
    }
}
