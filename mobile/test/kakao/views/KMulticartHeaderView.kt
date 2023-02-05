package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.hasTextColor
import ru.beru.android.R
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import org.hamcrest.Matcher
import ru.yandex.market.uikit.text.InternalTextView

class KMulticartHeaderView(function: ViewBuilder.() -> Unit) : KBaseView<KMulticartHeaderView>(function) {

    private val multicartHeaderItemsRecyclerView = KRecyclerView(builder = {
        withId(R.id.recyclerView)
        isDescendantOfA { withId(R.id.contentContainer) }
    }, itemTypeBuilder = {
        itemType(::KMulticartHeaderItem)
    })

    fun checkIsItemSelected(position: Int) {
        multicartHeaderItemsRecyclerView.childAt<KMulticartHeaderItem>(position) {
            matches {
                isInstanceOf(InternalTextView::class.java)
                isVisible()
                hasTextColor(R.color.black)
            }
        }
    }

    fun clickOnItem(position: Int) {
        multicartHeaderItemsRecyclerView.childAt<KMulticartHeaderItem>(position) {
            click()
        }
    }

    class KMulticartHeaderItem(parent: Matcher<View>) : KRecyclerItem<KMulticartHeaderItem>(parent)
}
