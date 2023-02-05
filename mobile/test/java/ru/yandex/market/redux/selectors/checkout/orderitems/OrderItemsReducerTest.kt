package ru.yandex.market.redux.selectors.checkout.orderitems

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.checkout.OrderOptionsService
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.orderOptionsServiceTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.service.model.serviceTimeIntervalTestInstance
import ru.yandex.market.redux.actions.checkout.orderitems.OrderItemAction
import ru.yandex.market.redux.reducers.checkout.buckets.changeStateFieldByAction
import java.util.Date

class OrderItemsReducerTest {

    @Test
    fun `Bucket's order items reduced by OrderItemGuidChangeAction`() {
        val guids = listOf("A", "B", "C")
        val orderItem = orderItemTestInstance()
        val bucket = bucketInfo2TestInstance(orderItems = listOf(orderItem))
        val action = OrderItemAction.OrderItemGuidChangeAction(
            bucketId = bucket.packId,
            skuId = requireNotNull(orderItem.skuId),
            guids = guids
        )
        val reducedBucket = bucket.changeStateFieldByAction(action)
        val expectedBucket = bucket.copy(
            orderItems = listOf(
                orderItem.copy(
                    guids = guids
                )
            )
        )
        assertThat(reducedBucket).isEqualTo(expectedBucket)
    }

    @Test
    fun `Bucket's order items reduced by OrderItemServiceChangeAction`() {
        val service = orderOptionsServiceTestInstance()
        val emptyService = OrderOptionsService(
            serviceId = service.serviceId,
            title = "",
            description = "",
            date = null,
            price = Money.zeroRub(),
            timeInterval = null,
            timeslotsInfo = null
        )
        val orderItem = orderItemTestInstance(service = emptyService)
        val bucket = bucketInfo2TestInstance(orderItems = listOf(orderItem))
        val action = OrderItemAction.OrderItemServiceChangeAction(
            bucketId = bucket.packId,
            skuId = requireNotNull(orderItem.skuId),
            service = service
        )
        val reducedBucket = bucket.changeStateFieldByAction(action)
        val expectedBucket = bucket.copy(
            orderItems = listOf(
                orderItem.copy(
                    service = service
                )
            )
        )
        assertThat(reducedBucket).isEqualTo(expectedBucket)
    }

    @Test
    fun `Bucket's order items reduced by ServiceDateSelectedAction`() {
        val service = orderOptionsServiceTestInstance()
        val orderItem = orderItemTestInstance(service = service)
        val bucket = bucketInfo2TestInstance(orderItems = listOf(orderItem))
        val date = Date()
        val timeInterval = serviceTimeIntervalTestInstance()
        val action = OrderItemAction.ServiceDateSelectedAction(
            bucketId = bucket.packId,
            skuId = requireNotNull(orderItem.skuId),
            serviceId = service.serviceId,
            selectedDate = date,
            selectedTimeInterval = timeInterval
        )
        val reducedBucket = bucket.changeStateFieldByAction(action)
        val expectedBucket = bucket.copy(
            orderItems = listOf(
                orderItem.copy(
                    service = service.copy(
                        date = date,
                        timeInterval = timeInterval
                    )
                )
            )
        )
        assertThat(reducedBucket).isEqualTo(expectedBucket)
    }

    @Test
    fun `Bucket's order items not reduced by ServiceDateSelectedAction with invalid ids`() {
        val service = orderOptionsServiceTestInstance()
        val orderItem = orderItemTestInstance(service = service)
        val bucket = bucketInfo2TestInstance(orderItems = listOf(orderItem))
        val date = Date()
        val timeInterval = serviceTimeIntervalTestInstance()
        val action = OrderItemAction.ServiceDateSelectedAction(
            bucketId = bucket.packId,
            skuId = requireNotNull(orderItem.skuId),
            serviceId = service.serviceId,
            selectedDate = date,
            selectedTimeInterval = timeInterval
        )

        val randomSuffix = "SUFFIX"
        val invalidActions = listOf(
            action.copy(skuId = action.skuId + randomSuffix),
            action.copy(serviceId = action.serviceId + randomSuffix)
        )

        for (invalidAction in invalidActions) {
            val reducedBucket = bucket.changeStateFieldByAction(invalidAction)
            assertThat(reducedBucket).isEqualTo(bucket)
        }
    }
}
