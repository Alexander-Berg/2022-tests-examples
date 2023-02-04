@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.SubtitleItem
import com.yandex.mapkit.search.SubtitleMetadata

class SubtitleItemEncodable(it: SubtitleItem) {
    val type: String = it.type
    val text: String? = it.text
    val properties: List<KeyValuePairEncodable> = it.properties.map { KeyValuePairEncodable(it) }
}

class SubtitleMetadataEncodable(it: SubtitleMetadata) {
    val subtitleItems: List<SubtitleItemEncodable> = it.subtitleItems.map { SubtitleItemEncodable(it) }
    val serpSubtitleItems: List<SubtitleItemEncodable> = it.serpSubtitleItems.map { SubtitleItemEncodable(it) }
}
