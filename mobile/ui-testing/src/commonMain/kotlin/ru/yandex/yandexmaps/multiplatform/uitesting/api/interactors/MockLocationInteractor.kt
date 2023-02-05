package ru.yandex.yandexmaps.multiplatform.uitesting.api.interactors

import ru.yandex.yandexmaps.multiplatform.core.geometry.Point

public class MockLocation(
    public val latitude: Double,
    public val longitude: Double,
)

public interface MockLocationInteractor {
    public fun setMockLocation(mockLocation: MockLocation)
}

public fun Point.toMockLocation(): MockLocation = MockLocation(latitude = lat, longitude = lon)
