package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.cms.view.CarouselWidgetView
import ru.yandex.market.mocks.state.PromoMockState.OfferPromo
import ru.yandex.market.screen.CmsScreen.ScrollBoxStoriesWidget
import ru.yandex.market.screen.CmsScreen.ScrollBoxVendorsWidget
import ru.yandex.market.test.kakao.util.findFirstItemPositionOrThrow

class KCarouselWidgetView : KBaseCompoundView<KCarouselWidgetView> {

    constructor(function: ViewBuilder.() -> Unit) : super(CarouselWidgetView::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(CarouselWidgetView::class, parent, function)

    val title = KTextView(parentMatcher) {
        withId(R.id.carouselWidgetTitle)
    }

    val showMoreButton = KTextView(parentMatcher) {
        withId(R.id.showMoreSnippetText)
        withParent { withId(R.id.showMoreSnippetContainer) }
    }

    val recyclerView = KRecyclerView(
        builder = {
            isDescendantOfA { withMatcher(this@KCarouselWidgetView.parentMatcher) }
            withId(R.id.carouselWidgetItems)
        },
        itemTypeBuilder = {
            itemType(::ScrollBoxProductItem)
            itemType(::ScrollBoxActualOrderItem)
            itemType(::ScrollBoxLiveStreamItem)
            itemType(::ScrollBoxStoriesWidget)
            itemType(::ScrollBoxVendorsWidget)
            itemType(::KCarouselProductItem)
        }
    )

    fun productChildAt(childPosition: Int, block: ScrollBoxProductItem.() -> Unit) {
        recyclerView.childAt(childPosition, block)
    }

    fun findFirstProductItemPositionWithPromoOrThrow(promo: OfferPromo): Int {
        return recyclerView.findFirstItemPositionOrThrow<ScrollBoxProductItem> {
            checkIsOfferPromoShown(promo)
        }
    }
}
