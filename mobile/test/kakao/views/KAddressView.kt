package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R
import ru.yandex.market.uikit.text.InternalTextView

open class KAddressView(private val function: ViewBuilder.() -> Unit) : KBaseView<KAddressView>(function) {

    private val pinIcon = KImageView {
        withId(R.id.pinIcon)
    }

    private val selectedDeliveryAddress = KTextView {
        withId(R.id.selectedDeliveryAddress)
        isInstanceOf(InternalTextView::class.java)
    }

    fun checkSelectedDeliveryAddress(text: String) {
        selectedDeliveryAddress.hasText(text)
    }

}