@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.RelatedPlacesObjectMetadata

class RelatedPlacesObjectMetadataEncodable(it: RelatedPlacesObjectMetadata) {
    val similarPlaces: List<PlaceInfoEncodable> = it.similarPlaces.map { PlaceInfoEncodable(it) }
}
