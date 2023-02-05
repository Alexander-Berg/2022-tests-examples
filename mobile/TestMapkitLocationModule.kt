package ru.yandex.yandexmaps.app.di.modules

import com.yandex.mapkit.MapKit
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.location.LocationSimulator
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
interface TestMapkitLocationModule {

    @Binds
    @Singleton
    fun bindLocationManager(locationSimulator: LocationSimulator): LocationManager

    companion object {
        @Provides
        @Singleton
        fun provideLocationSimulator(mapKit: MapKit): LocationSimulator {
            return mapKit.createLocationSimulator()
        }
    }
}
