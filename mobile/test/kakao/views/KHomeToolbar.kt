package ru.yandex.market.test.kakao.views

import android.widget.TextView
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.text.KTextView
import ru.beru.android.R

open class KHomeToolbar(private val function: ViewBuilder.() -> Unit) : KBaseView<KToolbar>(function) {

    val searchInput = KEditText {
        withParent { withParent(this@KHomeToolbar.function) }
        withId(R.id.searchRequestView)
    }

    val title = KTextView {
        withParent { withParent(this@KHomeToolbar.function) }
        isInstanceOf(TextView::class.java)
    }

}