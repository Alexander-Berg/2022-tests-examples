package ru.yandex.market.checkout.tds

import android.webkit.CookieManager
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.PayLinkDecorator
import ru.yandex.market.clean.data.repository.PaymentRepository
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.model.orderPaymentTestInstance
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.util.auth.yuid.YandexUidProvider

class LoadPayUrlUseCaseTest {

    private val paymentRepository = mock<PaymentRepository>()
    private val yandexUidProvider = mock<YandexUidProvider>()
    private val payLinkDecorator = mock<PayLinkDecorator>()
    private val cookieManager = mock<CookieManager>()
    private val getOrderUseCase = mock<GetOrderUseCase>()

    private val loadPayUrlUseCase =
        LoadPayUrlUseCase(
            paymentRepository,
            yandexUidProvider,
            payLinkDecorator,
            cookieManager,
            getOrderUseCase
        )

    @Test
    fun `Ignore order if his id is not long`() {
        val normalId = 2141L
        val status = OrderStatus.UNPAID
        val orderPayment = orderPaymentTestInstance()
        whenever(getOrderUseCase.execute(normalId.toString(), false)).thenReturn(
            Single.just(createMockOrder(normalId, status))
        )
        whenever(paymentRepository.mapOrderStatusToPayment(eq(status), eq(2))).thenReturn(Single.just(orderPayment))
        loadPayUrlUseCase.checkOrdersStatuses(listOf("fdasf", normalId.toString()))
            .test()
            .assertResult(orderPayment)
    }

    private fun createMockOrder(orderId: Long, orderStatus: OrderStatus?): Order {
        return Order.generateTestInstance().copy(id = orderId, status = orderStatus)
    }

    @Test
    fun `Ignore order with null status`() {
        val id1 = 1L
        val id2 = 2L
        val status = OrderStatus.UNPAID
        val orderPayment = orderPaymentTestInstance()

        whenever(getOrderUseCase.execute(id1.toString(), false)).thenReturn(
            Single.just(createMockOrder(id1, null))
        )
        whenever(getOrderUseCase.execute(id2.toString(), false)).thenReturn(
            Single.just(createMockOrder(id2, status))
        )
        whenever(paymentRepository.mapOrderStatusToPayment(eq(status), eq(2))).thenReturn(Single.just(orderPayment))

        loadPayUrlUseCase.checkOrdersStatuses(listOf(id1.toString(), id2.toString()))
            .test()
            .assertResult(orderPayment)
    }

    @Test
    fun `Ignore error when getting order`() {
        val id1 = 1L
        val id2 = 2L
        val status = OrderStatus.UNPAID
        val orderPayment = orderPaymentTestInstance()

        whenever(getOrderUseCase.execute(id2.toString(), false)).thenReturn(
            Single.error(RuntimeException())
        )
        whenever(getOrderUseCase.execute(id2.toString(), false)).thenReturn(
            Single.just(createMockOrder(id2, status))
        )
        whenever(paymentRepository.mapOrderStatusToPayment(eq(status), eq(2))).thenReturn(Single.just(orderPayment))

        loadPayUrlUseCase.checkOrdersStatuses(listOf(id1.toString(), id2.toString()))
            .test()
            .assertResult(orderPayment)
    }

    @Test
    fun `Ignore error when getting orderPayment`() {
        val id1 = 1L
        val id2 = 2L
        val status1 = OrderStatus.UNPAID
        val status2 = OrderStatus.PROCESSING
        val orderPayment = orderPaymentTestInstance()

        whenever(getOrderUseCase.execute(id1.toString(), false)).thenReturn(
            Single.just(createMockOrder(id1, null))
        )
        whenever(getOrderUseCase.execute(id2.toString(), false)).thenReturn(
            Single.just(createMockOrder(id2, status2))
        )
        whenever(
            paymentRepository.mapOrderStatusToPayment(
                eq(status1),
                eq(2)
            )
        ).thenReturn(Single.error(RuntimeException()))
        whenever(paymentRepository.mapOrderStatusToPayment(eq(status2), eq(2))).thenReturn(Single.just(orderPayment))

        loadPayUrlUseCase.checkOrdersStatuses(listOf(id1.toString(), id2.toString()))
            .test()
            .assertResult(orderPayment)
    }
}