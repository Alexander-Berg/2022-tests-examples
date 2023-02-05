package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher

class KNestedScrollView(function: ViewBuilder.() -> Unit) : KBaseView<NestedScrollView>(function) {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
            this({
                isDescendantOfA { withMatcher(parent) }
                function(this)
            })

    fun scrollForward() {
        val scrollable = UiScrollable(
            UiSelector().scrollable(true)
        )
        scrollable.scrollForward()
    }

    fun scrollBackward() {
        val scrollable = UiScrollable(
            UiSelector().scrollable(true)
        )
        scrollable.scrollBackward()
    }
}