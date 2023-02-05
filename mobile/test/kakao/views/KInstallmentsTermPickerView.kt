package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KSwipeView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import ru.yandex.market.test.kakao.matchers.InstallmentTermPickerViewTermTextMatcher

class KInstallmentsTermPickerView(function: ViewBuilder.() -> Unit) : KBaseView<KInstallmentsTermPickerView>(function) {

    private val monthlyPayment = KTextView {
        isDescendantOfA { function() }
        withId(R.id.monthlyPayment)
    }

    private val termSelector = KSwipeView {
        isDescendantOfA { function() }
        withId(R.id.internalNumberPicker)
    }

    fun isMonthlyPaymentVisible() {
        monthlyPayment.isVisible()
    }

    fun isTermSelectorVisible() {
        termSelector.isVisible()
    }

    fun clickTermSelector() {
        termSelector.click()
    }

    fun hasSelectedTerm(text: String) {
        matches { withMatcher(InstallmentTermPickerViewTermTextMatcher(text)) }
    }

    fun hasMonthlyPayment(text: String) {
        monthlyPayment.hasText(text)
    }

    fun scrollUp() {
        termSelector.swipeUp()
    }

    fun scrollDown() {
        termSelector.swipeDown()
    }
}