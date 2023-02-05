package ru.yandex.market.test.kakao.views

import android.view.View
import android.widget.Checkable
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher

class KCheckable : KBaseView<KCheckable> {

    constructor(function: ViewBuilder.() -> Unit) : super(function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    constructor(parent: DataInteraction, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun checkState(isChecked: Boolean) {
        view.check(
            ViewAssertion { view, noViewFoundException ->
                if (view is Checkable) {
                    if (view.isChecked != isChecked) {
                        throw AssertionError("The Checkable expected to be ${formatStateName(isChecked)} but it is not")
                    }
                } else if (view != null) {
                    throw AssertionError("The view is expected to be Checkable but is ${view::class}")
                } else {
                    throw noViewFoundException
                }
            }
        )
    }

    private fun formatStateName(isChecked: Boolean): String = if (isChecked) "checked" else "unchecked"
}