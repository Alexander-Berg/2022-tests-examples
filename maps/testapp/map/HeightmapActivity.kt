package com.yandex.maps.testapp.map

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ToggleButton
import android.widget.Button
import android.widget.LinearLayout
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapType
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.experiments.Experiment
import com.yandex.maps.testapp.experiments.ExperimentsUtils
import java.util.logging.Logger

class HeightmapActivity: MapBaseActivity() {
    private companion object {
        val LOGGER = Logger.getLogger("yandex.maps_3d")!!
    }

    private val MAPS3D_SERVICE_ID: String = "MAPKIT"
    private val MAPS3D_PARAM_NAME: String = "maps_3d"

    private var terrainToggle: ToggleButton? = null
    private var tilesButton: Button? = null
    private var shadowTypeToggle: ToggleButton? = null

    private var tilesType: Int = 0

    private var pointsOfInterest = arrayOf(
            Point(55.883367, 37.485624),
            Point(55.746643, 37.612467),
            Point(55.591782, 37.729450),
            Point(55.785422, 37.565772),
            Point(55.792205, 37.587873),
            Point(55.740254, 37.535583),
            Point(55.774091, 37.552451)
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.heightmap_prototype)
        super.onCreate(savedInstanceState)
        super.setMapType(MapType.NONE)

        terrainToggle = findViewById(R.id.terrain_toggle)
        terrainToggle!!.setOnCheckedChangeListener { button, value -> updateExperimentState() }

        tilesButton = findViewById(R.id.tiles_button)
        tilesButton!!.setOnClickListener({ button -> tilesType = (tilesType + 1) % 3; updateExperimentState() })

        shadowTypeToggle = findViewById(R.id.shadow_type_toggle)
        shadowTypeToggle!!.setOnCheckedChangeListener { button, value -> updateExperimentState() }

        createLocationButtons()

        moveToPoint(0, false)

        updateExperimentState()
    }

    override fun vulkanPreferred(): Boolean {
        return false
    }

    private fun createLocationButtons() {
        var layout: LinearLayout = findViewById(R.id.location_buttons_layout)

        for (e in pointsOfInterest.withIndex()) {
            var button = Button(this)
            button.setText("Location #" + e.index.toString())
            button.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            button.setOnClickListener({ button -> moveToPoint(e.index, true) })
            layout.addView(button)
        }
    }

    private fun moveToPoint(index: Int, animate: Boolean) {
        val position = CameraPosition(pointsOfInterest[index], 15.5f, 0.0f, 45.0f)
        if (animate) {
            mapview.map.move(position, Animation(Animation.Type.SMOOTH, 2.0F), {_ -> })
        } else {
            mapview.map.move(position)
        }
    }

    private fun updateExperimentState() {
        var tilesTypeStr = ""
        if (tilesType == 0)
            tilesTypeStr = "zlev_roads"
        else if (tilesType == 1)
            tilesTypeStr = "hd_roads"
        else if (tilesType == 2)
            tilesTypeStr = "terrain_roads"
        tilesButton!!.setText(tilesTypeStr)

        val terrain = terrainToggle!!.isChecked
        val shadow = shadowTypeToggle!!.isChecked
        val experiment = Experiment(MAPS3D_SERVICE_ID, MAPS3D_PARAM_NAME, "$terrain $tilesType $shadow", "")
        ExperimentsUtils.refreshCustomExperiment(experiment)
    }

}
