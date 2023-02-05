package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KTextInputLayout
import ru.beru.android.R

class KOrderFeedbackCommentView(private val function: ViewBuilder.() -> Unit) :
    KBaseView<KOrderFeedbackCommentView>(function) {

    private val inputView = KTextInputLayout {
        isDescendantOfA(this@KOrderFeedbackCommentView.function)
        withId(R.id.comment_input_layout)
    }

    fun replaceText(text: String) {
        inputView.edit.replaceText(text)
    }

    fun hasText(text: String) {
        inputView.edit.hasText(text)
    }
}