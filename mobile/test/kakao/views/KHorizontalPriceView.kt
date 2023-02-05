package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.feature.price.ui.BasePricesViewOld
import ru.yandex.market.test.kakao.assertions.PriceViewAssertions

class KHorizontalPriceView : KBaseCompoundView<KHorizontalPriceView>, PriceViewAssertions {

    constructor(
        function: ViewBuilder.() -> Unit
    ) : super(BasePricesViewOld::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(BasePricesViewOld::class, parent, function)

    override val currentPrice = KTextView {
        isDescendantOfA { withMatcher(this@KHorizontalPriceView.parentMatcher) }
        withId(R.id.actualPriceView)
    }

    override val basePrice = KTextView {
        isDescendantOfA { withMatcher(this@KHorizontalPriceView.parentMatcher) }
        withId(R.id.basePriceView)
    }

    override val discountBadge = KView {
        isDescendantOfA { withMatcher(this@KHorizontalPriceView.parentMatcher) }
        withId(R.id.saleBadgeContainer)
    }

    override val smartCoins: KView? = null


    override val bnplDescriptionText = KTextView {
        isDescendantOfA { withMatcher(this@KHorizontalPriceView.parentMatcher) }
        withId(R.id.pricesPriceBnplTextView)
    }

    override val subscriptionSuffixText: KTextView? = null
}