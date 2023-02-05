package ru.yandex.market.test.kakao.views

import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

class KErrorAlertView(function: ViewBuilder.() -> Unit) : KBaseView<KErrorAlertView>(function) {

    val alertTitle = KTextView {
        withId(R.id.alertTitle)
    }
}