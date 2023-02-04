@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.SuggestItem

class SuggestItemEncodable(it: SuggestItem) {
    val type: SuggestItem.Type = it.type
    val title: SpannableStringEncodable = SpannableStringEncodable(it.title)
    val subtitle: SpannableStringEncodable? = it.subtitle?.let { SpannableStringEncodable(it) }
    val tags: List<String> = it.tags
    val searchText: String = it.searchText
    val displayText: String? = it.displayText
    val uri: String? = it.uri
    val distance: LocalizedValueEncodable? = it.distance?.let { LocalizedValueEncodable(it) }
    val isPersonal: Boolean = it.isPersonal
    val action: SuggestItem.Action = it.action
    val logId: String? = it.logId
    val isOffline: Boolean = it.isOffline
    val isWordItem: Boolean = it.isWordItem
    val properties: List<KeyValuePairEncodable> = it.properties.map { KeyValuePairEncodable(it) }
}

