package ru.yandex.market.test.kakao.util

import io.github.kakaocup.kakao.common.matchers.PositionMatcher
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import com.yandex.alicekit.core.utils.getOrThrow
import junit.framework.AssertionFailedError

inline fun <reified T : KRecyclerItem<*>> KRecyclerView.findFirstItemPositionOrThrow(assertion: T.() -> Unit): Int {
    val adapterSize = getSize()
    if (adapterSize <= 0) {
        throw AssertionFailedError("RecyclerView пустой.")
    }
    for (i in 0 until adapterSize) {
        scrollTo(i)
        try {
            childAt(i, assertion)
            return i
        } catch (error: AssertionError) {
            // no-op
        }
    }
    throw AssertionFailedError("Ни один из детей RecyclerView не выполняет заданного условия.")
}

inline fun <reified T : KRecyclerItem<*>> KRecyclerView.executeOnRecyclerItemWithoutScroll(
    position: Int,
    function: T.() -> Unit
) {
    val item = itemTypes.getOrThrow(T::class)
        .provideItem(PositionMatcher(matcher, position)) as T
    item.function()
}