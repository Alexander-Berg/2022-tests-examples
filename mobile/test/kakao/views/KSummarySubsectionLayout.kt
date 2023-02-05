package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KSummarySubsectionLayout : KBaseView<KSummarySubsectionLayout> {

    private val function: ViewBuilder.() -> Unit

    val title
        get() = KTextView {
            withParent(this@KSummarySubsectionLayout.function)
            withId(R.id.title)
        }

    val content
        get() = KTextView {
            withParent(this@KSummarySubsectionLayout.function)
            withId(R.id.content)
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
        title {
            isVisible()
            hasText(titleText)
        }
    }

    fun hasContent(contentText: String) {
        content {
            isVisible()
            hasText(contentText)
        }
    }

}