@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.Attribution

class AttributionEncodable(it: Attribution) {
    class AuthorEncodable(it: Attribution.Author) {
        val name: String = it.name
        val uri: String? = it.uri
        val email: String? = it.email
    }

    class LinkEncodable(it: Attribution.Link) {
        val href: String = it.href
    }

    val author: AuthorEncodable? = it.author?.let { AuthorEncodable(it) }
    val link: LinkEncodable? = it.link?.let { LinkEncodable(it) }
}
