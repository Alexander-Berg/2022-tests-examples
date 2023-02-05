package ru.yandex.market.di.module

import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import ru.yandex.market.clean.data.store.AutoDetectedRegionDataStore
import ru.yandex.market.clean.data.store.LocationDataSource
import ru.yandex.market.common.schedulers.WorkerScheduler
import ru.yandex.market.di.TestAutoDetectedRegionDataStore
import ru.yandex.market.di.TestLocationDataSource
import ru.yandex.market.di.module.common.LocationModule
import ru.yandex.market.mocks.StateFacade
import javax.inject.Inject

class TestLocationModule @Inject constructor(private val stateFacade: StateFacade) : LocationModule() {

    override fun provideLocationDataSource(
        locationManager: LocationManager?,
        fusedLocationProviderClient: FusedLocationProviderClient?,
        workerScheduler: WorkerScheduler,
        context: Context,
        analyticsService: ru.yandex.market.analitycs.AnalyticsService
    ): LocationDataSource {
        return TestLocationDataSource(stateFacade.getStates())
    }

    override fun provideAutoDetectedRegionDataStore(): AutoDetectedRegionDataStore {
        return TestAutoDetectedRegionDataStore(stateFacade.getStates())
    }
}