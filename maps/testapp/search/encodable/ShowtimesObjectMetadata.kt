@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Showtime
import com.yandex.mapkit.search.ShowtimesObjectMetadata

class ShowtimeEncodable(it: Showtime) {
    val startTime: TimeEncodable = TimeEncodable(it.startTime)
    val price: MoneyEncodable? = it.price?.let { MoneyEncodable(it) }
    val ticketId: String? = it.ticketId
}

class ShowtimesObjectMetadataEncodable(it: ShowtimesObjectMetadata) {
    val title: String = it.title
    val showtimes: List<ShowtimeEncodable> = it.showtimes.map { ShowtimeEncodable(it) }
}
