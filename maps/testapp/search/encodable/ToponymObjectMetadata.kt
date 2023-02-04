@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Precision
import com.yandex.mapkit.search.ToponymObjectMetadata

class ToponymObjectMetadataEncodable(it: ToponymObjectMetadata) {
    val address: AddressEncodable = AddressEncodable(it.address)
    val precision: Precision? = it.precision
    val formerName: String? = it.formerName
    val balloonPoint: PointEncodable = PointEncodable(it.balloonPoint)
    val id: String? = it.id
}
