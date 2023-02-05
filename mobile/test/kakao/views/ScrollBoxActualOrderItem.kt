package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class ScrollBoxActualOrderItem(parent: Matcher<View>) : KRecyclerItem<ScrollBoxActualOrderItem>(parent) {
    val container = KView(parent) { withId(R.id.content) }
    val image = KImageView(parent) {
        withId(R.id.imageView)
        withParent { withId(R.id.content) }
    }
    val title = KTextView(parent) { withId(R.id.titleTextView) }
    val subtitle = KTextView(parent) { withId(R.id.subtitleTextView) }
    val actionText = KTextView(parent) { withId(R.id.actionTextView) }
    val actionButton = KButton(parent) { withId(R.id.actionButton) }
    val counter = KTextView(parent) { withId(R.id.countMoreItems) }
}