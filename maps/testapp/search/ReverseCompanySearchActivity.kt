package com.yandex.maps.testapp.search

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Snippet

class ReverseCompanySearchActivity: CompanySearchActivity() {
    override fun defaultSearchBoxText() = "55.733771,37.587937"

    override fun createSession(): Session {
        val searchOptions = makeSearchOptions(SearchType.BIZ.value, null)
        searchOptions.snippets = (Snippet.BUSINESS_RATING1X.value
            or Snippet.MASS_TRANSIT.value
            or Snippet.PANORAMAS.value
            or Snippet.PHOTOS.value
            or Snippet.SUBTITLE.value)

        val (lat, lon) = searchBoxText.split(",").map { it.toDouble() }
        return searchManager.submit(Point(lat, lon), null, searchOptions, searchListener)
    }
}
