package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.viewpager.widget.ViewPager
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class PagerItemsCountMatcher(private val itemsCount: Int) : TypeSafeMatcher<View>() {

    override fun describeTo(desc: Description) {
        desc.appendText("pager items count $itemsCount")
    }

    public override fun matchesSafely(view: View): Boolean {
        return view is ViewPager &&
                view.adapter?.count ?: 0 == itemsCount
    }

}