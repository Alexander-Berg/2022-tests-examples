package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import ru.beru.android.R

open class KDeliveryInformationBottomView(function: ViewBuilder.() -> Unit) :
    KBaseView<KDeliveryInformationBottomView>(function) {

    private val infoIcon = KImageView {
        withId(R.id.infoIv)
        isDescendantOfA { withId(R.id.content) }
    }

    fun clickOnInfoIcon() {
        infoIcon.click()
    }

}
