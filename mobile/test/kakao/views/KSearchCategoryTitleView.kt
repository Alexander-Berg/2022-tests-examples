package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

class KSearchCategoryTitleView(function: ViewBuilder.() -> Unit) :
    KBaseView<KSearchCategoryTitleView>(function) {

    private val categoryTitle = KTextView {
        withId(R.id.categoryNameTitle)
    }
}