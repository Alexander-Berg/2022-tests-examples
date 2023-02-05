package ru.yandex.market.clean.data.repository.checkout

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.clean.data.fapi.contract.checkout.ResolveServicesTimeslotsContract
import ru.yandex.market.clean.data.fapi.mapper.FrontApiServicesTimeslotsMapper
import ru.yandex.market.clean.data.fapi.source.checkout.CheckoutFapiClient
import ru.yandex.market.clean.domain.model.ServiceTimeslotModificationAffectingInfo
import ru.yandex.market.clean.domain.model.checkout.BucketFieldModification
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.checkout.deliveryOptionModelTestInstance
import ru.yandex.market.clean.domain.model.checkout.orderOptionsServiceTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.utils.asList
import java.util.Date

class ServicesTimeslotsRepositoryTest {

    private val date = Date(2021, 11, 12)
    private val formattedDate = "12-12-2021"

    private val checkoutFapiClient = mock<CheckoutFapiClient>()
    private val frontApiServicesTimeslotsMapper = mock<FrontApiServicesTimeslotsMapper>()
    private val dateFormatter = mock<DateFormatter> {
        on { formatDashNumericReversed(eq(date)) } doReturn formattedDate
    }

    private val servicesTimeslotsRepository = ServicesTimeslotsRepository(
        checkoutFapiClient,
        frontApiServicesTimeslotsMapper,
        dateFormatter
    )

    @Test
    fun `test correct request data for multiple splits`() {
        val regionId1 = 1L
        val regionId2 = 2L
        val gps1 = GeoCoordinates(1.1, 1.1)
        val gps2 = GeoCoordinates(2.2, 2.2)
        val serviceId1 = "serviceId1"
        val serviceId2 = "serviceId2"
        val service1 = orderOptionsServiceTestInstance(serviceId = serviceId1)
        val service2 = orderOptionsServiceTestInstance(serviceId = serviceId2)
        val item1 = orderItemTestInstance(service = service1)
        val item2 = orderItemTestInstance(service = service2)
        val pack1 = bucketInfo2TestInstance(orderItems = item1.asList())
        val pack2 = bucketInfo2TestInstance(orderItems = item2.asList())
        val split1 = checkoutSplitTestInstance(
            id = "1",
            buckets = pack1.asList(),
            deliveryLocality = deliveryLocalityTestInstance(
                regionId = regionId1,
                geoCoordinates = gps1
            )
        )
        val split2 = checkoutSplitTestInstance(
            id = "2",
            buckets = pack2.asList(),
            deliveryLocality = deliveryLocalityTestInstance(
                regionId = regionId2,
                geoCoordinates = gps2
            )
        )

        val deliveryModification1 = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = split1.id,
            packId = pack1.packId,
            selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to deliveryOptionModelTestInstance())
        )

        val deliveryModification2 = BucketFieldModification.BucketSelectedDeliveryOption(
            splitId = split2.id,
            packId = pack2.packId,
            selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to deliveryOptionModelTestInstance())
        )

        val timeslotsAffectingInfo1 = ServiceTimeslotModificationAffectingInfo(
            deliveryModification = deliveryModification1,
            deliveryDate = date,
            affectedItems = pack1.orderItems
        )

        val timeslotsAffectingInfo2 = ServiceTimeslotModificationAffectingInfo(
            deliveryModification = deliveryModification2,
            deliveryDate = date,
            affectedItems = pack2.orderItems
        )

        val expectedRequestData = listOf(
            ResolveServicesTimeslotsContract.RequestData(
                services = ResolveServicesTimeslotsContract.Service(
                    key = item1.matchingKey,
                    wareMd5Id = item1.persistentOfferId,
                    serviceId = serviceId1,
                    date = formattedDate
                ).asList(),
                regionId = regionId1,
                gps = gps1
            ),
            ResolveServicesTimeslotsContract.RequestData(
                services = ResolveServicesTimeslotsContract.Service(
                    key = item2.matchingKey,
                    wareMd5Id = item2.persistentOfferId,
                    serviceId = serviceId2,
                    date = formattedDate
                ).asList(),
                regionId = regionId2,
                gps = gps2
            )
        )

        servicesTimeslotsRepository.getServicesTimeslotsForModifications(
            splits = listOf(split1, split2),
            modifications = listOf(timeslotsAffectingInfo1, timeslotsAffectingInfo2)
        ).test()

        verify(checkoutFapiClient).getServicesTimeslots(eq(expectedRequestData))
    }

}