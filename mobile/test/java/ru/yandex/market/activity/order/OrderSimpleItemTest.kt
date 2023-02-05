package ru.yandex.market.activity.order

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.OfferEventData
import ru.yandex.market.analytics.mapper.OfferEventDataMapper
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.clean.presentation.feature.order.details.orderItems.OrderItemVO
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.ui.view.mvp.cartcounterbutton.CartCounterArguments

class OrderSimpleItemTest {

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val offerAnalytics = mock<CartCounterArguments.OfferAnalytics>()
    private val eventData = mock<OfferEventData>()

    private val eventDataMapper = mock<OfferEventDataMapper>() {
        on { map(offerAnalytics) } doReturn eventData
    }
    private val cartCountArguments = mock<CartCounterArguments>()
    private val vo = mock<OrderItemVO>()
    private val dependencyProvider = mock<OrderSimpleItem.DependencyProvider>() {
        on { offerAnalyticsFacade } doReturn offerAnalyticsFacade
        on { offerEventDataMapper } doReturn eventDataMapper
        on { parentMvpDelegate } doReturn mock()
    }

    private val orderSimpleItem = OrderSimpleItem(
        dependencyProvider,
        mock(),
        vo,
        "tag",
        mock(),
        true,
    )

    @Test
    fun `Test order Snippet Visible event`() {
        val mockAnalytics = mock<CartCounterArguments.CartCounterAnalyticsParam>()
        whenever(cartCountArguments.cartCounterAnalytics).doReturn(mockAnalytics)
        whenever(mockAnalytics.primaryOfferAnalytics).doReturn(offerAnalytics)
        whenever(vo.cartCounterArguments).doReturn(cartCountArguments)
        whenever(vo.orderId).doReturn(1)
        whenever(vo.isInCart).doReturn(false)
        orderSimpleItem.sendOrderSnippetVisibleMetric()
        verify(offerAnalyticsFacade).orderSnippetVisible(eventData, 1, false)
    }

    @Test
    fun `Test order Snippet Visible event 2`() {
        whenever(vo.cartCounterArguments).doReturn(null)
        whenever(vo.orderId).doReturn(1)
        whenever(vo.isInCart).doReturn(false)
        whenever(vo.skuId).doReturn("id")
        val price = mock<MoneyVo>()
        whenever(vo.price).doReturn(price)
        orderSimpleItem.sendOrderSnippetVisibleMetric()
        verify(offerAnalyticsFacade).orderSnippetVisible("id", 1, price, false)
    }
}
