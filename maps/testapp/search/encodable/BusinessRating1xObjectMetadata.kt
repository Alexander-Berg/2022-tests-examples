@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.BusinessRating1xObjectMetadata

class BusinessRating1xObjectMetadataEncodable(it: BusinessRating1xObjectMetadata) {
    val ratings: Int = it.ratings
    val reviews: Int = it.reviews
    val score: Float? = it.score
}
