@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.RelatedAdvertsObjectMetadata

class RelatedAdvertsObjectMetadataEncodable(it: RelatedAdvertsObjectMetadata) {
    val placesOnMap: List<PlaceInfoEncodable> = it.placesOnMap.map { PlaceInfoEncodable(it) }
    val placesOnCard: List<PlaceInfoEncodable> = it.placesOnCard.map { PlaceInfoEncodable(it) }
}
