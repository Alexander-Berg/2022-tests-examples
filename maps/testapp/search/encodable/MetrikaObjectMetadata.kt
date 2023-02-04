@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.search.Goals
import com.yandex.mapkit.search.MetrikaObjectMetadata

class GoalsEncodable(it: Goals) {
    val call: String? = it.call
    val route: String? = it.route
    val cta: String? = it.cta
}

class MetrikaObjectMetadataEncodable(it: MetrikaObjectMetadata) {
    val counter: String? = it.counter
    val goals: GoalsEncodable? = it.goals?.let { GoalsEncodable(it) }
}
