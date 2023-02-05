package ru.yandex.market.clean.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock
import ru.yandex.market.checkout.delivery.share.ShareDeliveryUseCases
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.data.order.OutletInfo
import ru.yandex.market.data.order.options.DeliveryOption
import ru.yandex.market.data.order.options.point.OutletPoint
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.money.MoneyComparator

@RunWith(Parameterized::class)
class DeliveryTypeSummaryMapperTest(private val args: DeliveryTypeSummaryMapperTestArgs) {
    data class DeliveryTypeSummaryMapperTestArgs(
        val packId: String,
        val bucketsCount: Int,
        val expectedPickPointCount: Int,
        val typeIsSupported: Boolean,
        val deliveryType: DeliveryType,
        val deliveryOptions: List<DeliveryOption>
    );

    private val moneyMapper = mock<MoneyMapper>()
    private val moneyComparator = mock<MoneyComparator>()
    private val mapper = DeliveryTypeSummaryMapper(moneyMapper, moneyComparator)

    @Test
    fun `Check mapping`() {
        val commonOptions = mapper.getCommonOptions(
            args.deliveryType,
            args.deliveryOptions,
            args.packId != ShareDeliveryUseCases.SHARED_PACK_ID,
            args.bucketsCount,
        )
        val pickupPointsCount = mapper.calcPickupPointsCount(args.deliveryType, commonOptions)
        val isAllBucketsSupport = mapper.isAllBucketsSupport(
            args.deliveryType,
            commonOptions,
            args.packId != ShareDeliveryUseCases.SHARED_PACK_ID,
            args.bucketsCount
        )
        assertEquals(args.expectedPickPointCount, pickupPointsCount)
        assertEquals(args.typeIsSupported, isAllBucketsSupport)
    }

    companion object {
        @Parameterized.Parameters(name = "{index}: expected: {0}")
        @JvmStatic
        fun data() = listOf(
            // 0
            // if one pack with normalized delivery options then return count of them
            DeliveryTypeSummaryMapperTestArgs(
                packId = "123",
                bucketsCount = 1,
                expectedPickPointCount = 2,
                typeIsSupported = true,
                deliveryType = DeliveryType.PICKUP,
                deliveryOptions = listOf(
                    DeliveryOption.testInstance().copy(
                        id = "id1",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("1").build())
                    ),
                    DeliveryOption.testInstance().copy(
                        id = "id2",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("2").build())
                    )
                )
            ),

            // 1
            // if one pack with different delivery options but the same outlets then return count without duplication
            DeliveryTypeSummaryMapperTestArgs(
                packId = "123",
                bucketsCount = 1,
                expectedPickPointCount = 1,
                typeIsSupported = true,
                deliveryType = DeliveryType.PICKUP,
                deliveryOptions = listOf(
                    DeliveryOption.testInstance().copy(
                        id = "id1",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("11").build())
                    ),
                    DeliveryOption.testInstance().copy(
                        id = "id2",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("11").build())

                    )
                )
            ),

            // 2
            // count only common outlets
            DeliveryTypeSummaryMapperTestArgs(
                packId = ShareDeliveryUseCases.SHARED_PACK_ID,
                bucketsCount = 2,
                expectedPickPointCount = 2,
                typeIsSupported = true,
                deliveryType = DeliveryType.PICKUP,
                deliveryOptions = listOf(
                    DeliveryOption.testInstance().copy(
                        id = "id1",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("1").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id2",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("2").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id1",
                        packId = "packId2",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("1").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id2",
                        packId = "packId2",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("2").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id3",
                        packId = "packId2",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("3").build())
                    )
                )
            ),

            // 3
            // count only common outlets
            DeliveryTypeSummaryMapperTestArgs(
                packId = ShareDeliveryUseCases.SHARED_PACK_ID,
                bucketsCount = 2,
                expectedPickPointCount = 0,
                typeIsSupported = false,
                deliveryType = DeliveryType.PICKUP,
                deliveryOptions = listOf(
                    DeliveryOption.testInstance().copy(
                        id = "id1",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("1").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id2",
                        packId = "packId1",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("2").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id3",
                        packId = "packId2",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("3").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id4",
                        packId = "packId2",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("4").build())
                    ),

                    DeliveryOption.testInstance().copy(
                        id = "id5",
                        packId = "packId2",
                        deliveryPoint = OutletPoint(OutletInfo.testInstance().toBuilder().id("5").build())
                    )
                )
            )
        )
    }
}
