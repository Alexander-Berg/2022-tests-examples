package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.mocks.state.PromoMockState.OfferPromo
import ru.yandex.market.test.kakao.util.isVisible
import ru.yandex.market.test.util.scrollToView

class ScrollBoxProductItem(parent: Matcher<View>) : KRecyclerItem<ScrollBoxProductItem>(parent) {
    val image = KImageView(parent) {
        withId(R.id.imageView)
        withParent { withId(R.id.contentContainer) }
    }
    val title = KTextView(parent) { withId(R.id.titleView) }
    val cartButton = KButton(parent) { withId(R.id.cartCounterButton) }
    val offerPromoIconView = KOfferPromoIconView(parent) { withId(R.id.offerPromoIconView) }

    fun clickOnCartButton() {
        cartButton.click()
    }

    fun checkIsOfferPromoIconVisible(isVisible: Boolean) {
        offerPromoIconView.isVisible(isVisible)
    }

    fun checkIsOfferPromoShown(promo: OfferPromo?) {
        offerPromoIconView.checkIsPromoShown(promo)
    }

    fun scrollToCartButton(position: Int, recyclerView: KRecyclerView) {
        recyclerView.scrollToView(position, R.id.cartCounterButton)
    }
}