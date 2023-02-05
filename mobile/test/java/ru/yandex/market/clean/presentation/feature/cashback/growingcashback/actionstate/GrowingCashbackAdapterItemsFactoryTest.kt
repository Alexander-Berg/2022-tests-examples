package ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import just.adapter.AnyItem
import just.adapter.HasAnyCallbacks
import just.adapter.HasAnyModel
import just.adapter.item.callAlways
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.item.CashbackAmountItem
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.item.ListFooterItem
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.item.ListFooterItemActions
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.item.ListHeaderItem
import ru.yandex.market.clean.presentation.feature.cashback.growingcashback.actionstate.vo.GrowingCashbackActionVo

class GrowingCashbackAdapterItemsFactoryTest {

    private val factory = GrowingCashbackAdapterItemsFactory()

    @Test
    fun createAdapterItems() {
        val vo = GrowingCashbackActionVo.GrowingCashbackActionStateVo(
            title = "title",
            header = mock(),
            items = List(3) { mock() },
            footer = mock()
        )
        val buttonActions = mock<ListFooterItemActions>()

        val expected = listOf(
            ListHeaderItem(vo.header),
            CashbackAmountItem(vo.items[0]),
            CashbackAmountItem(vo.items[1]),
            CashbackAmountItem(vo.items[2]),
            ListFooterItem(
                vo.footer,
                callAlways(buttonActions)
            )
        )
        val actual = factory.createAdapterItems(vo, buttonActions)

        for (i in expected.indices) {
            assertThat(areItemsEquals(actual[i], expected[i]))
                .withFailMessage { "items at position $i are not equals" }
                .isTrue
        }
    }

    private fun areItemsEquals(first: AnyItem, second: AnyItem): Boolean {
        val sameClass = first::class == second::class
        val sameModel = (first as? HasAnyModel)?.model == (second as? HasAnyModel)?.model
        val sameDispatcher =
            (first as? HasAnyCallbacks)?.callbacks == (second as? HasAnyCallbacks)?.callbacks
        return sameClass && sameModel && sameDispatcher
    }
}
