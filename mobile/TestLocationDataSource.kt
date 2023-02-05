package ru.yandex.market.di

import android.location.Location
import android.location.LocationManager
import com.annimon.stream.Optional
import io.reactivex.Single
import ru.yandex.market.utils.asOptional
import ru.yandex.market.clean.data.store.LocationDataSource
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.mocks.State
import ru.yandex.market.mocks.obtain
import ru.yandex.market.mocks.state.LocationState
import ru.yandex.market.mocks.tryObtain

class TestLocationDataSource(private val states: List<State>) : LocationDataSource {

    override fun getCurrentFusedLocation(): Single<Location> {
        return getGpsLocation()
    }

    override fun getLastFusedLocation(): Single<Optional<Location>> {
        return getLastKnownGpsLocation()
    }

    override fun getNetworkLocation(): Single<Location> {
        return Single.just(states.obtain<LocationState>().locationMock.networkLocation)
            .map { mapCoordinates(LocationManager.NETWORK_PROVIDER, it) }
    }

    override fun getGpsLocation(): Single<Location> {
        return Single.just(states.obtain<LocationState>().locationMock.gpsLocation)
            .map { mapCoordinates(LocationManager.GPS_PROVIDER, it) }
    }

    override fun getLastKnownNetworkLocation(): Single<Optional<Location>> {
        return Single.just(states.tryObtain<LocationState>()?.locationMock?.lastKnownNetworkLocation.asOptional())
            .map { coordinates -> coordinates.map { mapCoordinates(LocationManager.NETWORK_PROVIDER, it) } }
    }

    override fun getLastKnownGpsLocation(): Single<Optional<Location>> {
        return Single.just(states.tryObtain<LocationState>()?.locationMock?.lastKnownGpsLocation.asOptional())
            .map { coordinates -> coordinates.map { mapCoordinates(LocationManager.GPS_PROVIDER, it) } }
    }

    private fun mapCoordinates(provider: String, coordinates: GeoCoordinates): Location {
        val result = Location(provider)
        result.latitude = coordinates.latitude
        result.longitude = coordinates.longitude
        return result
    }
}