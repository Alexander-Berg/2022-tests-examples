package com.yandex.metrokit.testapp.controller

import com.yandex.metrokit.scheme_window.surface.Surface

class RouteSurfaceController(
        routeSurface: Surface,
        pinAData: PlacemarkData,
        pinBData: PlacemarkData,
        private val stationA: String,
        private val stationB: String
) {
    private val pinAController = StationPinController(routeSurface, pinAData)
    private val pinBController = StationPinController(routeSurface, pinBData)

    fun pinEndStations(animateA: Boolean, animateB: Boolean) {
        pinAController.pin(stationA, animateA)
        pinBController.pin(stationB, animateB)
    }

    fun unpinEndStations(animateA: Boolean, animateB: Boolean) {
        pinAController.unpin(animateA)
        pinBController.unpin(animateB)
    }

    fun dispose() {
        arrayOf(pinAController, pinBController).forEach {
            it.dispose()
        }
    }
}
