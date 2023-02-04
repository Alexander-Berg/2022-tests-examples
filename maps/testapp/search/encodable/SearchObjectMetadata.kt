@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.SearchObjectMetadata

class SearchObjectMetadataEncodable(it: SearchObjectMetadata) {
    val logId: String? = it.logId
    val reqId: String? = it.reqId
}
