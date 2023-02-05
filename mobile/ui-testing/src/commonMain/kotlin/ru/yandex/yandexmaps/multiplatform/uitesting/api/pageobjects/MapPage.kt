package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPosition
import ru.yandex.yandexmaps.multiplatform.mapkit.map.ScreenPoint

public interface MapPage {
    public fun setCameraPosition(cameraPosition: CameraPosition)
    public fun getCameraPosition(): CameraPosition?

    // masstransit
    public fun isMtLayerEnabled(): Boolean
    public fun isAnyMtVehicleInVisibleRegion(): Boolean
    public fun isAnyMTVehicleVisible(): Boolean
    public fun isAnyMtStopVisible(): Boolean
    public fun tapOnAnyMtVehicleOnMap()
    public fun tapOnAnyMtStopOnMap()

    public fun isAnyPolylineOnMapVisible(): Boolean

    public fun longTapOnMap(point: ScreenPoint)
    public fun tapOnMap(point: ScreenPoint)
    public fun doubleTapOnMap(point: ScreenPoint)

    public fun isAnyParkingVisible(): Boolean
    public fun isAnyTrafficLinesVisible(): Boolean
    public fun isAnyPanoramasVisible(): Boolean
    public fun isAnySearchPinsVisible(): Boolean
}
