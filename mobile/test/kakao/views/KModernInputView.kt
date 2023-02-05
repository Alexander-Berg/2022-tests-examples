package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KModernInputView(private val findParentViewAction: ViewBuilder.() -> Unit) :
    KBaseView<KModernInputView>(findParentViewAction) {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this({
        isDescendantOfA { withMatcher(parent) }
        function(this)
    })

    val inputTextView = KEditText {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputInputEditText)
    }

    val iconClearView = KImageView {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputIconClear)
    }

    val iconCorrectTick = KImageView {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputIconCorrectTick)
    }

    val additionalRightImage = KImageView {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputAdditionalRightImage)
    }

    val errorTextView = KTextView {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputErrorTextView)
    }

    val warningTextView = KTextView {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputWarningTextView)
    }

    private val inputLayout = KView {
        isDescendantOfA(this@KModernInputView.findParentViewAction)
        withId(R.id.modernInputInputLayout)
    }

    fun replaceText(text: String) {
        inputTextView.replaceText(text)
    }

    fun hasText(text: String) {
        inputTextView.hasText(text)
    }

    fun hasHint(hint: String) {
        inputTextView.hasHint(hint)
    }

    fun hasBackgroundState(state: BackgroundState) {
        when (state) {
            BackgroundState.NORMAL -> inputLayout.hasBackground(R.drawable.bg_button_outlined_small_gray)
            BackgroundState.ACTIVE -> inputLayout.hasBackground(R.drawable.bg_button_outlined_small)
            BackgroundState.ERROR -> inputLayout.hasBackground(R.drawable.bg_button_outline_small_red)
        }
    }

    enum class BackgroundState { NORMAL, ACTIVE, ERROR }
}
