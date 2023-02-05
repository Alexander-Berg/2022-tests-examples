package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.beru.android.R

class ScrollBoxLiveStreamItem(parent: Matcher<View>) : KRecyclerItem<ScrollBoxLiveStreamItem>(parent) {

    private val title = KTextView(parent) {
        withId(R.id.cmsLiveStreamHorizontalTitle)
    }

    private val date = KTextView(parent) {
        withId(R.id.cmsLiveStreamHorizontalDate)
    }

    fun scrollToTranslation(position: Int, recyclerView: KRecyclerView) {
        recyclerView.scrollTo(position)
    }

    fun checkTitle(title: String) {
        this.title.containsText(title)
    }

    fun checkDate(date: String) {
        this.date.containsText(date)
    }
}