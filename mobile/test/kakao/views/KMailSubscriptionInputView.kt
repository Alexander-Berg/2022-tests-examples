package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KMailSubscriptionInputView(private val function: ViewBuilder.() -> Unit) :
    KBaseView<KMailSubscriptionInputView>(function) {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this({
        isDescendantOfA { withMatcher(parent) }
        function(this)
    })

    val inputView = KEditText {
        isDescendantOfA(this@KMailSubscriptionInputView.function)
        withId(R.id.inputView)
    }

    val progressButton = KProgressButton {
        isDescendantOfA(this@KMailSubscriptionInputView.function)
        withId(R.id.tickButton)
    }

    val errorView = KTextView {
        isDescendantOfA(this@KMailSubscriptionInputView.function)
        withId(R.id.errorView)
    }
}