package ru.yandex.market.clean.domain.usecase.postamate

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.postamate.PostamateCellCodesRepository
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.model.postamat.OrderCellCode
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.data.order.OutletInfo
import ru.yandex.market.domain.delivery.model.OutletPurpose

class PreloadCellCodesUseCaseTest {

    private val postamateCellCodesRepository = mock<PostamateCellCodesRepository> {
        on(it.getAllCachedCellCodes()).thenReturn(Single.just(listOf()))
    }
    private val getPostamateCellCodeUseCase = mock<GetPostamateCellCodeUseCase> {
        on(it.getCode(FIRST_TEST_ORDER_ID)).thenReturn(Single.just(FIRST_TEST_CELL_CODE))
        on(it.getCode(SECOND_TEST_ORDER_ID)).thenReturn(Single.just(SECOND_TEST_CELL_CODE))
    }
    private val preloadCellCodesUseCase =
        PreloadCellCodesUseCase(getPostamateCellCodeUseCase, postamateCellCodesRepository)

    @Test
    fun `preload cell codes for pickup orders`() {
        val savedCodes = mutableListOf<String>()
        val pickupPostamateOrder = createOrder()
        val secondPickupPostamateOrder = createOrder(2)

        whenever(postamateCellCodesRepository.saveOrderCellCode(any(), any())).thenAnswer { args ->
            val completable = Completable.fromAction {
                savedCodes.add(args.arguments[1].toString())
            }
            completable
        }

        preloadCellCodesUseCase.preloadCodesForOrders(
            listOf(pickupPostamateOrder, secondPickupPostamateOrder),
        ).test()

        verify(postamateCellCodesRepository).saveOrderCellCode(FIRST_TEST_ORDER_ID, FIRST_TEST_CELL_CODE)
        verify(postamateCellCodesRepository).saveOrderCellCode(SECOND_TEST_ORDER_ID, SECOND_TEST_CELL_CODE)
        assertEquals(listOf(FIRST_TEST_CELL_CODE, SECOND_TEST_CELL_CODE), savedCodes)
    }

    @Test
    fun `do not preload cell codes for not pickup orders`() {
        val pickupPostamateOrder = createOrder(status = OrderStatus.DELIVERY)

        preloadCellCodesUseCase.preloadCodesForOrders(
            listOf(pickupPostamateOrder),
        ).test()

        verify(postamateCellCodesRepository, times(0)).saveOrderCellCode(any(), any())
    }

    @Test
    fun `do not preload cell codes for not postamate orders`() {
        val pickupPostamateOrder = createOrder(purpose = OutletPurpose.POST)

        preloadCellCodesUseCase.preloadCodesForOrders(
            listOf(pickupPostamateOrder),
        ).test()

        verify(postamateCellCodesRepository, times(0)).saveOrderCellCode(any(), any())
    }

    @Test
    fun `do not preload cell codes for already loaded orders`() {
        whenever(postamateCellCodesRepository.getAllCachedCellCodes()).thenReturn(Single.just(listOf(FIRST_TEST_ENTITY)))
        val pickupPostamateOrder = createOrder()

        preloadCellCodesUseCase.preloadCodesForOrders(
            listOf(pickupPostamateOrder),
        ).test()

        verify(postamateCellCodesRepository, times(0)).saveOrderCellCode(any(), any())
    }

    private fun createOrder(
        id: Long = 1,
        status: OrderStatus = OrderStatus.PICKUP,
        purpose: OutletPurpose = OutletPurpose.POST_TERM,
    ): Order {
        return Order.generateTestInstance().copy(
            outletInfo = OutletInfo.testInstance().apply {
                outletPurposes = listOf(purpose)
            },
            id = id,
            status = status,
        )
    }

    companion object {
        private const val FIRST_TEST_ORDER_ID = "1"
        private const val FIRST_TEST_CELL_CODE = "6556"
        private const val SECOND_TEST_ORDER_ID = "2"
        private const val SECOND_TEST_CELL_CODE = "123"
        private val FIRST_TEST_ENTITY = OrderCellCode(FIRST_TEST_ORDER_ID, FIRST_TEST_CELL_CODE)
    }
}