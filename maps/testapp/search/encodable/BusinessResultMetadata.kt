@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessResultMetadata

class BusinessResultMetadataEncodable(it: BusinessResultMetadata) {
    val categories: List<CategoryEncodable> = it.categories.map { CategoryEncodable(it) }
}
