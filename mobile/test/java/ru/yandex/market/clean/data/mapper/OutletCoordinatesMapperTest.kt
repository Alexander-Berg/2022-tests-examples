package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.data.fapi.dto.FrontApiGpsCoordinatesDto
import ru.yandex.market.data.searchitem.offer.Coordinates
import ru.yandex.market.data.searchitem.offer.Geo
import ru.yandex.market.domain.models.region.GeoCoordinates

class OutletCoordinatesMapperTest {

    private val outletCoordinatesMapper = OutletCoordinatesMapper()

    @Test
    fun map() {
        val geo = Geo.builder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build()
        val result = outletCoordinatesMapper.map(geo)
        assertThat(result).isEqualTo(COORDINATES)
    }

    @Test
    fun mapGps() {
        val gps = FrontApiGpsCoordinatesDto(LATITUDE, LONGITUDE)
        val result = outletCoordinatesMapper.mapGps(gps)
        assertThat(result).isEqualTo(COORDINATES)
    }

    @Test
    fun mapFromGeoCoordinates() {
        val geoCoordinates = GeoCoordinates(LATITUDE, LONGITUDE)
        val result = outletCoordinatesMapper.mapFromGeoCoordinates(geoCoordinates)
        assertThat(result).isEqualTo(COORDINATES)
    }

    companion object {
        private const val LATITUDE = 14.0
        private const val LONGITUDE = 15.0
        private val COORDINATES = Coordinates(LATITUDE, LONGITUDE)
    }
}