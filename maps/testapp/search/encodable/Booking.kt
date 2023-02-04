@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BookingLink
import com.yandex.mapkit.search.BookingOffer
import com.yandex.mapkit.search.BookingParams
import com.yandex.mapkit.search.BookingResponse

class BookingParamsEncodable(it: BookingParams) {
    val checkIn: TimeEncodable = TimeEncodable(it.checkIn)
    val nights: Int = it.nights
    val persons: Int = it.persons
}

class BookingLinkEncodable(it: BookingLink) {
    val type: String = it.type
    val uri: String = it.uri
}

class BookingOfferEncodable(it: BookingOffer) {
    val partnerName: String = it.partnerName
    val bookingLinks: List<BookingLinkEncodable> = it.bookingLinks.map { BookingLinkEncodable(it) }
    val favicon: ImageEncodable? = it.favicon?.let { ImageEncodable(it) }
    val price: MoneyEncodable? = it.price?.let { MoneyEncodable(it) }
}

class BookingResponseEncodable(it: BookingResponse) {
    val params: BookingParamsEncodable? = it.params?.let { BookingParamsEncodable(it) }
    val offers: List<BookingOfferEncodable> = it.offers.map { BookingOfferEncodable(it) }
}
