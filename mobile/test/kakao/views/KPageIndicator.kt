package ru.yandex.market.test.kakao.views

import androidx.test.espresso.ViewAssertion
import com.shuhart.bubblepagerindicator.BubblePageIndicator
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView

class KPageIndicator(function: ViewBuilder.() -> Unit) : KBaseView<KPageIndicator>(function) {
    fun checkDisplayedPageIndex(index: Int) {
        view.check(ViewAssertion { view, noViewFoundException ->
            if (view is BubblePageIndicator) {
                if (index != view.currentPage) {
                    throw AssertionError("Wrong page, expected $index got ${view.currentPage} ")
                }
            } else {
                noViewFoundException?.let { throw AssertionError(it) }
            }
        })
    }
}
