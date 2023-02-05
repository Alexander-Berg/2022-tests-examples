package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class RecyclerViewColumnCountMatcher(private val columnCount: Int) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText("Recycler view show items in $columnCount columns")
    }

    override fun matchesSafely(item: View?): Boolean {
        return (item as? RecyclerView)?.layoutManager?.let {
            return (it as? GridLayoutManager)?.spanCount == columnCount
        } ?: false
    }
}