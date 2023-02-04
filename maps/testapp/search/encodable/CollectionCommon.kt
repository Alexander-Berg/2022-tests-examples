@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Author
import com.yandex.mapkit.search.Collection

class AuthorEncodable(it: Author) {
    val name: String = it.name
    val description: String? = it.description
    val favicon: ImageEncodable? = it.favicon?.let { ImageEncodable(it) }
    val uri: String? = it.uri
}

class CollectionEncodable(it: Collection) {
    val uri: String = it.uri
    val title: String? = it.title
    val description: String? = it.description
    val image: ImageEncodable? = it.image?.let { ImageEncodable(it) }
    val itemCount: Int? = it.itemCount
    val rubric: String? = it.rubric
    val author: AuthorEncodable? = it.author?.let { AuthorEncodable(it) }
    val seoname: String? = it.seoname
}

