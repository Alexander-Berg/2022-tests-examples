package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.feature.price.ui.BasePricesView
import ru.yandex.market.test.kakao.assertions.PriceViewAssertions

class KVerticalPriceView : KBaseCompoundView<KVerticalPriceView>, PriceViewAssertions {

    constructor(
        function: ViewBuilder.() -> Unit
    ) : super(BasePricesView::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(BasePricesView::class, parent, function)

    override val currentPrice = KTextView {
        isDescendantOfA { withMatcher(this@KVerticalPriceView.parentMatcher) }
        withId(R.id.actualPriceView)
    }

    override val basePrice = KTextView {
        isDescendantOfA { withMatcher(this@KVerticalPriceView.parentMatcher) }
        withId(R.id.basePriceView)
    }

    override val smartCoins: KView? = null

    override val discountBadge: KView? = null

    override val bnplDescriptionText: KTextView? = null

    override val subscriptionSuffixText: KTextView? = null
}