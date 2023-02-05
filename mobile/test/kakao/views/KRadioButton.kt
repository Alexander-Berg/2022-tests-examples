package ru.yandex.market.test.kakao.views

import android.view.View
import android.widget.RadioButton
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher

class KRadioButton : KBaseView<KRadioButton> {

    constructor(function: ViewBuilder.() -> Unit) : super(function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun checkState(isChecked: Boolean) {
        view.check(
            ViewAssertion { view, notFoundException ->
                if (view is RadioButton) {
                    assert(view.isChecked == isChecked)
                } else {
                    notFoundException.let {
                        throw AssertionError(it)
                    }
                }
            }
        )
    }
}