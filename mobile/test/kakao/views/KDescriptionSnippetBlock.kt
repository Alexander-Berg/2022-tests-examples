package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KDescriptionSnippetBlock(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KDescriptionSnippetBlock>(parent, function) {
    private val description = KTextView(parent) {
        withId(R.id.description)
    }

    private val rating = KRatingBriefView(parent) {
        withId(R.id.ratingBarDescription)
    }

    private val receipt = KTextView(parent) {
        withId(R.id.receipt)
    }

    fun isDescriptionVisible() {
        description.isVisible()
    }

    fun isRatingVisible() {
        rating.isVisible()
    }

    fun checkReceiptVisibility(isVisible: Boolean) {
        if (isVisible) {
            receipt.isVisible()
            receipt.containsText(RECEIPT_TEXT)
        } else {
            receipt.isNotDisplayed()
        }
    }

    fun checkDescription(description: String) {
        this.description.hasText(description)
    }

    companion object {
        private const val RECEIPT_TEXT = "Рецептурное средство"
    }
}
