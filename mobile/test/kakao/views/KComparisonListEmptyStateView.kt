package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

open class KComparisonListEmptyStateView(private val function: ViewBuilder.() -> Unit) : KBaseView<KView>(function) {

    private val title = KTextView {
        withParent(this@KComparisonListEmptyStateView.function)
        withId(R.id.titleTextView)
    }

    private val subTitle = KTextView {
        withParent(this@KComparisonListEmptyStateView.function)
        withId(R.id.subtitleTextView)
    }

    private val navToAllProductsButton = KButton {
        withParent(this@KComparisonListEmptyStateView.function)
        withId(R.id.navToCategoryButton)
    }

    fun clickOnNavToAllProductsButton() {
        navToAllProductsButton.click()
    }

    fun checkVisibility() {
        isVisible()
        title.isVisible()
        subTitle.isVisible()
        navToAllProductsButton.isVisible()
    }

    fun checkTexts() {
        title.hasText(EMPTY_STATE_TEXT)
        navToAllProductsButton.hasText(EMPTY_STATE_BUTTON_TEXT)
    }

    fun checkNavigateToCatalogButtonColor() {
        navToAllProductsButton.hasBackground(R.drawable.bg_button_filled_medium)
    }

    companion object {
        private const val EMPTY_STATE_TEXT = "Сравним что-нибудь?"
        private const val EMPTY_STATE_BUTTON_TEXT = "Посмотреть товары"
    }
}