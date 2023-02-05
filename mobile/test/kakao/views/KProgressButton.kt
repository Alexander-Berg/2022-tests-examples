package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.progress.KProgressBar
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.TextViewAssertions

import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.util.isNotVisible

open class KProgressButton(
    private val function: ViewBuilder.() -> Unit
) : KBaseView<KProgressButton>(function), TextViewAssertions {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this({
        isDescendantOfA { withMatcher(parent) }
        function(this)
    })

    val textView = KTextView {
        isDescendantOfA(this@KProgressButton.function)
        withId(R.id.button_text)
    }

    val progressView = KProgressBar {
        isDescendantOfA(this@KProgressButton.function)
        withId(R.id.progress_bar)
    }

    fun isProgressVisible(isVisible: Boolean) {
        if (isVisible) {
            progressView.isVisible()
        } else {
            progressView.isNotVisible()
        }
    }

    override fun hasText(text: String) {
        textView.hasText(text)
    }
}