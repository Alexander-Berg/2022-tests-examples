package com.yandex.maps.testapp.search

import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.ExtendedSearchType
import com.yandex.mapkit.search.TransitObjectMetadata

class TransitSearchActivity : CardSearchActivity() {
    override fun defaultSearchBoxText() = "автобус 2"

    override fun createSession() =
        searchManager.submit(
            searchBoxText,
            location(),
            makeSearchOptions(null, ExtendedSearchType.TRANSIT.value).setOrigin("mobile-maps-searchnearby-text"),
            searchListener
        )

    override fun fillObjectCard(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<TransitObjectMetadata>() ?: return
        searchResults.addSection("details",
            ItemWithDetails(metadata.routeId, "route Id"),
            ItemWithDetails(metadata.types.joinToString(";"), "transit types")
        )
    }
}


