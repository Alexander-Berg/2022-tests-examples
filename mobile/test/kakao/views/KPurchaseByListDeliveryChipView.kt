package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.TextViewAssertions
import org.hamcrest.Matcher
import ru.beru.android.R

open class KPurchaseByListDeliveryChipView(
    private val function: ViewBuilder.() -> Unit
) : KBaseView<KPurchaseByListDeliveryChipView>(function), TextViewAssertions {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this({
        isDescendantOfA { withMatcher(parent) }
        function(this)
    })

    val textView = KTextView {
        isDescendantOfA(this@KPurchaseByListDeliveryChipView.function)
        withId(R.id.fastFilterText)
    }

    override fun hasText(text: String) {
        textView.hasText(text)
    }

    fun isSelected(isSelected: Boolean) {
        if (isSelected) {
            this.hasBackground(R.drawable.bg_fast_filter_checked)
        } else {
            this.hasBackground(R.drawable.bg_fast_filter_white_color)
        }
    }

    fun clickChip() {
        textView.click()
    }
}