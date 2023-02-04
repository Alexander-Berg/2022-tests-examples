@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BillboardAction
import com.yandex.mapkit.search.BillboardObjectMetadata
import com.yandex.mapkit.search.Creative
import com.yandex.mapkit.search.Disclaimer

class CreativeEncodable(it: Creative) {
    val id: String = it.id
    val type: String = it.type
    val properties: List<KeyValuePairEncodable> = it.properties.map { KeyValuePairEncodable(it) }
}

class DisclaimerEncodable(it: Disclaimer) {
    val text: String? = it.text
}

class BillboardActionEncodable(it: BillboardAction) {
    val type: String = it.type
    val properties: List<KeyValuePairEncodable> = it.properties.map { KeyValuePairEncodable(it) }
}

class BillboardObjectMetadataEncodable(it: BillboardObjectMetadata) {
    val placeId: String = it.placeId
    val title: String? = it.title
    val address: String? = it.address
    val actions: List<BillboardActionEncodable> = it.actions.map { BillboardActionEncodable(it) }
    val creatives: List<CreativeEncodable> = it.creatives.map { CreativeEncodable(it) }
    val disclaimers: List<DisclaimerEncodable> = it.disclaimers.map { DisclaimerEncodable(it) }
    val properties: List<KeyValuePairEncodable> = it.properties.map { KeyValuePairEncodable(it) }
    val logId: String? = it.logId
}
