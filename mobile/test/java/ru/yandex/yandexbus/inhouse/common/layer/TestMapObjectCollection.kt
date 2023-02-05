package ru.yandex.yandexbus.inhouse.common.layer

import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.*
import com.yandex.runtime.image.AnimatedImageProvider
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.model.AnimatedModelProvider
import com.yandex.runtime.ui_view.ViewProvider

class TestMapObjectCollection(
    private val parent: MapObjectCollection? = null
) : TestMapObject(), MapObjectCollection {

    private val childTapListener = MapObjectTapListener { mapObject, point ->
        if (tapListeners.isNotEmpty()) {
            var isHandledByAny = false
            tapListeners.forEach {
                val isHandled = it.onMapObjectTap(mapObject, point)
                isHandledByAny = isHandledByAny || isHandled
            }
            isHandledByAny
        } else {
            false
        }
    }

    private val children: MutableList<TestPlacemarkMapObject> = mutableListOf()

    val placemarks: List<TestPlacemarkMapObject> = children

    override fun getParent() = parent ?: this

    override fun clear() = children.clear()

    override fun addPolyline(polyline: Polyline): PolylineMapObject {
        throw NotImplementedError()
    }

    override fun addCollection(): MapObjectCollection {
        return TestMapObjectCollection(this)
    }

    override fun remove(mapObject: MapObject) {
        children.remove(mapObject)
    }

    override fun traverse(mapObjectVisitor: MapObjectVisitor) {
        children.forEach { mapObjectVisitor.onPlacemarkVisited(it) }
    }

    override fun addEmptyPlacemark(point: Point): PlacemarkMapObject {
        return addTestPlacemark(point)
    }

    override fun addEmptyPlacemarks(points: List<Point>): List<PlacemarkMapObject> {
        return points
            .map { addEmptyPlacemark(it) }
    }

    override fun addPlacemark(point: Point): PlacemarkMapObject {
        return addTestPlacemark(point)
    }

    override fun addPlacemark(point: Point, image: ImageProvider): PlacemarkMapObject {
        return addTestPlacemark(point, image)
    }

    override fun addPlacemark(point: Point, image: ImageProvider, style: IconStyle): PlacemarkMapObject {
        return addTestPlacemark(point, image, style)
    }

    override fun addPlacemark(point: Point, view: ViewProvider): PlacemarkMapObject {
        return addTestPlacemark(point, view = view)
    }

    override fun addPlacemark(point: Point, view: ViewProvider, style: IconStyle): PlacemarkMapObject {
        return addTestPlacemark(point, style = style, view = view)
    }

    override fun addPlacemark(
        point: Point,
        animatedImage: AnimatedImageProvider,
        style: IconStyle
    ): PlacemarkMapObject {
        return addTestPlacemark(point, style = style)
    }

    override fun addPlacemark(
        point: Point,
        animatedModel: AnimatedModelProvider,
        style: ModelStyle
    ): PlacemarkMapObject {
        return addTestPlacemark(point)
    }

    override fun addPlacemarks(
        points: List<Point>,
        image: ImageProvider,
        style: IconStyle
    ): List<PlacemarkMapObject> {
        return points
            .map { addPlacemark(it, image, style) }
    }

    private fun addTestPlacemark(
        point: Point,
        image: ImageProvider? = null,
        style: IconStyle? = null,
        view: ViewProvider? = null
    ): PlacemarkMapObject {
        val placemarkMapObject = TestPlacemarkMapObject(this, point, image, style, view).apply {
            addTapListener(childTapListener)
        }
        children.add(placemarkMapObject)
        return placemarkMapObject
    }

    override fun addPolygon(polygon: Polygon): PolygonMapObject {
        throw NotImplementedError()
    }

    override fun addColoredPolyline(polyline: Polyline): ColoredPolylineMapObject {
        throw NotImplementedError()
    }

    override fun addColoredPolyline(): ColoredPolylineMapObject {
        throw NotImplementedError()
    }

    override fun addCircle(circle: Circle, strokeColor: Int, strokeWidth: Float, fillColor: Int): CircleMapObject {
        throw NotImplementedError()
    }

    override fun placemarksStyler(): PlacemarksStyler {
        throw NotImplementedError()
    }

    override fun addListener(collectionListener: MapObjectCollectionListener) {
        // no-op
    }

    override fun setDragListener(dragListener: MapObjectDragListener?) {
        // no-op
    }

    override fun setVisible(visible: Boolean, animation: Animation, onFinished: Callback?) {
        // no-op
    }

    override fun addClusterizedPlacemarkCollection(clusterListener: ClusterListener): ClusterizedPlacemarkCollection {
        throw NotImplementedError()
    }
}
