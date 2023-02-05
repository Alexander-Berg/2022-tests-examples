package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases.helpers

import ru.yandex.yandexmaps.multiplatform.core.geometry.BoundingBox
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point
import ru.yandex.yandexmaps.multiplatform.mapkit.geometry.BoundingBoxHelper

public object KnownLocations {
    public val MOSCOW_CENTER: Point = Point(lat = 55.751426, lon = 37.618879)
    public val YANDEX_CENTRAL_OFFICE: Point = Point(lat = 55.733842, lon = 37.588144)
    public val YANDEX_AURORA: Point = Point(lat = 55.735525, lon = 37.642474)
    public val YANDEX_SOCHI: Point = Point(lat = 43.412712, lon = 39.965866)
    public val GARDEN_RING_BBOX: BoundingBox = BoundingBox.Companion.invoke(
        55.748119,
        37.594030,
        55.758079,
        37.648741
    )
    public val DAY_NIGHT_BUS_STOP: Point = Point(lat = 55.753538, lon = 37.635391)
}
