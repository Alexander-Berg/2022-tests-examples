@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.DrivingArrivalPoint
import com.yandex.mapkit.search.Entrance
import com.yandex.mapkit.search.RoutePointMetadata

class EntranceEncodable(it: Entrance) {
    val name: String? = it.name
    val point: PointEncodable = PointEncodable(it.point)
    val direction: DirectionEncodable? = it.direction?.let { DirectionEncodable(it) }
}

class DrivingArrivalPointEncodable(it: DrivingArrivalPoint) {
    val id: String? = it.id
    val anchor: PointEncodable = PointEncodable(it.anchor)
    val geometry: GeometryEncodable? = it.geometry?.let { GeometryEncodable(it) }
    val walkingTime: LocalizedValueEncodable? = it.walkingTime?.let { LocalizedValueEncodable(it) }
    val price: MoneyEncodable? = it.price?.let { MoneyEncodable(it) }
    val description: String? = it.description
    val tags: List<String> = it.tags
}

class RoutePointMetadataEncodable(it: RoutePointMetadata) {
    val routePointContext: String = it.routePointContext
    val entrances: List<EntranceEncodable> = it.entrances.map { EntranceEncodable(it) }
    val drivingArrivalPoints: List<DrivingArrivalPointEncodable> = it.drivingArrivalPoints.map { DrivingArrivalPointEncodable(it) }
}
