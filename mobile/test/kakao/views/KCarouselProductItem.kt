package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import org.hamcrest.Matcher
import ru.beru.android.R

class KCarouselProductItem(parent: Matcher<View>) : KRecyclerItem<KCarouselProductItem>(parent) {

    private val wishListButton = KImageView(parent) {
        withId(R.id.wishListButton)
    }

    private val descriptionBlock = KDescriptionSnippetBlock(parent) {
        withId(R.id.descriptionBlock)
    }

    private val photoBlock = KPhotoSnippetBlock(parent) {
        withId(R.id.imageView)
    }

    private val offerBlock = KOfferSnippetBlock(parent) {
        withId(R.id.offerBlock)
    }

    private val cartCounterButton = KCartCounterView(parent) {
        withId(R.id.cartCounterButton)
    }

    fun checkDescriptionVisible() {
        descriptionBlock.isDescriptionVisible()
    }

    fun checkWishLikeVisible() {
        wishListButton.isVisible()
    }

    fun checkCartCounterVisible() {
        cartCounterButton.isVisible()
    }

    fun checkPriceVisible() {
        offerBlock.checkPriceVisible()
    }

    fun checkDiscountVisible() {
        offerBlock.checkDiscountVisible()
    }

    fun checkVisibleRating() {
        descriptionBlock.isRatingVisible()
    }
}
