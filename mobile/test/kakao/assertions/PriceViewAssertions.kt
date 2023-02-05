package ru.yandex.market.test.kakao.assertions

import androidx.annotation.ColorRes
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import ru.yandex.market.test.kakao.matchers.CoinsBadgesDrawableMatcher
import ru.yandex.market.test.kakao.matchers.CompoundDrawableMatcher
import ru.yandex.market.test.kakao.matchers.TextStrikedThroughMatcher
import ru.yandex.market.test.util.formatPrice
import ru.yandex.market.utils.Characters

interface PriceViewAssertions {

    val currentPrice: KTextView
    val basePrice: KTextView
    val smartCoins: KView?
    val discountBadge: KView?
    val bnplDescriptionText: KTextView?
    val subscriptionSuffixText: KTextView?

    fun checkCurrentPrice(price: String, @ColorRes priceColor: Int) {
        currentPrice {
            hasText(price)
            hasTextColor(priceColor)
        }
    }

    fun checkCurrentPriceHasText(price: String) {
        currentPrice {
            containsText(price)
        }
    }

    fun checkBasePrice(price: String, @ColorRes strikeThroughColor: Int) {
        basePrice {
            isVisible()
            hasText(price)
            matches { withMatcher(TextStrikedThroughMatcher(strikeThroughColor)) }
        }
    }

    fun checkDiscountPrice(basePriceValue: Double, currentPriceValue: Double, discountColor: Int) {
        currentPrice.isVisible()
        currentPrice.hasTextColor(discountColor)
        currentPrice.hasText(formatPrice(price = currentPriceValue, spaceChar = Characters.NARROW_NO_BREAK_SPACE))

        basePrice.isVisible()
        basePrice.hasText(formatPrice(price = basePriceValue, spaceChar = Characters.NARROW_NO_BREAK_SPACE))
        basePrice.matches { withMatcher(TextStrikedThroughMatcher()) }
    }

    fun checkHasNoBasePrice() {
        basePrice.doesNotExist()
    }

    fun checkPriceBadgeHide() {
        currentPrice.matches {
            withMatcher(
                CompoundDrawableMatcher(
                    null,
                    CompoundDrawableMatcher.DrawablePosition.LEFT
                )
            )
        }
    }

    fun checkCoinsBadge(coinsColorsRgb: List<String>) {
        smartCoins?.matches { withMatcher(CoinsBadgesDrawableMatcher(coinsColorsRgb)) }
            ?: throw RuntimeException("Cannot find view smartCoins")
    }

    fun checkDiscountBadgeVisible() {
        discountBadge?.isVisible()
    }

    fun checkDiscountBadgeGone() {
        discountBadge?.isGone()
    }

    fun checkIsSubscriptionSuffixVisible() {
        subscriptionSuffixText?.isVisible()
    }
}