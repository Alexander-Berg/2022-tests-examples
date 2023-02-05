package ru.yandex.market.clean.domain.usecase.checkout.checkout2

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.DeliveryTypeSummary
import ru.yandex.market.clean.domain.usecase.checkout.GetDeliveryTypeSummaryUseCase
import ru.yandex.market.domain.delivery.model.DeliveryType

class GetDeliveryTypesForSplitUseCaseTest {

    private val getDeliveryTypeSummaryUseCase = mock<GetDeliveryTypeSummaryUseCase>()

    private val getDeliveryTypesForSplitUseCase = GetDeliveryTypesForSplitUseCase(getDeliveryTypeSummaryUseCase)

    @Test
    fun `Get all delivery types when all delivery types are available for all buckets`() {
        val expectedDeliveryTypes = listOf(
            DeliveryType.PICKUP,
            DeliveryType.DELIVERY,
        )
        val deliverySummaries = createDeliverySummaries(
            DeliveryType.PICKUP to true,
            DeliveryType.DELIVERY to true,
        )
        setUpDeliveryTypesSummaryUseCase(deliverySummaries)

        getDeliveryTypesForSplitUseCase.getCurrentDeliveryTypesForSplit(TEST_SPLIT_ID)
            .test()
            .assertValue(expectedDeliveryTypes)
    }

    @Test
    fun `Get no delivery types when all delivery types are not available for all buckets`() {
        val deliverySummaries = createDeliverySummaries(
            DeliveryType.PICKUP to false,
            DeliveryType.DELIVERY to false,
        )
        setUpDeliveryTypesSummaryUseCase(deliverySummaries)

        getDeliveryTypesForSplitUseCase.getCurrentDeliveryTypesForSplit(TEST_SPLIT_ID)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `Get some delivery types when some delivery types are available for all buckets and some not`() {
        val expectedDeliveryTypes = listOf(
            DeliveryType.PICKUP,
            DeliveryType.DELIVERY
        )
        val deliverySummaries = createDeliverySummaries(
            DeliveryType.PICKUP to true,
            DeliveryType.DELIVERY to true,
            DeliveryType.DELIVERY to false
        )
        setUpDeliveryTypesSummaryUseCase(deliverySummaries)

        getDeliveryTypesForSplitUseCase.getCurrentDeliveryTypesForSplit(TEST_SPLIT_ID)
            .test()
            .assertValue(expectedDeliveryTypes)
    }

    private fun setUpDeliveryTypesSummaryUseCase(
        deliverySummaries: List<DeliveryTypeSummary>
    ) {
        whenever(getDeliveryTypeSummaryUseCase.getCurrentDeliveryTypeSummaryForSplit(TEST_SPLIT_ID))
            .thenReturn(Single.just(deliverySummaries))
    }

    private fun createDeliverySummaries(
        vararg typesAndSupports: Pair<DeliveryType, Boolean>
    ): List<DeliveryTypeSummary> {
        return typesAndSupports.map { (deliveryType, allBucketsSupport) ->
            createDeliveryTypeSummary(deliveryType, allBucketsSupport)
        }
    }

    private fun createDeliveryTypeSummary(deliveryType: DeliveryType, allBucketsSupport: Boolean): DeliveryTypeSummary {
        return DeliveryTypeSummary.builder
            .deliveryType(deliveryType)
            .isFree(false)
            .allBucketsSupport(allBucketsSupport)
            .build()
    }

    companion object {
        private const val TEST_SPLIT_ID = "1"
    }
}
