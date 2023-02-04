package com.yandex.maps.testapp.map

import android.graphics.Color
import android.os.Bundle
import android.widget.CompoundButton
import androidx.core.graphics.ColorUtils
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.Utils
import com.yandex.maps.testapp.driving.Router
import com.yandex.maps.testapp.experiments.Experiment
import com.yandex.maps.testapp.experiments.ExperimentsUtils
import com.yandex.runtime.Error
import org.json.JSONObject
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.math.max

class RoadsDump(json: String) : JSONObject(json) {
    val roadSegments = this.getJSONArray("features")
        .let { 0.until(it.length()).map { i -> it.getJSONObject(i) } }
        .map { RoadSegment(it) }
        .filter {it.properties.zLevel != 0}

    class RoadSegment(f: JSONObject) {
        val properties: FeatureProperties = FeatureProperties(f.getJSONObject("properties"))
        val geometry: FeatureGeometry = FeatureGeometry(f.getJSONObject("geometry"))
    }

    class FeatureProperties(p: JSONObject) {
        val zLevel = max(p.getInt("f_zlev"), p.getInt("t_zlev"))
    }

    class FeatureGeometry(g: JSONObject) {
        val points = g.getJSONArray("coordinates")
            .let { 0.until(it.length()).map { i -> it.getJSONArray(i) } }
            .map { Point(it.getDouble(1), it.getDouble(0)) }
    }
}

class MapRouteHDActivity: MapBaseActivity() {
    private companion object {
        val LOGGER: Logger = Logger.getLogger("yandex.maps")
    }
    private var routePolyline1 : PolylineMapObject? = null
    private var routePolyline2 : PolylineMapObject? = null
    private var routePolyline3 : PolylineMapObject? = null

    private var roadsDump: RoadsDump? = null

    private var mapInputListener: InputListener? = null
    private var customRouteStartPoint: Point? = null
    private var customRouteEndPoint: Point? = null
    private var customRoutePolyline: PolylineMapObject? = null
    private var customRouteStartPin: PlacemarkMapObject? = null
    private var customRouteEndPin: PlacemarkMapObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        /*
        val experimentDatatesting = Experiment("MAPS_CONFIG", "experimental_datatesting", "1")
        val grayMapDesignId = "1eb49904-f17f-479f-8b58-0cf9cc4ae94e" // (серая карта)
        val whiteMapDesignId = "a2c30e38-44f5-4933-a2d3-b03474242611" // (белая карта)
        val experimentMapDesign = Experiment("MAPS_RENDERER", "experimental_map_design", grayMapDesignId)
        ExperimentsUtils.refreshCustomExperiment(experimentDatatesting)
        ExperimentsUtils.refreshCustomExperiment(experimentMapDesign)
*/
        setContentView(R.layout.map_route_hd)
        super.onCreate(savedInstanceState)

        roadsDump = RoadsDump(Utils.readResourceAsString(this, R.raw.zlevel_msk_kutuz))

        val roadDumpCollection = mapview.map.mapObjects.addCollection()
        for (segment in roadsDump!!.roadSegments) {
            val p = roadDumpCollection.addPolyline(Polyline(segment.geometry.points))
            var color = Color.LTGRAY
            if (segment.properties.zLevel != 0) {
                color = if (segment.properties.zLevel > 0) Color.CYAN else Color.MAGENTA
                color = ColorUtils.blendARGB(color, Color.BLACK,  1 - 1F / abs(segment.properties.zLevel))
            }
            p.setStrokeColor(color)
            p.strokeWidth = 1F
        }

        val roadDumpSwitch: CompoundButton = findViewById(R.id.road_dump_switch)
        roadDumpSwitch.setOnCheckedChangeListener{_, isChecked -> roadDumpCollection.isVisible = isChecked}

        val route1Switch: CompoundButton = findViewById(R.id.route_1_switch)
        route1Switch.setOnCheckedChangeListener{_, isChecked -> routePolyline1?.isVisible = isChecked}

        val route2Switch: CompoundButton = findViewById(R.id.route_2_switch)
        route2Switch.setOnCheckedChangeListener{_, isChecked -> routePolyline2?.isVisible = isChecked}

        val route3Switch: CompoundButton = findViewById(R.id.route_3_switch)
        route3Switch.setOnCheckedChangeListener{_, isChecked -> routePolyline3?.isVisible = isChecked}

        val textStyle = TextStyle().setColor(Color.MAGENTA).setSize(12f).setOffset(3f).setPlacement(TextStyle.Placement.BOTTOM);
        customRouteStartPin = mapview.map.mapObjects.addPlacemark(Point(0.0,0.0))
        customRouteStartPin!!.setText("Старт", textStyle)
        customRouteEndPin = mapview.map.mapObjects.addPlacemark(Point(0.0,0.0))
        customRouteEndPin!!.setText("Финиш", textStyle)
        mapInputListener = object : InputListener {
            override fun onMapTap(p0: Map, p1: Point) {}

            override fun onMapLongTap(map: Map, point: Point) {
                if (customRouteStartPoint != null && customRouteEndPoint != null) {
                    customRouteStartPoint = point
                    customRouteStartPin!!.geometry = point
                    customRouteEndPoint = null
                    customRouteEndPin!!.geometry = Point(0.0, 0.0)
                    mapview.map.mapObjects.remove(customRoutePolyline!!)
                } else if (customRouteStartPoint != null) {
                    customRouteEndPoint = point
                    customRouteEndPin!!.geometry = point
                    customRoutePolyline = addRoute(customRouteStartPoint!!, customRouteEndPoint!!)
                } else {
                    customRouteStartPoint = point
                    customRouteStartPin!!.geometry = point
                }
            }
        }
        mapview.map.addInputListener(mapInputListener!!)
    }

    private fun addRoute(from : Point, to : Point): PolylineMapObject {
        val routePolyline = mapview.map.mapObjects.addPolyline()
        val router = Router()
        router.addWaypoint(from)
        router.addWaypoint(to)
        val listener = object : DrivingSession.DrivingRouteListener {
            override fun onDrivingRoutes(routes : MutableList<DrivingRoute>) {
                if (routes.size > 0) {
                    RouteHelper.updatePolyline(routePolyline, routes[0], RouteHelper.createDefaultJamStyle(), true)
                    var maneuverStyle = RouteHelper.createDefaultManeuverStyle()
                    var arrowManeuverStyle = maneuverStyle.arrow
                    maneuverStyle = ManeuverStyle(
                        ArrowManeuverStyle(
                            arrowManeuverStyle.getFillColor(),
                            arrowManeuverStyle.getOutlineColor(),
                            arrowManeuverStyle.getOutlineWidth(),
                            arrowManeuverStyle.getLength(),
                            arrowManeuverStyle.getTriangleHeight(),
                            true),
                        maneuverStyle.polygon)
                    RouteHelper.applyManeuverStyle(routePolyline, maneuverStyle)
                    routePolyline.strokeWidth = 8f
                    routePolyline.isInnerOutlineEnabled = true
                    routePolyline.outlineColor = Color.WHITE
                    routePolyline.outlineWidth = 2f
                    updatePolylineWithZlevels(routePolyline)
                }
            }
            override fun onDrivingRoutesError(error : Error) {
                LOGGER.warning(error.toString())
            }
        }
        router.requestRoute(DrivingOptions(), VehicleOptions(), listener)
        return routePolyline
    }

    private fun updatePolylineWithZlevels(routePolyline: PolylineMapObject) {
        val delta = 0.000001
        LOGGER.warning("routePolyline.geometry.points.size: " + routePolyline.geometry.points.size)
        val zlevels = mutableListOf<Int>()
        for (i in 0 until routePolyline.geometry.points.size - 1) {
            val segmentBegin = routePolyline.geometry.points[i]
            val segmentEnd = routePolyline.geometry.points[i+1]
            var zlevel = 0
            for (segment in roadsDump!!.roadSegments) {
                for (s in 0 until segment.geometry.points.size - 1) {
                    val dumpSegmentBegin = segment.geometry.points[s]
                    val dumpSegmentEnd = segment.geometry.points[s + 1]
                    if (abs(segmentBegin.latitude - dumpSegmentBegin.latitude) < delta &&
                        abs(segmentBegin.longitude - dumpSegmentBegin.longitude) < delta &&
                        abs(segmentEnd.latitude - dumpSegmentEnd.latitude) < delta &&
                        abs(segmentEnd.longitude - dumpSegmentEnd.longitude) < delta) {
                        zlevel = segment.properties.zLevel
                        break
                    }
                }
                if (zlevel != 0) break
            }
            zlevels.add(zlevel)
        }
        routePolyline.setZlevels(zlevels)
        // LOGGER.warning("ROUTE SEGMENTS Z-LEVEL: " + zlevels.joinToString(",") + " Arrows: " + routePolyline.arrows().size)
    }

    override fun onInitMap() {
        traffic = MapKitFactory.getInstance().createTrafficLayer(mapview.mapWindow)
        mapview.map.move(CameraPosition(Point(55.740483, 37.535461), 17.0f, 0.0f, 0.0f))

        routePolyline1 = addRoute(Point(55.740937, 37.537015), Point(55.738240, 37.536786))
        routePolyline2 = addRoute(Point(55.736530, 37.539631), Point(55.739998, 37.532920))
        routePolyline2!!.isVisible = false
        routePolyline3 = addRoute(Point(55.742687, 37.533918), Point(55.741030, 37.534457))
        routePolyline3!!.isVisible = false
    }
}
