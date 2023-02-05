package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import ru.yandex.market.feature.starrating.StarsLayout
import ru.yandex.market.test.util.assertThat

class KStarsLayout(function: ViewBuilder.() -> Unit) : KBaseView<KStarsLayout>(function) {

    fun clickOnStar(position: Int) {
        view.check { view, notFoundException ->
            if (view is StarsLayout) {
                view.getChildAt(position - 1)?.performClick()
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }

    fun checkSelectedStar(position: Int) {
        view.check { view, notFoundException ->
            if (view is StarsLayout) {
                assertThat(position == view.selectedCount) {
                    "Selected star is ${view.selectedCount} but $position expected"
                }
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }
}