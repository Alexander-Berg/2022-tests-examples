package ru.yandex.market.test.kakao.matchers

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.uikit.text.InternalTextView
import androidx.core.view.children

/**
 * Проверяет по [index] индексу у [InternalTextView] текст
 * Применимо к [RecyclerView]
 * В местах где нужен этот матчер везде [InternalTextView] из-за
 * этого пока проверка только на [InternalTextView.getText]
 */
class PickupPointInfoItemMatcher(
    private val index: Int,
    private val text: CharSequence,
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("PickupPointInfo checking item in $index index with text - $text")
    }

    override fun matchesSafely(recyclerView: View): Boolean {
        return if (recyclerView is RecyclerView) {
            val viewToCheck = recyclerView.children.toList().getOrNull(index)
            if (viewToCheck is InternalTextView) {
                viewToCheck.text == text
            } else {
                false
            }
        } else {
            false
        }
    }
}
