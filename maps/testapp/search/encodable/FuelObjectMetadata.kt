@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.FuelMetadata
import com.yandex.mapkit.search.FuelType

class FuelTypeEncodable(it: FuelType) {
    val name: String? = it.name
    val price: MoneyEncodable? = it.price?.let { MoneyEncodable(it) }
}

class FuelMetadataEncodable(it: FuelMetadata) {
    val timestamp: Long? = it.timestamp
    val fuels: List<FuelTypeEncodable> = it.fuels.map { FuelTypeEncodable(it) }
    val attribution: AttributionEncodable? = it.attribution?.let { AttributionEncodable(it) }
}

