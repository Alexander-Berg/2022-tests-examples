package ru.yandex.yandexnavi.projected.testapp.impl

import com.yandex.navikit.providers.places.PlaceInfo
import com.yandex.navikit.providers.places.PlacesProvider
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.ADDR_YNDX_AURORA
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.ADDR_YNDX_RED_ROSE
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_AURORA
import ru.yandex.yandexnavi.projected.testapp.impl.KnownPoints.YNDX_RED_ROSE

class PlacesProviderImpl : PlacesProvider {

    private var homeCounter = 0
    private var workCounter = 0

    val home = PlaceInfo(YNDX_RED_ROSE, ADDR_YNDX_RED_ROSE)
    val work = PlaceInfo(YNDX_AURORA, ADDR_YNDX_AURORA)

    val homesAndWorks = arrayOf(
        home to work,
        home to null,
        null to work,
        null to null
    )

    override fun homeInfo(): PlaceInfo? {
        return homesAndWorks[homeCounter++ % homesAndWorks.size].first
    }

    override fun workInfo(): PlaceInfo? {
        return homesAndWorks[workCounter++ % homesAndWorks.size].second
    }
}
