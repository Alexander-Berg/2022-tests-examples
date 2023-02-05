package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.children
import ru.yandex.market.clean.presentation.feature.sku.ReasonsToBuyView
import ru.yandex.market.clean.presentation.view.NameplateView

class ReasonsToBuyViewTextMatcher(
    @IdRes val id: Int,
    val text: String
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("ReasonToBuyView contains textView with text \"$text\"")
    }

    override fun matchesSafely(view: View): Boolean {
        return (view is ReasonsToBuyView) && view.children.any {
            it is NameplateView && it.findViewById<TextView>(id).text.toString().equals(text)
        }
    }
}