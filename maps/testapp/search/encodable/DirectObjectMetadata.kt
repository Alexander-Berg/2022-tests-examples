@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.ContactInfo
import com.yandex.mapkit.search.Counter
import com.yandex.mapkit.search.DirectObjectMetadata

class CounterEncodable(it: Counter) {
    val type: String = it.type
    val url: String = it.url
}

class ContactInfoEncodable(it: ContactInfo) {
    val companyName: String = it.companyName
    val address: String? = it.address
    val phone: String? = it.phone
    val email: String? = it.email
    val hours: String? = it.hours
}

class DirectObjectMetadataEncodable(it: DirectObjectMetadata) {
    val title: String = it.title
    val text: String = it.text
    val extra: String? = it.extra
    val disclaimers: List<String> = it.disclaimers
    val domain: String? = it.domain
    val url: String = it.url
    val counters: List<CounterEncodable> = it.counters.map { CounterEncodable(it) }
    val links: List<AtomLinkEncodable> = it.links.map { AtomLinkEncodable(it) }
    val contactInfo: ContactInfoEncodable? = it.contactInfo?.let { ContactInfoEncodable(it) }
}

