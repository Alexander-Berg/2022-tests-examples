@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Panorama
import com.yandex.mapkit.search.PanoramasObjectMetadata

class PanoramaEncodable(it: Panorama) {
    val id: String = it.id
    val direction: DirectionEncodable = DirectionEncodable(it.direction)
    val span: SpanEncodable = SpanEncodable(it.span)
    val point: PointEncodable = PointEncodable(it.point)
}

class PanoramasObjectMetadataEncodable(it: PanoramasObjectMetadata) {
    val panoramas: List<PanoramaEncodable> = it.panoramas.map { PanoramaEncodable(it) }
}
