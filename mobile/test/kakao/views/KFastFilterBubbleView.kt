package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.image.KImageView
import ru.beru.android.R

class KFastFilterBubbleView(findParentViewAction: ViewBuilder.() -> Unit) :
    KBaseView<KFastFilterBubbleView>(findParentViewAction) {

    private val removeFilterButton = KImageView {
        withId(R.id.removeFilterButton)
    }

    fun isApplied() {
        removeFilterButton.isVisible()
    }

}
