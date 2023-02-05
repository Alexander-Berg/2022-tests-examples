package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class KWidgetHeaderView(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KWidgetHeaderView>(parent, function) {

    private val showAllTextView = KTextView(parent) {
        withId(R.id.showMoreTextView)
    }

    private val title = KTextView(parent) {
        withId(R.id.titleTextView)
    }

    fun clickShowAll() {
        showAllTextView.click()
    }

    fun checkVisibleShowAll() {
        showAllTextView.isVisible()
    }

    fun checkVisibleTitle() {
        title.isVisible()
    }

    fun checkHasTitleText(name: String) {
        title.hasText(name)
    }

    fun checkGoneShowAll() {
        showAllTextView.isGone()
    }
}