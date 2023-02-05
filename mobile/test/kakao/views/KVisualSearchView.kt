package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.common.views.KView
import org.hamcrest.Matcher
import ru.beru.android.R

class KVisualSearchView(parent: Matcher<View>, function: ViewBuilder.() -> Unit) :
    KBaseView<KVisualSearchView>(parent, function) {

    private val buttonOk = KView {
        withId(R.id.buttonOk)
    }

    fun clickOkButton() {
        buttonOk.click()
    }

}

