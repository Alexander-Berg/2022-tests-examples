package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.test.kakao.matchers.BnplTableViewMatcher

class KBnplWidget(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : KBaseView<KBnplWidget>(parent, function) {

    private val bnplInitSumText = KTextView(parent) {
        isDescendantOfA(function)
        withId(R.id.bnplBlockInitSum)
    }

    private val bnplMonthSumText = KTextView(parent) {
        isDescendantOfA(function)
        withId(R.id.bnplBlockMonthSum)
    }

    private val bnplTableView = KView(parent) {
        isDescendantOfA(function)
        withId(R.id.bnplPaymentsTableView)
    }

    private val moreInfoTextBnplButton = KTextView(parent) {
        isDescendantOfA(function)
        withId(R.id.moreInfoTextView)
    }

    private val orderButton = KButton(parent) {
        isDescendantOfA(function)
        withId(R.id.proceedBnpl)
    }

    fun checkBnplSumText(initSumText: String, monthSumText: String) {
        bnplInitSumText.hasText(initSumText)
        bnplMonthSumText.hasText(monthSumText)
    }

    fun checkMoreInfoTextBnpl() {
        moreInfoTextBnplButton {
            isVisible()
            hasText("Подробнее")
        }
    }

    fun checkBnplTableViewHasTable(paymentsCount: Int) {
        bnplTableView {
            isVisible()
            matches {
                withMatcher(
                    BnplTableViewMatcher(
                        R.id.bnplPaymentsTableView,
                        R.id.paymentsContainer,
                        R.id.paymentPeriodIcon,
                        paymentsCount
                    )
                )
            }
        }
    }

    fun checkOrderButton() {
        orderButton {
            isVisible()
            hasText("Оформить")
        }
    }

    fun clickOrderButton() {
        orderButton.click()
    }
}
