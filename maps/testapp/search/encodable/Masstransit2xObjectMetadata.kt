@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.MassTransit2xObjectMetadata
import com.yandex.mapkit.search.NearbyStop

class NearbyStopEncodable(it: NearbyStop) {
    class StyleEncodable(it: NearbyStop.Style) {
        val color: Int? = it.color
    }

    class LineEncodable(it: NearbyStop.Line) {
        val id: String = it.id
        val name: String = it.name
        val style: StyleEncodable? = it.style?.let { StyleEncodable(it) }
        val vehicleTypes: List<String> = it.vehicleTypes
    }

    class LineAtStopEncodable(it: NearbyStop.LineAtStop) {
        val line: LineEncodable = LineEncodable(it.line)
    }

    class StopEncodable(it: NearbyStop.Stop) {
        val id: String = it.id
        val name: String = it.name
    }

    val stop: StopEncodable = StopEncodable(it.stop)
    val point: PointEncodable = PointEncodable(it.point)
    val distance: LocalizedValueEncodable = LocalizedValueEncodable(it.distance)
    val linesAtStop: List<LineAtStopEncodable> = it.linesAtStop.map { LineAtStopEncodable(it) }
}

class MassTransit2xObjectMetadataEncodable(it: MassTransit2xObjectMetadata) {
    val stops: List<NearbyStopEncodable> = it.stops.map { NearbyStopEncodable(it) }
}

