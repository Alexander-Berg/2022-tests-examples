package ru.yandex.market.clean.domain.usecase.checkout

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import ru.yandex.market.common.LocalTime
import ru.yandex.market.checkout.domain.model.packPositionTestInstance
import ru.yandex.market.clean.domain.model.OrderItem
import ru.yandex.market.clean.domain.model.ServiceTimeslotModificationAffectingInfo
import ru.yandex.market.clean.domain.model.checkout.BucketFieldModification
import ru.yandex.market.clean.domain.model.checkout.CheckoutSplit
import ru.yandex.market.clean.domain.model.checkout.ServiceFieldModification
import ru.yandex.market.domain.service.model.ServiceTimeInterval
import ru.yandex.market.clean.domain.model.checkout.SplitFieldModification
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.checkout.deliveryOptionModelTestInstance
import ru.yandex.market.clean.domain.model.checkout.orderOptionsServiceTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.clean.domain.model.serviceTimeIntervalsInfoTestInstance
import ru.yandex.market.clean.domain.model.serviceTimeslotTestInstance
import ru.yandex.market.clean.domain.model.serviceTimeslotsInfoTestInstance
import ru.yandex.market.data.order.options.deliveryOptionTestInstance
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.extensions.plusDays
import ru.yandex.market.utils.Duration
import ru.yandex.market.utils.TimeUnit
import ru.yandex.market.utils.asList
import ru.yandex.market.utils.plus
import java.util.Date

class GetActualizedServicesDatesModificationsUseCaseTest {

    private val getServicesTimeslotsForBucketModificationsUseCase =
        mock<GetServicesTimeslotsForBucketModificationsUseCase>()

    private val useCase = GetActualizedServicesDatesModificationsUseCase(
        getServicesTimeslotsForBucketModificationsUseCase
    )

    private val dateEarlier = Date().plusDays(10)
    private val dateLater = Date().plusDays(20)

    private val earlierDeliveryOption =
        deliveryOptionModelTestInstance(deliveryOption = deliveryOptionTestInstance(endDate = dateEarlier))
    private val laterDeliveryOption =
        deliveryOptionModelTestInstance(deliveryOption = deliveryOptionTestInstance(endDate = dateLater))

    private val splitIdWithService = "splitWithService"
    private val splitIdNoService = "splitNoService"
    private val packIdWithService = "packWithService"
    private val packIdNoService = "packNoService"
    private val skuIdWithService = "skuIdWithService"
    private val skuIdNoService = "skuIdNoService"
    private val serviceId = "serviceId"

    private fun getCheckoutSplitWithoutServices(): CheckoutSplit {
        return checkoutSplitTestInstance(
            id = splitIdNoService,
            buckets = bucketInfo2TestInstance(
                packPosition = packPositionTestInstance(id = packIdNoService),
                orderItems = orderItemTestInstance(
                    skuId = skuIdNoService,
                    service = null
                ).asList()
            ).asList()
        )
    }

    private fun getCheckoutSplitWithServices(): CheckoutSplit {
        return checkoutSplitTestInstance(
            id = splitIdWithService,
            buckets = bucketInfo2TestInstance(
                packPosition = packPositionTestInstance(id = packIdWithService),
                orderItems = orderItemTestInstance(
                    skuId = skuIdWithService,
                    service = orderOptionsServiceTestInstance(serviceId = serviceId)
                ).asList()
            ).asList()
        )
    }

    private fun CheckoutSplit.getFirstPackId(): String {
        return buckets.first().packId
    }

    private fun CheckoutSplit.getFirstPackItems(): List<OrderItem> {
        return buckets.first().orderItems
    }

    @Test
    fun `no service modifications if there are no items with services`() {
        val splitWithoutServices = getCheckoutSplitWithoutServices()
        val splitId = splitWithoutServices.id
        val packId = splitWithoutServices.getFirstPackId()
        val splitsWithoutServices = splitWithoutServices.asList()

        val bucketDeliveryOptionModifications = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = splitId,
            packId = packId,
            selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to deliveryOptionModelTestInstance())
        ).asList()

        useCase.execute(splitsWithoutServices, bucketDeliveryOptionModifications)
            .test()
            .assertResult(emptyList())

        verifyZeroInteractions(getServicesTimeslotsForBucketModificationsUseCase)
    }

    @Test
    fun `use split delivery type from modification if modified`() {
        val split = checkoutSplitTestInstance(selectedDeliveryType = DeliveryType.DELIVERY)
        val splitId = split.id
        val packId = split.getFirstPackId()
        val items = split.getFirstPackItems()
        val splits = split.asList()

        val deliveryModification = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = splitId,
            packId = packId,
            selectedDeliveryOption = mapOf(
                DeliveryType.DELIVERY to earlierDeliveryOption,
                DeliveryType.PICKUP to laterDeliveryOption
            )
        )

        val modifications = listOf(
            deliveryModification,
            SplitFieldModification.SplitDeliveryType(splitId, DeliveryType.PICKUP)
        )

        val expectedRequestStartingDate = dateLater

        val expectedTimeslotsAffectingInfo = ServiceTimeslotModificationAffectingInfo(
            deliveryModification = deliveryModification,
            deliveryDate = expectedRequestStartingDate,
            affectedItems = items
        ).asList()

        useCase.execute(splits, modifications).test()

        verify(getServicesTimeslotsForBucketModificationsUseCase).execute(
            eq(splits),
            eq(expectedTimeslotsAffectingInfo)
        )
    }

    @Test
    fun `select first timeslot if present and ignore if no services affected`() {
        val splitWithServices = getCheckoutSplitWithServices()
        val splitWithoutServices = getCheckoutSplitWithoutServices()

        val splits = listOf(splitWithServices, splitWithoutServices)

        val packWithServicesMod = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = splitWithServices.id,
            packId = splitWithServices.getFirstPackId(),
            selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to earlierDeliveryOption)
        )

        val packWithoutServicesMod = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = splitWithoutServices.id,
            packId = splitWithoutServices.getFirstPackId(),
            selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to laterDeliveryOption)
        )

        val modifications = listOf(packWithoutServicesMod, packWithServicesMod)

        val expectedRequestStartingDate = dateEarlier

        val expectedTimeslotsAffectingInfo = ServiceTimeslotModificationAffectingInfo(
            deliveryModification = modifications[1],
            deliveryDate = expectedRequestStartingDate,
            affectedItems = splitWithServices.getFirstPackItems()
        ).asList()

        val earlyTimeInterval = ServiceTimeInterval(
            fromTime = LocalTime(12, 0, 0),
            toTime = LocalTime(14, 0, 0)
        )

        val lateTimeInterval = ServiceTimeInterval(
            fromTime = LocalTime(14, 0, 0),
            toTime = LocalTime(16, 0, 0)
        )

        val timeslot1 = expectedRequestStartingDate.plus(Duration(12.0, TimeUnit.HOURS))
        val timeslot2 = expectedRequestStartingDate.plus(Duration(16.0, TimeUnit.HOURS))

        val timeslotsInfo = serviceTimeslotsInfoTestInstance(
            timeslots = listOf(
                serviceTimeslotTestInstance(date = timeslot1),
                serviceTimeslotTestInstance(date = timeslot2)
            ),
            timeIntervalsInfo = serviceTimeIntervalsInfoTestInstance(
                timeIntervals = listOf(earlyTimeInterval, lateTimeInterval)
            )
        )

        val matchingKey = splitWithServices.getFirstPackItems().first().matchingKey

        val expectedServiceModifications = listOf(
            ServiceFieldModification.ServiceSelectedDate(
                splitId = splitWithServices.id,
                packId = splitWithServices.getFirstPackId(),
                skuId = skuIdWithService,
                serviceId = serviceId,
                selectedDate = timeslot1,
                selectedTimeInterval = null
            ),
            ServiceFieldModification.ServiceTimeslots(
                splitId = splitWithServices.id,
                packId = splitWithServices.getFirstPackId(),
                skuId = skuIdWithService,
                serviceId = serviceId,
                timeslotsInfo = timeslotsInfo
            )
        )

        whenever(
            getServicesTimeslotsForBucketModificationsUseCase.execute(
                eq(splits),
                eq(expectedTimeslotsAffectingInfo)
            )
        ).thenReturn(Single.just(mapOf(matchingKey to timeslotsInfo)))

        useCase.execute(splits, modifications)
            .test()
            .assertResult(expectedServiceModifications)
    }


    @Test
    fun `select latest time interval if no timeslots in response`() {
        val splitWithServices = getCheckoutSplitWithServices()
        val splitWithoutServices = getCheckoutSplitWithoutServices()

        val splits = listOf(splitWithServices, splitWithoutServices)

        val packWithServicesMod = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = splitWithServices.id,
            packId = splitWithServices.getFirstPackId(),
            selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to earlierDeliveryOption)
        )

        val modifications = listOf(packWithServicesMod)

        val expectedRequestStartingDate = dateEarlier

        val expectedTimeslotsAffectingInfo = ServiceTimeslotModificationAffectingInfo(
            deliveryModification = modifications[0],
            deliveryDate = expectedRequestStartingDate,
            affectedItems = splitWithServices.getFirstPackItems()
        ).asList()

        val earlyTimeInterval = ServiceTimeInterval(
            fromTime = LocalTime(12, 0, 0),
            toTime = LocalTime(14, 0, 0)
        )

        val lateTimeInterval = ServiceTimeInterval(
            fromTime = LocalTime(14, 0, 0),
            toTime = LocalTime(16, 0, 0)
        )

        val timeslotsInfo = serviceTimeslotsInfoTestInstance(
            timeslots = null,
            timeIntervalsInfo = serviceTimeIntervalsInfoTestInstance(
                timeIntervals = listOf(earlyTimeInterval, lateTimeInterval)
            )
        )

        val matchingKey = splitWithServices.getFirstPackItems().first().matchingKey

        val expectedServiceDate = expectedRequestStartingDate.plusDays(1)

        val expectedServiceModifications = listOf(
            ServiceFieldModification.ServiceSelectedDate(
                splitId = splitWithServices.id,
                packId = splitWithServices.getFirstPackId(),
                skuId = skuIdWithService,
                serviceId = serviceId,
                selectedDate = expectedServiceDate,
                selectedTimeInterval = lateTimeInterval
            ),
            ServiceFieldModification.ServiceTimeslots(
                splitId = splitWithServices.id,
                packId = splitWithServices.getFirstPackId(),
                skuId = skuIdWithService,
                serviceId = serviceId,
                timeslotsInfo = timeslotsInfo
            )
        )

        whenever(
            getServicesTimeslotsForBucketModificationsUseCase.execute(
                eq(splits),
                eq(expectedTimeslotsAffectingInfo)
            )
        ).thenReturn(Single.just(mapOf(matchingKey to timeslotsInfo)))

        useCase.execute(splits, modifications)
            .test()
            .assertResult(expectedServiceModifications)
    }

}
