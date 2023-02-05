package ru.yandex.market.test.kakao.views

import androidx.core.view.isVisible
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import kotlinx.android.synthetic.main.view_product_snippet_price.view.price
import kotlinx.android.synthetic.main.view_product_snippet_price.view.priceBeforeDiscount
import kotlinx.android.synthetic.main.view_product_snippet_price.view.productIsAbsentText
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.stories.views.StorySkuPriceView
import ru.yandex.market.test.util.assertThat
import ru.yandex.market.test.util.createErrorMessage
import ru.yandex.market.test.util.withView

class KStoryPriceView(function: ViewBuilder.() -> Unit) : KBaseView<KStoryPriceView>(function) {

    fun checkMainPrice(text: String) {
        withView<StorySkuPriceView> { view ->
            assertThat(view.price.text == text) {
                createErrorMessage(
                    element = "Main price",
                    expected = text,
                    actual = view.price.text
                )
            }
        }
    }

    fun checkMainPriceVisible(isVisible: Boolean) {
        withView<StorySkuPriceView> { view ->
            assertThat(view.price.isVisible == isVisible) {
                createErrorMessage(
                    element = "Main price",
                    expected = isVisible,
                    actual = view.price.isVisible
                )
            }
        }
    }

    fun checkBeforeDiscountPrice(text: String) {
        withView<StorySkuPriceView> { view ->
            assertThat(view.priceBeforeDiscount.text == text) {
                createErrorMessage(
                    element = "Before discount price",
                    expected = text,
                    actual = view.priceBeforeDiscount.text
                )
            }
        }
    }

    fun checkBeforeDiscountPriceVisible(isVisible: Boolean) {
        withView<StorySkuPriceView> { view ->
            assertThat(view.priceBeforeDiscount.isVisible == isVisible) {
                createErrorMessage(
                    element = "Before discount price visibility",
                    expected = isVisible,
                    actual = view.priceBeforeDiscount.isVisible
                )
            }
        }
    }

    private fun checkOutOfStockText() {
        withView<StorySkuPriceView> { view ->
            assertThat(view.productIsAbsentText.text == view.context.getString(OUT_OF_STOCK_TEXT_RES)) {
                createErrorMessage(
                    element = "Out of stock text",
                    expected = "Нет в наличии",
                    actual = view.productIsAbsentText.text
                )
            }
        }
    }

    fun checkOutOfStockVisible(isVisible: Boolean) {
        withView<StorySkuPriceView> { view ->
            assertThat(view.productIsAbsentText.isVisible == isVisible) {
                createErrorMessage(
                    element = "Out of stock visibility",
                    expected = isVisible,
                    actual = view.productIsAbsentText.isVisible
                )
            }
        }
        if (isVisible) {
            checkOutOfStockText()
        }
    }

    private companion object {
        const val OUT_OF_STOCK_TEXT_RES = R.string.view_product_snippet_price_product_absent
    }
}