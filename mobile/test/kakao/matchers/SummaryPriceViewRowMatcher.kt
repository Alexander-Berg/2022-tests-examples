package ru.yandex.market.test.kakao.matchers

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.gridlayout.widget.GridLayout
import androidx.test.espresso.matcher.ViewMatchers
import io.github.kakaocup.kakao.common.matchers.DrawableMatcher
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.children
import ru.yandex.market.checkout.summary.SummaryPriceView

class SummaryPriceViewRowMatcher(
    private val title: String,
    private val value: String,
    @ColorRes private val valueColor: Int,
    @DrawableRes private val icon: Int?
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText(
            "SummaryPriceView contains row with" +
                    " title \"$title\"," +
                    " icon \"$icon\"," +
                    " value \"$value\"," +
                    " valueColor \"$valueColor\""
        )

    }

    override fun matchesSafely(item: View?): Boolean {
        return (item as? SummaryPriceView?)?.let {
            return item.children
                .any {
                    when (it) {
                        is GridLayout -> hasRow(it)
                        is LinearLayout -> matchLinearLayoutView(it)
                        else -> false
                    }
                }
        } ?: false
    }

    private fun hasRow(gridLayout: GridLayout): Boolean {
        if (gridLayout.columnCount == SUMMARY_PRICE_VIEW_COLUMNS_COUNT) {
            var childIndex = 0
            for (row in 0..gridLayout.rowCount) {
                val isTitleMatches = matchTitle(gridLayout.getChildAt(childIndex++))
                val isValueMatches = matchValue(gridLayout.getChildAt(childIndex++) as? TextView)
                if (isTitleMatches && isValueMatches) {
                    return true
                }
            }
        }
        return false
    }

    private fun matchTitle(view: View?): Boolean {
        if (view is TextView) {
            val textMatches = ViewMatchers.withText(title).matches(view)
            val iconMatches = icon?.let { TextViewDrawableMatcher(it, Gravity.RIGHT).matches(view) } ?: true
            return textMatches && iconMatches
        } else if (view is ViewGroup) {
            val textMatches = view.children
                .filterIsInstance(TextView::class.java)
                .any { ViewMatchers.withText(title).matches(it) }

            val iconMatches = icon?.let { icon ->
                view.children
                    .filterIsInstance(ImageView::class.java)
                    .any { DrawableMatcher(icon).matches(it) }
            } ?: true

            return textMatches && iconMatches
        }
        return false
    }

    private fun matchValue(textView: TextView?): Boolean {
        val textMatches = ViewMatchers.withText(value).matches(textView)
        val colorMatches = ViewMatchers.hasTextColor(valueColor).matches(textView)
        return textMatches && colorMatches
    }

    private fun matchLinearLayoutView(linearLayout: LinearLayout): Boolean {
        val isTitleMatches = matchTitle(linearLayout.getChildAt(LINEAR_CONTAINER_TITLE_POSITION))
        val isValueMatches = matchValue(linearLayout.getChildAt(LINEAR_CONTAINER_VALUE_POSITION) as? TextView)
        if (isTitleMatches && isValueMatches) {
            return true
        }
        return false
    }

    companion object {
        private const val SUMMARY_PRICE_VIEW_COLUMNS_COUNT = 2
        private const val LINEAR_CONTAINER_TITLE_POSITION = 0
        private const val LINEAR_CONTAINER_VALUE_POSITION = 2

    }
}