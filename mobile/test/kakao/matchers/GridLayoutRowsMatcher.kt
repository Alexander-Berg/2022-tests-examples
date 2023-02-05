package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.gridlayout.widget.GridLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.util.extensions.rows

class GridLayoutRowsMatcher(
    private val matcher: Matcher<Iterable<List<View>>>
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        matcher.describeTo(description)
    }

    override fun matchesSafely(item: View?): Boolean {
        if (item !is GridLayout) {
            return false
        }
        return matcher.matches(item.rows.toList())
    }
}