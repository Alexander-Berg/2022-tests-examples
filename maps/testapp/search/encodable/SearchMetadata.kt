@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.DisplayType
import com.yandex.mapkit.search.SearchMetadata
import com.yandex.mapkit.search.Sort

class SearchMetadataEncodable(it: SearchMetadata) {
    val found: Int = it.found
    val displayType: DisplayType = it.displayType
    val boundingBox: BoundingBoxEncodable? = it.boundingBox?.let { BoundingBoxEncodable(it) }
    val sort: Sort? = it.sort
    val toponym: GeoObjectEncodable? = it.toponym?.let { GeoObjectEncodable(it) }
    val toponymResultMetadata: ToponymResultMetadataEncodable? = it.toponymResultMetadata?.let { ToponymResultMetadataEncodable(it) }
    val businessResultMetadata: BusinessResultMetadataEncodable? = it.businessResultMetadata?.let { BusinessResultMetadataEncodable(it) }
    val collectionResultMetadata: CollectionResultMetadataEncodable? = it.collectionResultMetadata?.let { CollectionResultMetadataEncodable(it) }
    val reqid: String = it.reqid
    val context: String = it.context
    val requestText: String = it.requestText
    val correctedRequestText: String? = it.correctedRequestText
    val requestBoundingBox: BoundingBoxEncodable? = it.requestBoundingBox?.let { BoundingBoxEncodable(it) }
}
