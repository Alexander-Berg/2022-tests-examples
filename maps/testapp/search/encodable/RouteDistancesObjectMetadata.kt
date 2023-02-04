@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.*

class TransitInfoEncodable(it: TransitInfo) {
    val duration: LocalizedValueEncodable = LocalizedValueEncodable(it.duration)
    val transferCount: Int = it.transferCount
}

class TravelInfoEncodable(it: TravelInfo) {
    val duration: LocalizedValueEncodable = LocalizedValueEncodable(it.duration)
    val distance: LocalizedValueEncodable = LocalizedValueEncodable(it.distance)
}

class RelativeDistanceEncodable(it: RelativeDistance) {
    val driving: TravelInfoEncodable? = it.driving?.let { TravelInfoEncodable(it) }
    val walking: TravelInfoEncodable? = it.walking?.let { TravelInfoEncodable(it) }
}

class AbsoluteDistanceEncodable(it: AbsoluteDistance) {
    val driving: TravelInfoEncodable? = it.driving?.let { TravelInfoEncodable(it) }
    val walking: TravelInfoEncodable? = it.walking?.let { TravelInfoEncodable(it) }
    val transit: TransitInfoEncodable? = it.transit?.let { TransitInfoEncodable(it) }
}

class RouteDistancesObjectMetadataEncodable(it: RouteDistancesObjectMetadata) {
    val absolute: AbsoluteDistanceEncodable? = it.absolute?.let { AbsoluteDistanceEncodable(it) }
    val relative: RelativeDistanceEncodable? = it.relative?.let { RelativeDistanceEncodable(it) }
}
