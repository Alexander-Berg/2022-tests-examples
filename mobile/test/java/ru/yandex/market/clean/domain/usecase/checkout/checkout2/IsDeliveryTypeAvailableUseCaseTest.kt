package ru.yandex.market.clean.domain.usecase.checkout.checkout2

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.checkout.CheckoutSplit
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.domain.delivery.model.DeliveryType

class IsDeliveryTypeAvailableUseCaseTest {

    private val checkoutSplitsUseCase = mock<CheckoutSplitsUseCase>()

    private val isDeliveryTypeAvailableUseCase = IsDeliveryTypeAvailableUseCase(checkoutSplitsUseCase)

    @Test
    fun `Delivery type is available for not click and collect split`() {
        val clickAndCollectSplit = createMockCheckoutSplit(isClickAndCollect = false)
        setUpMockCheckoutSplitsUseCase(TEST_SPLIT_ID, clickAndCollectSplit)
        val testDeliveryType = DeliveryType.DELIVERY

        isDeliveryTypeAvailableUseCase.execute(TEST_SPLIT_ID, testDeliveryType)
            .test()
            .assertValue(true)
    }

    @Test
    fun `Delivery type is available for click and collect split and pickup delivery type`() {
        val clickAndCollectSplit = createMockCheckoutSplit(isClickAndCollect = true)
        setUpMockCheckoutSplitsUseCase(TEST_SPLIT_ID, clickAndCollectSplit)
        val testDeliveryType = DeliveryType.PICKUP

        isDeliveryTypeAvailableUseCase.execute(TEST_SPLIT_ID, testDeliveryType)
            .test()
            .assertValue(true)
    }

    @Test
    fun `Delivery type is not available for click and collect split and not pickup delivery type`() {
        val notClickAndCollectSplit = createMockCheckoutSplit(isClickAndCollect = true)
        setUpMockCheckoutSplitsUseCase(TEST_SPLIT_ID, notClickAndCollectSplit)
        val testDeliveryType = DeliveryType.DELIVERY

        isDeliveryTypeAvailableUseCase.execute(TEST_SPLIT_ID, testDeliveryType)
            .test()
            .assertValue(false)
    }

    private fun setUpMockCheckoutSplitsUseCase(testSplitId: String, notClickAndCollectSplit: CheckoutSplit) {
        whenever(checkoutSplitsUseCase.getCheckoutSplit(testSplitId))
            .thenReturn(Single.just(notClickAndCollectSplit))
    }

    private fun createMockCheckoutSplit(isClickAndCollect: Boolean): CheckoutSplit {
        val orderItem = orderItemTestInstance(
            isClickAndCollect = isClickAndCollect
        )
        val bucket = bucketInfo2TestInstance(
            orderItems = listOf(orderItem)
        )
        return checkoutSplitTestInstance(
            buckets = listOf(bucket)
        )
    }

    companion object {
        private const val TEST_SPLIT_ID = "1"
    }
}
