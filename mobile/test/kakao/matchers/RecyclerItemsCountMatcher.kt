package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class RecyclerItemsCountMatcher(private val itemsCount: Int) : TypeSafeMatcher<View>() {

    override fun describeTo(desc: Description) {
        desc.appendText("recycler view items count $itemsCount")
    }

    public override fun matchesSafely(view: View): Boolean {
        return view is RecyclerView &&
                view.adapter?.itemCount ?: 0 == itemsCount
    }

}