package ru.yandex.market.test.kakao.views

import com.tapadoo.alerter.Alert
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import timber.log.Timber

class KTextInputFormView(private val function: ViewBuilder.() -> Unit) : KBaseView<KTextInputFormView>(function) {

    private val toolbar = KToolbar {
        isDescendantOfA(this@KTextInputFormView.function)
        withId(R.id.toolbar)
    }
    private val commentView = KEditText {
        isDescendantOfA(this@KTextInputFormView.function)
        withId(R.id.commentView)
    }
    private val sendButton = KButton {
        isDescendantOfA(this@KTextInputFormView.function)
        withId(R.id.sendCommentButton)
    }
    private val counterView = KTextView {
        isDescendantOfA(this@KTextInputFormView.function)
        withId(R.id.counterView)
    }
    private val headerTextView = KTextView {
        isDescendantOfA(this@KTextInputFormView.function)
        withId(R.id.headerTextView)
    }

    private val closeButton = KButton {
        isDescendantOfA(this@KTextInputFormView.function)
        isDescendantOfA { withId(R.id.toolbar) }
        withId(R.id.action_close)
    }

    private val alertText = KTextView {
        isDescendantOfA {
            withId(R.id.alertContainer)
        }
        isDescendantOfA {
            isInstanceOf(Alert::class.java)
        }
        withId(R.id.tvText)
    }

    fun clickButton() {
        sendButton.click()
    }

    fun setCommentViewText(newText: String) {
        try {
            commentView.replaceText(newText)
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    fun checkButtonInactive() {
        sendButton.isDisabled()
    }

    fun checkButtonActive() {
        sendButton.isClickable()
    }

    fun checkCounterText(text: String) {
        counterView.hasText(text)
    }

    fun checkButtonVisible() {
        sendButton.isVisible()
    }

    fun checkFocused() {
        commentView.isFocused()
    }

    fun checkCounterColor(colorRes: Int) {
        counterView.hasTextColor(colorRes)
    }

    fun checkText(text: String) {
        commentView.hasText(text)
    }

    fun hasTitle(title: String) {
        toolbar.hasTitle(title)
    }

    fun clickClose() {
        closeButton.click()
    }

    fun checkCloseVisible() {
        closeButton.isVisible()
    }

    fun checkError(text: String) {
        alertText.containsText(text)
    }

    fun checkHeaderText(expectedText: String) {
        headerTextView.hasText(expectedText)
    }
}