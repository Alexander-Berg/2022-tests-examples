package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KOfferSnippetBlock(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KOfferSnippetBlock>(parent, function) {

    private val price = KTextView(parent) {
        withId(R.id.priceView)
    }

    private val discount = KTextView(parent) {
        withId(R.id.discountTextView)
    }

    fun checkPriceVisible() {
        price.isVisible()
    }

    fun checkDiscountVisible() {
        price.isVisible()
    }
}
