package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.ScrollView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class ScrollPositionAtEndMatcher : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("scrollY at end")
    }

    override fun matchesSafely(item: View?): Boolean =
        item is ScrollView && item.scrollY == item.maxScrollAmount
}
