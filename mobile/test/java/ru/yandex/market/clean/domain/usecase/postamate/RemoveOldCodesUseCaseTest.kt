package ru.yandex.market.clean.domain.usecase.postamate

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.postamate.PostamateCellCodesRepository
import ru.yandex.market.clean.domain.model.postamat.OrderCellCode
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.data.order.OrderStatus

class RemoveOldCodesUseCaseTest {

    private val postamateCellCodesRepository = mock<PostamateCellCodesRepository> {
        on(it.getAllCachedCellCodes()).thenReturn(
            Single.just(
                listOf(
                    FIRST_TEST_ENTITY,
                    SECOND_TEST_ENTITY
                )
            )
        )
        on(it.removeOrderCellCode(any())).thenReturn(Completable.complete())
    }
    private val removeOldCellCodesUseCase = RemoveOldCellCodesUseCase(postamateCellCodesRepository)

    @Test
    fun `remove cell codes for finished orders`() {
        val deliveredOrder = Order.generateTestInstance().copy(id = 1, status = OrderStatus.DELIVERED)
        val canceledOrder = Order.generateTestInstance().copy(id = 2, status = OrderStatus.CANCELLED)

        removeOldCellCodesUseCase.removeOldCellCodesForOrders(listOf(deliveredOrder, canceledOrder)).test()

        verify(postamateCellCodesRepository).removeOrderCellCode(FIRST_TEST_ORDER_ID)
        verify(postamateCellCodesRepository).removeOrderCellCode(SECOND_TEST_ORDER_ID)
    }

    @Test
    fun `do not remove cell codes for not finished orders`() {
        val notFinishedOrder = Order.generateTestInstance().copy(id = 1)

        removeOldCellCodesUseCase.removeOldCellCodesForOrders(listOf(notFinishedOrder)).test()

        verify(postamateCellCodesRepository, times(0)).removeOrderCellCode(FIRST_TEST_ORDER_ID)
    }

    @Test
    fun `do not remove cell code if it is not in table`() {
        val deliveredOrder = Order.generateTestInstance().copy(id = 3, status = OrderStatus.DELIVERED)

        removeOldCellCodesUseCase.removeOldCellCodesForOrders(listOf(deliveredOrder)).test()

        verify(postamateCellCodesRepository, times(0)).removeOrderCellCode(FIRST_TEST_ORDER_ID)
    }

    companion object {
        private const val FIRST_TEST_ORDER_ID = "1"
        private const val SECOND_TEST_ORDER_ID = "2"
        private val FIRST_TEST_ENTITY = OrderCellCode(FIRST_TEST_ORDER_ID, "6556")
        private val SECOND_TEST_ENTITY = OrderCellCode(SECOND_TEST_ORDER_ID, "123")
    }
}