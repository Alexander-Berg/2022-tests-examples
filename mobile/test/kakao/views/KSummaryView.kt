package ru.yandex.market.test.kakao.views

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.espresso.DataInteraction
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.checkout.summary.SummaryBlockView
import ru.yandex.market.test.util.assertThat
import ru.yandex.market.test.util.createErrorMessage
import ru.yandex.market.test.util.withView

class KSummaryView : KBaseView<KSummaryView> {
    constructor(function: ViewBuilder.() -> Unit) : super(function)
    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)
    constructor(parent: DataInteraction, function: ViewBuilder.() -> Unit) : super(parent, function)

    private var summaryPriceTextView = KTextView {
        withId(R.id.summaryPriceTextView)
    }

    fun checkSummaryPrice(price: String) {
        summaryPriceTextView.isVisible()
        summaryPriceTextView.hasText(price)
    }

    fun checkRegularItem(position: Int, leftText: String, rightText: String) {
        withView<SummaryBlockView> { view ->
            val itemsContainer = view.findViewById<LinearLayout>(R.id.regularPriceItemsContainer)
            val item = itemsContainer.getChildAt(position)
            val leftTextView = item.findViewById<TextView>(R.id.leftTextView)
            val rightTextView = item.findViewById<TextView>(R.id.rightTextView)
            val isTextAsExpected = leftTextView.text == leftText && rightTextView.text == rightText
            assertThat(isTextAsExpected) {
                createErrorMessage(
                    element = "SummaryBlockView",
                    expected = "leftText = $leftText and rightText = $rightText",
                    actual = "leftText = ${leftTextView.text} and rightText = ${rightTextView.text} "
                )
            }
        }
    }

}
