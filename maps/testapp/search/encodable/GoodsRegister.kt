@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.GoodsCategory
import com.yandex.mapkit.search.GoodsRegister

class GoodsCategoryEncodable(it: GoodsCategory) {
    val name: String? = it.name
    val goods: List<GoodsEncodable> = it.goods.map { GoodsEncodable(it) }
}

class GoodsRegisterEncodable(it: GoodsRegister) {
    val categories: List<GoodsCategoryEncodable> = it.categories.map { GoodsCategoryEncodable(it) }
    val tags: List<String> = it.tags
}
