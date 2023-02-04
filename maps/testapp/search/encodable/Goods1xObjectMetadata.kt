@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Goods1xObjectMetadata

class Goods1xObjectMetadataEncodable(it: Goods1xObjectMetadata) {
    val goods: List<GoodsEncodable> = it.goods.map { GoodsEncodable(it) }
}

