@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Line
import com.yandex.mapkit.search.MassTransit1xObjectMetadata
import com.yandex.mapkit.search.Stop

class LineEncodable(it: Line) {
    val name: String = it.name
}

class StopEncodable(it: Stop) {
    class StyleEncodable(it: Stop.Style) {
        val color: Int = it.color
    }

    val name: String = it.name
    val distance: LocalizedValueEncodable = LocalizedValueEncodable(it.distance)
    val style: StyleEncodable = StyleEncodable(it.style)
    val point: PointEncodable = PointEncodable(it.point)
    val stopId: String? = it.stopId
    val line: LineEncodable? = it.line?.let { LineEncodable(it) }
}

class MassTransit1xObjectMetadataEncodable(it: MassTransit1xObjectMetadata) {
    val stops: List<StopEncodable> = it.stops.map { StopEncodable(it) }
}

