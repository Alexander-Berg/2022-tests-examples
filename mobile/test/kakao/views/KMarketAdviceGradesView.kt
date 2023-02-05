package ru.yandex.market.test.kakao.views

import androidx.core.content.res.ResourcesCompat
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.order.feedback.questions.view.MarketAdviceGradesView
import ru.yandex.market.test.util.assertThat

class KMarketAdviceGradesView(function: ViewBuilder.() -> Unit) : KBaseView<KMarketAdviceGradesView>(function) {

    fun clickOnRateButton(position: Int) {
        view.check { view, notFoundException ->
            if (view is MarketAdviceGradesView) {
                view.getChildAt(position)?.performClick()
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }

    fun checkSelectedGrade(position: Int) {
        view.check { view, notFoundException ->
            if (view is MarketAdviceGradesView) {
                assertThat(
                    view.getChildAt(position)?.background ==
                            ResourcesCompat.getDrawable(
                                view.resources,
                                R.drawable.bg_button_outlined_small,
                                view.context.theme
                            )
                ) {
                    "Other grade was selected or it has unknown background"
                }
            } else {
                notFoundException.let { throw AssertionError(it) }
            }
        }
    }
}