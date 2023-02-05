package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.matchers.InstallmentTermPickerViewTermTextMatcher
import ru.yandex.market.test.util.findRecyclerAndScrollTo

class KTinkoffInstallmentsWidget(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KTinkoffInstallmentsWidget>(parent, function) {

    private val installmentsTitleTextView = KTextView {
        isDescendantOfA { function() }
        withId(R.id.installmentsTitleTextView)
    }

    private val installmentsOrderButton = KButton {
        isDescendantOfA { function() }
        withId(R.id.installmentsOrderButton)
    }

    private val pickerView = KInstallmentsTermPickerView {
        isDescendantOfA { function() }
        withId(R.id.installmentsTermView)
    }

    fun checkInstallmentsTitleText(text: String) {
        installmentsTitleTextView {
            findRecyclerAndScrollTo()
            isVisible()
            containsText(text)
        }
    }

    fun checkInstallmentsOrderButtonIsVisible() {
        installmentsOrderButton.findRecyclerAndScrollTo()
        installmentsOrderButton.isVisible()
    }

    fun checkInstallmentsTermViewIsVisible() {
        pickerView {
            findRecyclerAndScrollTo()
            isVisible()
            isMonthlyPaymentVisible()
            isTermSelectorVisible()
        }
    }

    fun checkMonthlyPaymentValue(text: String) {
        pickerView {
            isVisible()
            hasMonthlyPayment(text)
        }
    }

    fun scrollSelectorUp() {
        pickerView.findRecyclerAndScrollTo()
        pickerView.scrollUp()
    }

    fun scrollSelectorDown() {
        pickerView.findRecyclerAndScrollTo()
        pickerView.scrollDown()
    }

    fun clickConfirmButton() {
        installmentsOrderButton.click()
    }

    fun checkTermValue(text: String) {
        pickerView {
            matches { withMatcher(InstallmentTermPickerViewTermTextMatcher(text)) }
        }
    }

    fun clickTermSelector() {
        pickerView.clickTermSelector()
    }
}