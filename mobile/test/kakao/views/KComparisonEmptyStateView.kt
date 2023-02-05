package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

class KComparisonEmptyStateView(private val function: ViewBuilder.() -> Unit) : KBaseView<KView>(function) {

    private val title = KTextView {
        withParent(this@KComparisonEmptyStateView.function)
        withId(R.id.titleTextView)
    }

    private val subTitle = KTextView {
        withParent(this@KComparisonEmptyStateView.function)
        withId(R.id.subtitleTextView)
    }

    private val navToCategoryButton = KButton {
        withParent(this@KComparisonEmptyStateView.function)
        withId(R.id.navToCategoryButton)
    }

    fun checkDisplayed() {
        isDisplayed()
        title.isDisplayed()
        subTitle.isDisplayed()
        navToCategoryButton.isDisplayed()
    }

    fun checkVisibility() {
        isVisible()
        title.isVisible()
        subTitle.isVisible()
        navToCategoryButton.isVisible()
    }

    fun checkTexts() {
        title.hasText(EMPTY_STATE_TITLE)
        subTitle.hasText(EMPTY_STATE_SUB_TITLE)
        navToCategoryButton.hasText(EMPTY_STATE_BUTTON_TEXT)
    }

    fun clickNavToCategory() {
        navToCategoryButton.click()
    }

    fun checkNavigateToCategoryButtonColor() {
        navToCategoryButton.hasBackground(R.drawable.bg_button_filled_medium)
    }

    companion object {
        private const val EMPTY_STATE_TITLE = "Сравним ещё что-нибудь?"
        private const val EMPTY_STATE_SUB_TITLE = "Похоже, вы удалили всё из этого списка.\n" +
                "Посмотрите ещё товары в этой категории — вдруг что-то понравится."
        private const val EMPTY_STATE_BUTTON_TEXT = "К категории"

    }
}