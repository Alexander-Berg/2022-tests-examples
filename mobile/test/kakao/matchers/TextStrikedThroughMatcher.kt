package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.annotation.ColorRes
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.uikit.text.StrikeThroughTextView

class TextStrikedThroughMatcher(@ColorRes private val strikeThroughColor: Int? = null) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description?) {
        description?.appendText("text is stricken through")
        strikeThroughColor?.run { description?.appendText(" vs color $strikeThroughColor") }
    }

    override fun matchesSafely(item: View?): Boolean {
        return item is StrikeThroughTextView &&
                item.isCrossedOut &&
                strikeThroughColor?.let { color ->
                    item.strikeThroughColor == item.context.getColor(color)
                } ?: true
    }
}