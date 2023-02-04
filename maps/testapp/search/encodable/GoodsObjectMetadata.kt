@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.GoodsObjectMetadata

class GoodsObjectMetadataEncodable(it: GoodsObjectMetadata) {
    val goods: List<GoodsEncodable> = it.goods.map { GoodsEncodable(it) }
}
