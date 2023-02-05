package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.isVisible

class ToolbarItemMatcher(
    @IdRes private val id: Int
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("Menu item with id $id displayed")
    }

    override fun matchesSafely(toolBar: View): Boolean {
        return (toolBar is Toolbar) && toolBar.findViewById<View>(id)?.isVisible ?: false
    }
}