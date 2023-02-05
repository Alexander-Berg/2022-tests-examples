package ru.yandex.market.test.kakao.views

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import kotlinx.android.synthetic.main.view_product_snippet.view.actionButton
import kotlinx.android.synthetic.main.view_product_snippet.view.productImage
import kotlinx.android.synthetic.main.view_product_snippet.view.productTitle
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.stories.views.StorySkuView
import ru.yandex.market.test.util.assertThat
import ru.yandex.market.test.util.createErrorMessage
import ru.yandex.market.test.util.withView
import ru.yandex.market.util.tint
import ru.yandex.market.util.tintResId

class KStorySkuView(function: ViewBuilder.() -> Unit) : KBaseView<KStorySkuView>(function) {

    fun checkTitle(text: String) {
        withView<StorySkuView> { view ->
            assertThat(view.productTitle.text.contains(text)) {
                createErrorMessage(
                    element = "SkuTitle",
                    expected = text,
                    actual = view.productTitle.text
                )
            }
        }
    }

    fun checkAddedToFavorite() {
        withView<StorySkuView> { view ->
            assertThat(view.actionButton.tint == ContextCompat.getColor(view.context, R.color.red_normal)) {
                createErrorMessage(
                    element = "AddToFavorites",
                    expected = R.color.red_normal,
                    actual = view.actionButton.tint
                )
            }
        }
    }

    fun checkNotAddedToFavorite() {
        withView<StorySkuView> { view ->
            assertThat(view.actionButton.tint == ContextCompat.getColor(view.context, R.color.warm_gray_500)) {
                createErrorMessage(
                    element = "AddToFavorites",
                    expected = R.color.warm_gray_500,
                    actual = view.actionButton.tint
                )
            }
        }
    }

    fun checkImageVisible(isVisible: Boolean) {
        withView<StorySkuView> { view ->
            assertThat(view.productImage.isVisible == isVisible) {
                createErrorMessage(
                    element = "SkuImage",
                    expected = isVisible,
                    actual = view.productImage.tint
                )
            }
        }
    }

    fun clickActionButton() {
        withView<StorySkuView> { view ->
            assertThat(view.actionButton.performClick()) {
                createErrorMessage(
                    element = "AddToFavorites",
                    actual = "was not clicked"
                )
            }
        }
    }
}