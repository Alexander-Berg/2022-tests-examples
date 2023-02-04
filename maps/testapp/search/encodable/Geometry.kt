@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.geometry.*

class DirectionEncodable(it: Direction) {
    val azimuth: Double = it.azimuth
    val tilt: Double = it.tilt
}

class SpanEncodable(it: Span) {
    val horizontalAngle: Double = it.horizontalAngle
    val verticalAngle: Double = it.verticalAngle
}

class PointEncodable(it: Point) {
    val latitude: Double = it.latitude
    val longitude: Double = it.longitude
}

class BoundingBoxEncodable(it: BoundingBox) {
    val southWest: PointEncodable = PointEncodable(it.southWest)
    val northEast: PointEncodable = PointEncodable(it.northEast)
}

class CircleEncodable(it: Circle) {
    val center: PointEncodable = PointEncodable(it.center)
    val radius: Float = it.radius
}

class SegmentEncodable(it: Segment) {
    val startPoint: PointEncodable = PointEncodable(it.startPoint)
    val endPoint: PointEncodable = PointEncodable(it.endPoint)
}

class PolylineEncodable(it: Polyline) {
    val points: List<PointEncodable> = it.points.map { PointEncodable(it) }
}

class PolylinePositionEncodable(it: PolylinePosition) {
    val segmentIndex: Int = it.segmentIndex
    val segmentPosition: Double = it.segmentPosition
}

class SubpolylineEncodable(it: Subpolyline) {
    val begin: PolylinePositionEncodable = PolylinePositionEncodable(it.begin)
    val end: PolylinePositionEncodable = PolylinePositionEncodable(it.end)
}

class LinearRingEncodable(it: LinearRing) {
    val points: List<PointEncodable> = it.points.map { PointEncodable(it) }
}

class PolygonEncodable(it: Polygon) {
    val outerRing: LinearRingEncodable = LinearRingEncodable(it.outerRing)
    val innerRings: List<LinearRingEncodable> = it.innerRings.map { LinearRingEncodable(it) }
}

class MultiPolygonEncodable(it: MultiPolygon) {
    val polygons: List<PolygonEncodable> = it.polygons.map { PolygonEncodable(it) }
}

class GeometryEncodable(it: Geometry) {
    val point: PointEncodable? = it.point?.let { PointEncodable(it) }
    val polyline: PolylineEncodable? = it.polyline?.let { PolylineEncodable(it) }
    val polygon: PolygonEncodable? = it.polygon?.let { PolygonEncodable(it) }
    val multiPolygon: MultiPolygonEncodable? = it.multiPolygon?.let { MultiPolygonEncodable(it) }
    val boundingBox: BoundingBoxEncodable? = it.boundingBox?.let { BoundingBoxEncodable(it) }
    val circle: CircleEncodable? = it.circle?.let { CircleEncodable(it) }
}

