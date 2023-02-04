@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.PlaceInfo

class PlaceInfoEncodable(it: PlaceInfo) {
    val name: String = it.name
    val uri: String? = it.uri
    val photoUrlTemplate: String? = it.photoUrlTemplate
    val logId: String? = it.logId
    val point: PointEncodable? = it.point?.let { PointEncodable(it) }
    val category: String? = it.category
    val shortName: String? = it.shortName
    val rating: Float? = it.rating
    val workingHours: WorkingHoursEncodable? = it.workingHours?.let { WorkingHoursEncodable(it) }
    val address: String? = it.address
    val tag: List<String> = it.tag
}
