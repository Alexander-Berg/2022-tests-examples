package ru.yandex.yandexmaps.multiplatform.uitesting.api

import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPosition
import ru.yandex.yandexmaps.multiplatform.uitesting.api.interactors.MockLocation

/*
    Lines must be sorted!
 */
public interface ApplicationInteractor {
    public fun getMetricsEvents(): List<MetricsEvent>
    public fun mockLocation(location: MockLocation)
}
