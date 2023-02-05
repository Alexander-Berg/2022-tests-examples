package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import ru.beru.android.R

open class KDefaultEmptyView(private val function: ViewBuilder.() -> Unit) : KBaseView<KDefaultEmptyView>(function) {

    val emptyRoot = KView {
        withParent(this@KDefaultEmptyView.function)
        withId(R.id.empty_root)
    }
}