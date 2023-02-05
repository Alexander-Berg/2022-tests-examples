package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher
import ru.yandex.market.uikit.view.CheckableFrameLayout

class KCheckableFrameLayout : KBaseView<KCheckableFrameLayout> {

    constructor(function: ViewBuilder.() -> Unit) : super(function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    constructor(parent: DataInteraction, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun isChecked() {
        view.check(
            ViewAssertion { view, noViewFoundException ->
                if (view is CheckableFrameLayout) {
                    if (!view.isChecked) throw AssertionError("The CheckableFrameLayout expected to be checked but it is not checked")
                } else if (view != null) {
                    throw AssertionError("The view is expected to be CheckableFrameLayout but is ${view::class}")
                } else {
                    throw noViewFoundException
                }
            }
        )
    }
}