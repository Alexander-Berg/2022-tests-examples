package ru.yandex.market.test.kakao.item

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.screen.CmsScreen
import ru.yandex.market.test.kakao.views.KCarouselWidgetView
import ru.yandex.market.test.kakao.views.KWidgetHeaderView

class ScrollBoxWidgetItem(parent: Matcher<View>) : KRecyclerItem<ScrollBoxWidgetItem>(parent) {

    private val headerView = KWidgetHeaderView(parent) {
        withId(R.id.widgetHeaderView)
        withParent { withId(R.id.carouselContainer) }
    }

    val actualView = KCarouselWidgetView {
        withId(R.id.carouselWidgetView)
        withParent { withId(R.id.carouselContainer) }
    }
    val title get() = actualView.title
    val recyclerView get() = actualView.recyclerView
    val showMoreButton get() = actualView.showMoreButton

    fun checkTitleText(name: String) {
        headerView.checkHasTitleText(name)
    }

    fun checkGoneTitleShowAll() {
        headerView.checkGoneShowAll()
    }

    fun getPopularBrandsVendorItem(firstWith: ViewBuilder.() -> Unit): CmsScreen.ScrollBoxVendorsWidget {
        return recyclerView.childWith {
            withDescendant {
                firstWith.invoke(this)
            }
        }
    }
}