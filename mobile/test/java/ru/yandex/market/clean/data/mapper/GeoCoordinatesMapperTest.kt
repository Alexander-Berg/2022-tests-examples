package ru.yandex.market.clean.data.mapper

import android.location.Location
import android.location.LocationManager
import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.domain.models.region.GeoCoordinates

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class GeoCoordinatesMapperTest {

    private val mapper = GeoCoordinatesMapper()

    @Test
    fun `Properly map framework location to geo coordinates`() {
        val location = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 20.0
            longitude = 30.0
        }

        val mapped = mapper.map(location)

        assertThat(mapped).isEqualTo(
            GeoCoordinates(20.0, 30.0)
        )
    }


}