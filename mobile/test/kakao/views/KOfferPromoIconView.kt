package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.assertion.ViewAssertions
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.image.KImageView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import ru.beru.android.R
import ru.yandex.market.clean.presentation.vo.OfferPromoVo
import ru.yandex.market.mocks.state.PromoMockState.OfferPromo
import ru.yandex.market.test.kakao.util.isNotVisible
import ru.yandex.market.test.kakao.util.isVisible
import ru.yandex.market.ui.view.OfferPromoIconView
import ru.yandex.market.utils.exhaustive

class KOfferPromoIconView : KBaseCompoundView<KOfferPromoIconView> {

    constructor(
        function: ViewBuilder.() -> Unit
    ) : super(OfferPromoIconView::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(OfferPromoIconView::class, parent, function)

    private val imageView = KImageView(parentMatcher) {
        isInstanceOf(AppCompatImageView::class.java)
    }

    fun checkIsPromoShown(offerPromo: OfferPromo?) {
        when (offerPromo) {
            is OfferPromo.PriceDrop -> {
                isVisible(false)
            }
            is OfferPromo.CheapestAsGift -> {
                isVisible(true)
                checkIsCheapestAsGiftPromoShown(offerPromo.bundleSize)
            }
            is OfferPromo.Gifts -> {
                isVisible(true)
                checkIsGiftsPromoShown()
            }
            null -> {
                isNotVisible()
            }
            is OfferPromo.SecretSale,
            is OfferPromo.DirectDiscount,
            is OfferPromo.FlashSales,
            is OfferPromo.PromoCode,
            is OfferPromo.PromoSpreadDiscountCount,
            is OfferPromo.BlueSet,
            is OfferPromo.Cashback -> {
                // no-op
            }
        }.exhaustive
    }

    private fun checkIsGiftsPromoShown() {
        imageView.hasDrawable(R.drawable.ic_king_badge_gift)
        checkIsPlusSignGone()
    }

    private fun checkIsCheapestAsGiftPromoShown(bundleSize: Int) {
        if (bundleSize == OfferPromoVo.CheapestAsGift.TWO_FOR_THREE) {
            imageView.hasDrawable(R.drawable.ic_king_badge_cheapest_as_gift_3)
            checkIsPlusSignGone()
        } else {
            isVisible(false)
        }
    }

    private fun checkIsPlusSignGone() {
        @Suppress("UNCHECKED_CAST")
        view.check(ViewAssertions.matches(PlusVisibilityMatcher(false) as Matcher<View>))
    }

    private class PlusVisibilityMatcher(val isVisible: Boolean) : TypeSafeMatcher<OfferPromoIconView>() {

        override fun matchesSafely(item: OfferPromoIconView?): Boolean {
            return item?.isPlusVisible == isVisible
        }

        override fun describeTo(description: Description?) {
            description?.apply {
                appendText("Expected plus sign is ")
                appendText(if (isVisible) "visible" else "not visible")
                appendText(", but actual visibility was different.")
            }
        }
    }
}