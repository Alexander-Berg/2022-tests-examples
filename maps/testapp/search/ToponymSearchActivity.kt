package com.yandex.maps.testapp.search

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.*

class ToponymSearchActivity : CardSearchActivity() {
    override fun defaultSearchBoxText() = "Moscow, Lva Tolstogo"

    override fun createSession() = searchManager.resolveURI(
        makeURI(),
        makeSearchOptions(SearchType.GEO.value, null)
            .setSnippets(Snippet.MASS_TRANSIT.value or Snippet.ROUTE_POINT.value),
        searchListener)

    override fun fillObjectCard(geoObject: GeoObject, searchResults: SectionedListView) {
        addToponymInfo(geoObject, searchResults)
        addMasstransitInfo(geoObject, searchResults)
        addRoutePoint(geoObject, searchResults)
    }

    private fun addToponymInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<ToponymObjectMetadata>() ?: return
        metadata.formerName?.let { searchResults.addSection("former name", it) }
        metadata.address.postalCode?.let { searchResults.addSection("postal code", it) }
        searchResults.addSection("components",
            metadata.address.components.map {
                ItemWithDetails(it.name, it.kinds.firstOrNull()?.toString() ?: "")
            }
        )
    }

    private fun addMasstransitInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<MassTransit1xObjectMetadata>() ?: return
        searchResults.addSection("masstransit", metadata.stops.map{
            ItemWithDetails(it.name, "Distance: " + it.distance.text + "; Line: "
                + (it.line?.name ?: "no data"))
        })
    }

    private fun makeURI(): String {
        val p = location().point!!
        return "ymapsbm1://geo?text=$searchBoxText&ll=${p.latitude},${p.longitude}&spn=1,1"
    }
}
