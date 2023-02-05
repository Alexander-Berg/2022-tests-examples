package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.dto.FrontApiAddressDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiCoordinatesDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiDeliveryAddressDto
import ru.yandex.market.clean.data.fapi.dto.FrontApiGpsCoordinatesDto
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.data.searchitem.offer.Coordinates

class AddressMapperTest {

    private val geoCoordinatesMapper = mock<GeoCoordinatesMapper>()
    private val outletCoordinatesMapper = mock<OutletCoordinatesMapper>()
    private val resourcesManager = mock<ResourcesManager>()

    private val addressMapper = AddressMapper(geoCoordinatesMapper, outletCoordinatesMapper, resourcesManager)

    @Test
    fun `map fapi delivery address`() {
        val gps = FrontApiCoordinatesDto(
            LATITUDE,
            LONGITUDE,
        )
        val fapiAddress = FrontApiDeliveryAddressDto(
            country = "country",
            city = "city",
            street = "street",
            district = "district",
            house = "house",
            apartment = "apartment",
            postcode = "postcode",
            regionId = 1,
            floor = "floor",
            entrance = "entrance",
            block = "block",
            intercom = "intercom",
            comment = "comment",
            gpsCoordinates = gps,
        )
        val address = addressMapper.map(fapiAddress)

        assertThat(address.regionId).isEqualTo(1)
        assertThat(address.postCode).isEqualTo("postcode")
        assertThat(address.country).isEqualTo("country")
        assertThat(address.city).isEqualTo("city")
        assertThat(address.street).isEqualTo("street")
        assertThat(address.district).isEqualTo("district")
        assertThat(address.house).isEqualTo("house")
        assertThat(address.room).isEqualTo("apartment")
        assertThat(address.block).isEqualTo("block")
        assertThat(address.entrance).isEqualTo("entrance")
        assertThat(address.intercom).isEqualTo("intercom")
        assertThat(address.floor).isEqualTo("floor")
        assertThat(address.comment).isEqualTo("comment")
        assertThat(address.geoLocationAsString).isEqualTo("15.0,14.0")
        assertThat(address.location).isEqualTo(COORDINATES)
    }

    @Test
    fun `map fapi address`() {
        val fapiAddress = FrontApiAddressDto(
            fullAddress = "fullAddress",
            country = "country",
            region = "1",
            locality = "locality",
            street = "street",
            district = "district",
            km = "km",
            building = "building",
            block = "block",
            wing = "wing",
            estate = "estate",
            entrance = "entrance",
            floor = "floor",
            room = "room",
            officeNumber = "officeNumber",
            note = "note",
        )
        val gps = FrontApiGpsCoordinatesDto(
            LATITUDE,
            LONGITUDE,
        )
        whenever(outletCoordinatesMapper.mapGps(any())).thenReturn(COORDINATES)

        val address = addressMapper.map(fapiAddress, gps)

        assertThat(address.fullAddress).isEqualTo("fullAddress")
        assertThat(address.city).isEqualTo("locality")
        assertThat(address.country).isEqualTo("country")
        assertThat(address.region).isEqualTo("1")
        assertThat(address.regionId).isEqualTo(1)
        assertThat(address.street).isEqualTo("street")
        assertThat(address.district).isEqualTo("district")
        assertThat(address.block).isEqualTo("block")
        assertThat(address.building).isEqualTo("building")
        assertThat(address.house).isEqualTo("building")
        assertThat(address.entrance).isEqualTo("entrance")
        assertThat(address.floor).isEqualTo("floor")
        assertThat(address.room).isEqualTo("room")
        assertThat(address.comment).isEqualTo("note")
        assertThat(address.geoLocationAsString).isEqualTo("15.0,14.0")
        assertThat(address.location).isEqualTo(COORDINATES)
        assertThat(address.intercom).isEqualTo("officeNumber")
    }

    companion object {
        private const val LATITUDE = 14.0
        private const val LONGITUDE = 15.0
        private val COORDINATES = Coordinates(LATITUDE, LONGITUDE)
    }
}
