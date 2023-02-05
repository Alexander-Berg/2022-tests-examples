package ru.yandex.market.test.kakao.views

import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import ru.yandex.market.uikit.text.TrimmedTextView

class KTrimmedTextView(function: ViewBuilder.() -> Unit) : KBaseView<KTextView>(function)

fun KTrimmedTextView.isTrimmed() {
    view.check(
        ViewAssertion { view, notFoundException ->
            if (view is TrimmedTextView) {
                if (view.isCollapsed) {
                    return@ViewAssertion
                }
                throw AssertionError("View is not trimmed")
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}

fun KTrimmedTextView.isExpanded() {
    view.check(
        ViewAssertion { view, notFoundException ->
            if (view is TrimmedTextView) {
                if (view.isExpanded) {
                    return@ViewAssertion
                }
                throw AssertionError("View is not trimmed")
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}