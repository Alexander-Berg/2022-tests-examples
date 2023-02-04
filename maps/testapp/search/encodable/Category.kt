@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Category

class CategoryEncodable(it: Category) {
    val name: String = it.name
    val categoryClass: String? = it.categoryClass
    val tags: List<String> = it.tags
}
