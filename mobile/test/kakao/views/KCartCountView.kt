package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.util.isVisibleAndEnabled

class KCartCountView(
    parent: Matcher<View>,
    function: ViewBuilder.() -> Unit
) : KBaseView<KCartCountView>(function) {

    private val plusButton = KButton {
        isDescendantOfA { withMatcher(parent) }
        withId(R.id.plus)
    }

    private val minusButton = KButton {
        isDescendantOfA { withMatcher(parent) }
        withId(R.id.minus)
    }

    private val countText = KTextView {
        isDescendantOfA { withMatcher(parent) }
        withId(R.id.countView)
    }

    fun checkCount(count: Int) {
        countText.isDisplayed()
        countText.containsText(count.toString())
    }

    fun add(count: Int = 1) {
        repeat(count) {
            plusButton.click()
        }
    }

    fun minus(count: Int = 1) {
        repeat(count) {
            minusButton.click()
        }
    }

    fun isMinusButtonVisibleAndEnabled(isVisible: Boolean = true, isEnabled: Boolean = true) {
        minusButton.isVisibleAndEnabled(isVisible, isEnabled)
    }

    fun isPlusButtonVisibleAndEnabled(isVisible: Boolean = true, isEnabled: Boolean = true) {
        plusButton.isVisibleAndEnabled(isVisible, isEnabled)
    }
}
