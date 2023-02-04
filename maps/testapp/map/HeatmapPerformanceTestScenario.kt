package com.yandex.maps.testapp.map

import android.content.Context;
import android.os.Bundle
import android.view.View
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.LayerLoadedListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.DataProvider
import com.yandex.mapkit.map.HeatmapLayer
import com.yandex.mapkit.map.MapType
import com.yandex.mapkit.map.MapWindow
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.Utils
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

class HeatmapPerformanceTestScenario: PerformanceTestScenario {
    companion object {
        private val durationFactor = 2.5f;
        val testSteps = arrayOf<PerformanceTestStep>(
            PerformanceTestStep(CameraPosition(Point(51.663910, 178.836715), 6f, 0f, 0f), 0f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(57.301306, -156.969973 + 360.0), 6f, 0f, 0f), 1.5f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(62.014384, -148.777336), 6f, 0f, 0f), 1.5f * durationFactor),

            PerformanceTestStep(CameraPosition(Point(62.014384, -148.777336), 5f, 0f, 0f), 1f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(57.301306, -156.969973), 5f, 0f, 0f), 1.5f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(51.663910, 178.836715 - 360.0), 5f, 0f, 0f), 1.5f * durationFactor),

            PerformanceTestStep(CameraPosition(Point(50.297600, -121.637404 + 360.0),  6f, 0f, 0f), .5f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(40.465743, -121.637404),  6f, 0f, 0f), 1.5f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(31.777752, -116.081650), 6f, 0f, 0f), 1.5f * durationFactor),

            PerformanceTestStep(CameraPosition(Point(40.658806, -116.634105), 4.75f, 0f, 0f), 1.5f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(40.658806, -116.634105), 0f, 90f, 0f), 1.5f * durationFactor),

            PerformanceTestStep(CameraPosition(Point(40.658806, -116.634105 - 120.0), 0f, 90f, 0f), 3f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(40.658806, -116.634105 - 240.0), 0f, 90f, 0f), 3f * durationFactor),
            PerformanceTestStep(CameraPosition(Point(40.658806, -116.634105 - 360.0), 0f, 90f, 0f), 3f * durationFactor)
        )
    }

    constructor(showLayer: Boolean, context: Context, mapWindow: MapWindow, callback: Callback, name: String)
        : super(mapWindow, callback, testSteps, name)
    {
        this.context = context
        this.mapview = mapWindow
        this.showLayer = showLayer
    }

    private val mapview: MapWindow
    private val showLayer: Boolean
    private val context: Context
    private var layer: HeatmapLayer? = null
    private val layerId: String = "heatmap_layer_for_perfomance_test"
    private var layerLoadedListener: LayerLoadedListener? = null

    fun createLayer(loadedListener: LayerLoadedListener) {
        layer?.remove()
        layer = mapview.getMap().addHeatmapLayer(
            layerId,
            style(),
            DataProvider{Utils.readResourceAsString(context, R.raw.earthquakes)}
            )

        layer!!.setLayerLoadedListener(loadedListener)
        layerLoadedListener = loadedListener
    }

    override protected fun preExecute(onComplete: Runnable) {
        mapview.getMap().setMapType(MapType.VECTOR_MAP)
        mapview.getMap().setNightModeEnabled(true)

        var loadedListener = object : LayerLoadedListener {
            override fun onLayerLoaded() {
                onComplete.run()
            }
        }

        if (showLayer) {
            createLayer(loadedListener)
        } else {
            onComplete.run()
        }
    }

    override protected fun postExecute() {
        layer?.remove()
        layer = null
    }

    private fun style() = DataProvider{
        // Style affects performance.
        // Make sure to set equivalent styles for all scenarios.
        """
        {
        "layers": [
            {
                "id": "Heatmap",
                "minzoom": 0,
                "maxzoom": 21,
                "type": "heatmap",
                "source-layer": "${layerId}",
                "style": { ${ Utils.readResourceAsString(context, R.raw.earthquakes_heatmap_style) } }
            }]
        }
        """.trimIndent()
    }

}
