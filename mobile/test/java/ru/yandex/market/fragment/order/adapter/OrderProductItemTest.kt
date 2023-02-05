package ru.yandex.market.fragment.order.adapter

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.OfferEventData
import ru.yandex.market.analytics.mapper.OfferEventDataMapper
import ru.yandex.market.analytics.offer.OfferAnalyticsFacade
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.ui.view.mvp.cartcounterbutton.CartCounterArguments

class OrderProductItemTest {

    private val offerAnalyticsFacade = mock<OfferAnalyticsFacade>()
    private val offerAnalytics = mock<CartCounterArguments.OfferAnalytics>()
    private val eventData = mock<OfferEventData>()

    private val cartCountArguments = mock<CartCounterArguments>()
    private val vo = mock<OrderProductVo>()
    private val offerEventDataMapper = mock<OfferEventDataMapper>()
    private val dependencyProvider = mock<OrderProductItem.DependencyProvider>() {
        on { getOfferAnalyticsFacade() } doReturn offerAnalyticsFacade
        on { getOfferEventDataMapper() } doReturn offerEventDataMapper
        on { getParentDelegate() } doReturn mock()
    }

    private val item = OrderProductItem(
        dependencyProvider,
        mock(),
        vo,
        emptyList(),
        mock(),
        mock(),
        false
    )

    @Test
    fun `Test order snippet visible event`() {
        val mockAnalytics = mock<CartCounterArguments.CartCounterAnalyticsParam>()
        whenever(cartCountArguments.cartCounterAnalytics).doReturn(mockAnalytics)
        whenever(mockAnalytics.primaryOfferAnalytics).doReturn(offerAnalytics)
        whenever(cartCountArguments.primaryOfferId).doReturn("id")
        whenever(vo.cartCounterArguments).doReturn(cartCountArguments)
        whenever(offerEventDataMapper.map(offerAnalytics)).doReturn(eventData)
        whenever(vo.orderId).doReturn(1)
        whenever(vo.skuId).doReturn("id")
        item.sendOfferAnalytics()
        verify(offerAnalyticsFacade).orderSnippetVisible(eventData, 1, false)
    }

    @Test
    fun `Test order snippet visible event 2`() {
        whenever(vo.cartCounterArguments).doReturn(null)
        whenever(vo.orderId).doReturn(1)
        whenever(vo.skuId).doReturn("id")
        val price = mock<MoneyVo>()
        whenever(vo.price).doReturn(price)
        item.sendOfferAnalytics()
        verify(offerAnalyticsFacade).orderSnippetVisible("id", 1, price, false)
    }

    @Test
    fun `Test order snippet navigate event`() {
        whenever(vo.cartCounterArguments).doReturn(null)
        whenever(vo.orderId).doReturn(1)
        whenever(vo.skuId).doReturn("id")
        whenever(vo.offerLocalUniqueId).doReturn("local_id")
        val price = mock<MoneyVo>()
        whenever(vo.price).doReturn(price)
        item.sendOfferNavigateAnalyticsEvent()
        verify(offerAnalyticsFacade).orderSnippetNavigate("id", 1, price, null, false, "local_id")
    }
}
