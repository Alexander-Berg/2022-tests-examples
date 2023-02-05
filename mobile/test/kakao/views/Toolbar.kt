package ru.yandex.market.test.kakao.views

import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatTextView
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.toolbar.ToolbarViewAssertions
import ru.beru.android.R

open class KToolbar(private val function: ViewBuilder.() -> Unit) : KBaseView<KToolbar>(function),
    ToolbarViewAssertions {

    val navigationButton = KImageView {
        withParent(this@KToolbar.function)
        isInstanceOf(ImageButton::class.java)
    }

    val searchInput = KEditText {
        withParent(this@KToolbar.function)
        withId(R.id.search_text)
    }

    @Deprecated(message = "Use hasTitle()")
    val title = KTextView {
        withParent(this@KToolbar.function)
        isInstanceOf(AppCompatTextView::class.java)
        withId(-1)
    }
}