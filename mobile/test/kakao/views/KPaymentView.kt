package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.yandex.market.uikit.text.InternalTextView

class KPaymentView : KBaseView<KPaymentView> {

    private val function: ViewBuilder.() -> Unit

    val title
        get() = KTextView {
            withIndex(0) {
                withParent(this@KPaymentView.function)
                isInstanceOf(InternalTextView::class.java)
            }
        }

    val subtitle
        get() = KTextView {
            withIndex(1) {
                withParent(this@KPaymentView.function)
                isInstanceOf(InternalTextView::class.java)
            }
        }

    constructor(function: ViewBuilder.() -> Unit) : super(function) {
        this.function = function
    }

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function) {
        this.function = {
            isDescendantOfA { withMatcher(parent) }
            function()
        }
    }

    fun hasTitle(titleText: String) {
        title.isVisible()
        title.hasText(titleText)
    }

    fun hasSubtitle(subtitleText: String) {
        subtitle.isVisible()
        subtitle.hasText(subtitleText)
    }

}