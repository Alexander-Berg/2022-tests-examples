package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.util.getString

class KTinkoffCreditWidget(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KTinkoffCreditWidget>(parent, function) {

    private val creditTitleTextView = KTextView(parent) {
        isDescendantOfA { function() }
        withId(R.id.cartTinkoffCreditBlockTitle)
    }

    private val creditPriceTextView = KTextView(parent) {
        isDescendantOfA { function() }
        withId(R.id.cartTinkoffCreditBlockMonthPayment)
    }


    private val creditOrderButton = KButton(parent) {
        isDescendantOfA { function() }
        withId(R.id.proceedCredit)
    }

    fun checkTitle() {
        creditTitleTextView {
            isVisible()
            hasText("Кредит от Тинькофф")
        }
    }

    fun checkPriceText(text: String) {
        creditPriceTextView {
            isVisible()
            hasText(getString(R.string.credit_price, text))
        }
    }

    fun checkOrderButton() {
        creditOrderButton.isVisible()
    }

    fun clickOrderButton() {
        creditOrderButton.click()
    }
}