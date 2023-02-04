@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Goods
import com.yandex.mapkit.search.PhotoLink

class PhotoLinkEncodable(it: PhotoLink) {
    val type: String = it.type
    val uri: String = it.uri
}

class GoodsEncodable(it: Goods) {
    val name: String = it.name
    val description: String? = it.description
    val price: MoneyEncodable? = it.price?.let { MoneyEncodable(it) }
    val unit: String? = it.unit
    val links: List<PhotoLinkEncodable> = it.links.map { PhotoLinkEncodable(it) }
    val tags: List<String> = it.tags
}

