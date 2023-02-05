package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import org.hamcrest.Matcher
import ru.beru.android.R

class KDatePickerView : KBaseCompoundView<KDatePickerView> {
    constructor(function: ViewBuilder.() -> Unit) : super(KDatePickerView::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(KDatePickerView::class, parent, function)

    private val dayRecyclerView = KRecyclerView({
        withId(R.id.dayRecyclerView)
    }, itemTypeBuilder = {
        itemType(::DatePickerItem)
    })

    private val timeRecyclerView = KRecyclerView({
        withId(R.id.timeRecyclerView)
    }, itemTypeBuilder = {
        itemType(::DatePickerItem)
    })

    fun selectDay(position: Int) {
        dayRecyclerView.childAt<DatePickerItem>(position + ITEM_POSITION_OFFSET) {
            click()
        }
    }

    fun selectTime(position: Int) {
        timeRecyclerView.childAt<DatePickerItem>(position + ITEM_POSITION_OFFSET) {
            click()
        }
    }

    companion object {
        // В date picker view добавляется один пустной елемент в dayRecyclerView и hourRecyclerView
        private const val ITEM_POSITION_OFFSET = 1
    }
}

class DatePickerItem(parent: Matcher<View>) : KRecyclerItem<DatePickerItem>(parent)