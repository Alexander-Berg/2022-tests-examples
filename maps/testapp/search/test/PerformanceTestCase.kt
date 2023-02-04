package com.yandex.maps.testapp.search

import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.search.SearchType

data class PerformanceTestCase(
    val query : String,
    val point : Geometry,
    val region : String,
    val shouldAddToStats : Boolean,
    val searchTypes : Int = SearchType.GEO.value or SearchType.BIZ.value,
    var start : Long = 0,
    var finish : Long = 0
) {
    fun duration() = finish - start
    fun isReverseSearchQuery() = query.isEmpty()
}