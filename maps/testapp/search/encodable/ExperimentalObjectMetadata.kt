@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.ExperimentalMetadata
import com.yandex.mapkit.search.ExperimentalStorage

class ExperimentalStorageEncodable(it: ExperimentalStorage) {
    class ItemEncodable(it: ExperimentalStorage.Item) {
        val key: String = it.key
        val value: String = it.value
    }

    val items: List<ItemEncodable> = it.items.map { ItemEncodable(it) }
}

class ExperimentalMetadataEncodable(it: ExperimentalMetadata) {
    val experimentalStorage = it.experimentalStorage?.let {
        ExperimentalStorageEncodable(it)
    }
}

