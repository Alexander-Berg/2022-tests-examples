package com.yandex.metrokit.testapp.controller

import com.yandex.metrokit.Language
import com.yandex.metrokit.ScreenPoint
import com.yandex.metrokit.scheme.data.routing.Route
import com.yandex.metrokit.scheme_manager.Scheme
import com.yandex.metrokit.scheme_view.SchemeViewDelegate
import com.yandex.metrokit.scheme_window.TapListener
import com.yandex.metrokit.scheme_window.camera.Camera
import com.yandex.metrokit.scheme_window.camera.CameraController
import com.yandex.metrokit.scheme_window.surface.SurfaceController

class SchemeViewController(
        private val schemeViewDelegate: SchemeViewDelegate,
        private val pinAData: PlacemarkData,
        private val pinBData: PlacemarkData,
        private val stationsSelectListener: StationsSelectListener
) {
    private val surfaceTapListener = object: TapListener {
        override fun didDoubleTap(point: ScreenPoint) {
            zoom *= 2.0f
        }

        override fun didTap(point: ScreenPoint) {
            stationsSelectListener.onStationSelectClear()
        }

        override fun didLongTap(point: ScreenPoint) {
            // Nothing
        }
    }

    private var mainSurfaceController: MainSurfaceController? = null
    private var routeSurfaceController: RouteSurfaceController? = null

    init {
        schemeViewDelegate.window.addTapListener(surfaceTapListener)
    }

    fun setScheme(scheme: Scheme, language: Language = scheme.defaultLanguage) {
        invalidateAllSurfacesWithAction {
            surfaceController().setScheme(scheme, language)
        }
    }

    fun setLanguage(language: Language) {
        invalidateAllSurfacesWithAction {
            surfaceController().setLanguage(language)
        }
    }

    fun setStyle(style: String) {
        invalidateAllSurfacesWithAction {
            surfaceController().setStyle(style)
        }
    }

    var zoom: Float
        get() {
            return cameraController().camera.scale
        }
        set(value) {
            val cameraController = cameraController()
            val camera = Camera(value, cameraController.camera.position)
            cameraController.setCamera(camera, null)
        }

    fun showRoute(route: Route) {
        val animateRoutePins = routeSurfaceController == null

        routeSurfaceController?.dispose()

        val surfaceController = surfaceController()

        surfaceController.setRoute(route)

        routeSurfaceController = RouteSurfaceController(
                surfaceController.routeSurface!!,
                pinAData,
                pinBData,
                route.from,
                route.to
        ).also { it.pinEndStations(false, animateRoutePins) }

        mainSurfaceController?.hidePins(animate = false)
    }

    fun dismissRoute() {
        routeSurfaceController?.let {
            it.dispose()
            routeSurfaceController = null
        }

        surfaceController().removeRoute()

        mainSurfaceController?.showPins(animate = false)
        mainSurfaceController?.disposePins(animate = true)
    }

    fun fps() = schemeViewDelegate.window.fps()

    private fun invalidateAllSurfacesWithAction(action: () -> Unit) {
        mainSurfaceController?.dispose()
        routeSurfaceController?.dispose()

        action()

        val surfaceController = surfaceController()

        mainSurfaceController = MainSurfaceController(
                surfaceController,
                surfaceController.mainSurface!!,
                pinAData,
                pinBData,
                stationsSelectListener
        )
        routeSurfaceController = null
    }

    private fun surfaceController(): SurfaceController {
        return schemeViewDelegate.window.surfaceController
    }

    private fun cameraController(): CameraController {
        return schemeViewDelegate.window.cameraController
    }
}
