package com.yandex.maps.testapp.directions_navigation

import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.PolylinePosition
import com.yandex.mapkit.geometry.geo.PolylineUtils
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.maps.testapp.R
import com.yandex.runtime.image.ImageProvider

private const val Z_INDEX = 100F

class RoadObjectsView(
    val mapObjectCollection: MapObjectCollection,
    imageProviderFactory: ImageProviderFactory
) : RouteListener, VisibilitySetting {

    override var visible = false
        set(value) {
            field = value
            update()
        }

    private val speedBumpIcon = imageProviderFactory.fromResource(R.drawable.speed_bump)
    private val railwayCrossingIcon = imageProviderFactory.fromResource(R.drawable.railway_crossing)
    private val placemarks = arrayListOf<PlacemarkMapObject>()

    private var route: DrivingRoute? = null

    override fun onCurrentRouteChanged(route: DrivingRoute?) {
        this.route = route
        update()
    }

    private fun update() {
        placemarks.forEach { mapObjectCollection.remove(it) }
        placemarks.clear()

        if (visible && route != null && !route!!.metadata.flags.predicted) {
            updateSpeedBumps()
            updateRailwayCrossings()
        }
    }

    private fun updateSpeedBumps() {
        route!!.speedBumps.forEach {
            addPlacemark(pointByPolylinePos(it.position), speedBumpIcon)
        }
    }

    private fun updateRailwayCrossings() {
        route!!.railwayCrossings.forEach {
            addPlacemark(pointByPolylinePos(it.position), railwayCrossingIcon)
        }
    }

    private fun pointByPolylinePos(position: PolylinePosition): Point {
        return PolylineUtils.pointByPolylinePosition(route!!.geometry, position)
    }

    private fun addPlacemark(point: Point, imageProvider: ImageProvider) {
        val placemarkMapObject = mapObjectCollection.addPlacemark(point)
        placemarkMapObject.setIcon(imageProvider)
        placemarkMapObject.zIndex = Z_INDEX
        placemarks.add(placemarkMapObject)
    }
}
