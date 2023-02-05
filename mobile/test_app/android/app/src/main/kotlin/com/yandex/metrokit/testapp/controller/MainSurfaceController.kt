package com.yandex.metrokit.testapp.controller

import com.yandex.metrokit.scheme_window.surface.SurfaceController
import com.yandex.metrokit.scheme_window.surface.Surface
import com.yandex.metrokit.scheme_window.surface.SurfaceListener
import com.yandex.metrokit.scheme_window.surface.SurfaceObject
import com.yandex.metrokit.scheme_window.surface.SurfaceObjectSchemeMetadata
import java.util.ArrayList

interface StationsSelectListener {
    fun onStationsSelect(stationA: String, stationB: String)
    fun onStationSelectClear()
}

class MainSurfaceController(
        private val surfaceController: SurfaceController, // TODO: Remove this dependency
        private val mainSurface: Surface,
        pinAData: PlacemarkData,
        pinBData: PlacemarkData,
        private val stationsSelectListener: StationsSelectListener
) {
    private val surfaceListener = SurfaceListener(this::onSchemeObjectTap)

    private val selectedStationsIds = ArrayList<String>(2)

    private val pinAController = StationPinController(mainSurface, pinAData)
    private val pinBController = StationPinController(mainSurface, pinBData)

    init {
        mainSurface.addListener(surfaceListener)
    }

    fun dispose() {
        if (mainSurface.isValid) {
            mainSurface.removeListener(surfaceListener)
        }
        clearState()
    }

    fun clearState() {
        selectedStationsIds.clear()
        arrayOf(pinAController, pinBController).forEach { it.dispose() }

    }

    fun showPins(animate: Boolean) {
        if (selectedStationsIds.size > 0) {
            pinAController.pin(selectedStationsIds[0], animate)
        }
        if (selectedStationsIds.size > 1) {
            pinBController.pin(selectedStationsIds[1], animate)
        }
    }

    fun hidePins(animate: Boolean) {
        arrayOf(pinAController, pinBController).forEach { it.unpin(animate) }
    }

    fun disposePins(animate: Boolean) {
        selectedStationsIds.clear()
        hidePins(animate)
    }

    private fun onSchemeObjectTap(surfaceObject: SurfaceObject) {
        if (surfaceController.routeSurface != null) {
            stationsSelectListener.onStationSelectClear()
            return
        }

        val stationId = surfaceObject.metadata.getItem(SurfaceObjectSchemeMetadata::class.java)
                ?.impl?.stationMetadata?.stationId ?: return

        if (!selectedStationsIds.contains(stationId)) {
            selectedStationsIds.add(stationId)
            if (selectedStationsIds.size == 1) {
                pinAController.pin(stationId, animated = true)
            }
        } else {
            selectedStationsIds.remove(stationId)
            val pinController = if (selectedStationsIds.size == 1) pinBController else pinAController
            pinController.unpin(animated = true)
        }

        if (selectedStationsIds.size == 2) {
            stationsSelectListener.onStationsSelect(selectedStationsIds[0], selectedStationsIds[1])
        }
    }
}
