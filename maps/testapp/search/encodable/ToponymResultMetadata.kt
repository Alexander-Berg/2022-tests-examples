@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.ToponymResultMetadata

class ToponymResultMetadataEncodable(it: ToponymResultMetadata) {
    class ResponseInfoEncodable(it: ToponymResultMetadata.ResponseInfo) {
        val mode: ToponymResultMetadata.SearchMode = it.mode
        val accuracy: Double? = it.accuracy
    }

    val found: Int = it.found
    val responseInfo: ResponseInfoEncodable? = it.responseInfo?.let { ResponseInfoEncodable(it) }
    val reversePoint: PointEncodable? = it.reversePoint?.let { PointEncodable(it) }
}
