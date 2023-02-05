package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.mocks.state.PromoMockState.OfferPromo

class CmsSaleItemView(function: ViewBuilder.() -> Unit) : KBaseView<CmsSaleItemView>(function) {

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : this({
        isDescendantOfA { withMatcher(parent) }
        function(this)
    })

    private val thisMatcher = ViewBuilder().apply(function).getViewMatcher()

    val offerPromoIconView = KOfferPromoIconView(thisMatcher) {
        withId(R.id.offerPromoIconView)
    }

    fun checkIsOfferPromoShown(promo: OfferPromo?) {
        offerPromoIconView.checkIsPromoShown(promo)
    }
}