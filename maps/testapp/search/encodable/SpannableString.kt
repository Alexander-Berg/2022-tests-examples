@file:Suppress("unused")

package com.yandex.maps.testapp.search.encodable

import com.yandex.mapkit.SpannableString

class SpannableStringEncodable(it: SpannableString) {
    class SpanEncodable(it: SpannableString.Span) {
        val begin: Int = it.begin
        val end: Int = it.end
    }
    val text: String = it.text
    val spans: List<SpanEncodable> = it.spans.map { SpanEncodable(it) }
}

