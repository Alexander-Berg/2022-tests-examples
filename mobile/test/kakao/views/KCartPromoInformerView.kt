package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.cart.vo.InformerInfo
import ru.yandex.market.clean.presentation.view.CartPromoInformerView
import ru.yandex.market.test.kakao.util.isVisible

class KCartPromoInformerView(
    parent: Matcher<View>,
    function: ViewBuilder.() -> Unit
) : KBaseCompoundView<KCartPromoInformerView>(
    CartPromoInformerView::class,
    parent,
    function
) {

    private val informerFstTextView = KTextView(parentMatcher) {
        withId(R.id.informerFstTextView)
    }

    private val supplierTextView = KTextView(parentMatcher) {
        withId(R.id.supplierTextView)
    }

    private val prefixTextView = KTextView(parentMatcher) {
        withId(R.id.supplierPrefix)
    }

    fun checkNoInformer() {
        informerFstTextView.isNotDisplayed()
    }

    fun checkInformer(informerInfo: InformerInfo) {
        informerFstTextView.isDisplayed()
        informerFstTextView.containsText(informerInfo.informerName)
    }

    fun checkSupplier(isVisible: Boolean) {
        supplierTextView.isVisible(isVisible)
    }

    fun checkResaleVisible(isVisible: Boolean) {
        prefixTextView.isVisible(isVisible)
    }

    fun checkResaleText(text: String) {
        prefixTextView.hasText(text)
    }
}
