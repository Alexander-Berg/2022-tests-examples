package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.feature.price.ui.BasePricesViewOld
import ru.yandex.market.test.kakao.assertions.PriceViewAssertions

class KPriceView(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : KBaseCompoundView<KPriceView>(
    BasePricesViewOld::class,
    parent,
    function
), PriceViewAssertions {

    override val currentPrice = KTextView {
        isDescendantOfA { withMatcher(this@KPriceView.parentMatcher) }
        withId(R.id.pricesPriceView)
    }
    override val basePrice = KTextView {
        isDescendantOfA { withMatcher(this@KPriceView.parentMatcher) }
        withId(R.id.pricesBasePriceView)
    }
    override val smartCoins = KView {
        isDescendantOfA { withMatcher(this@KPriceView.parentMatcher) }
        withId(R.id.pricesSmartCoinsView)
    }
    override val discountBadge = KView {
        isDescendantOfA { withMatcher(this@KPriceView.parentMatcher) }
        withId(R.id.saleBadgeContainer)
    }

    override val bnplDescriptionText = KTextView {
        isDescendantOfA { withMatcher(this@KPriceView.parentMatcher) }
        withId(R.id.pricesPriceBnplTextView)
    }

    override val subscriptionSuffixText = KTextView {
        isDescendantOfA { withMatcher(this@KPriceView.parentMatcher) }
        withId(R.id.subscriptionSuffixTextView)
    }
}