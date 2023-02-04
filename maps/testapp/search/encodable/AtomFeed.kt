@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.atom.AtomEntry
import com.yandex.mapkit.atom.AtomFeed
import com.yandex.mapkit.atom.Author
import com.yandex.mapkit.atom.Link

class AtomAuthorEncodable(it: Author) {
    val name: String = it.name
    val uri: String? = it.uri
    val email: String? = it.email
}

class AtomLinkEncodable(it: Link) {
    val href: String = it.href
    val rel: String? = it.rel
    val type: String? = it.type
}

class AtomEntryEncodable(it: AtomEntry) {
    val id: String? = it.id
    val updateTime: String? = it.updateTime
    val author: AtomAuthorEncodable = AtomAuthorEncodable(it.author)
    val attribution: AttributionEncodable? = it.attribution?.let { AttributionEncodable(it) }
    val links: List<AtomLinkEncodable> = it.links.map { AtomLinkEncodable(it) }
}

class FeedEncodable(it: AtomFeed) {
    val nextpage: String? = it.nextpage
    val links: List<AtomLinkEncodable> = it.links.map { AtomLinkEncodable(it) }
}

