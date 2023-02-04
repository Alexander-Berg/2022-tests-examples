package com.yandex.maps.testapp.search

import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.RelatedAdvertsObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.RoutePointMetadata
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchObjectMetadata
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.mapkit.uri.UriObjectMetadata
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.common.internal.point_context.decodeBase64
import com.yandex.runtime.Error

abstract class CardSearchActivity : TestAppActivity() {
    companion object {
        var intentGeoObject = GeoObject()
    }
    protected val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val searchBox by lazy { find<SearchBox>(R.id.card_search_view) }
    private val searchResults by lazy {find<SectionedListView>(R.id.card_search_results) }
    private var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_base_card_search)
        window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        if (intent.getBooleanExtra("useIntentGeoObject", false)) {
            fillInfo(null, intentGeoObject, searchResults)
            searchBox.hide()
        } else {
            searchBox.text = defaultSearchBoxText()
            findViewById<View>(R.id.search_options).hide()
            searchBox.setListener(searchBoxListener)
            searchBoxListener.onSubmit(searchBox.text)
        }
    }

    override fun onStopImpl(){}
    override fun onStartImpl(){}

    protected fun location() = Geometry.fromPoint(Point(55.75, 37.62))

    protected open val searchBoxText: String get() = searchBox.text!!

    protected abstract fun defaultSearchBoxText(): String

    protected abstract fun createSession(): Session

    protected abstract fun fillObjectCard(geoObject: GeoObject, searchResults: SectionedListView)

    protected fun addRoutePoint(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<RoutePointMetadata>() ?: return
        searchResults.addSection("snippet | route point",
            listOf(ItemWithDetails("Route point context", metadata.routePointContext),
                   ItemWithDetails("Decoded route point context (internal use only!)",
                                   decodeBase64(metadata.routePointContext) ?: "<decoding failed>")))

        searchResults.addSection("snippet | route point | entrances",
            metadata.entrances.map {
                ItemWithDetails(it.name ?: "",
                                toString(it.point) + " direction: %.0f".format(it.direction?.azimuth))
            }
        )

        searchResults.addSection("snippet | route point | driving arrival points",
            metadata.drivingArrivalPoints.map {
                ItemWithDetails(it.id,
                                toString(it.anchor) + "; " + it.tags.joinToString { it } + "; " +
                                    (it.price?.text ?: "") + "; " + (it.walkingTime?.text ?: "") + "; "
                                    + (it.description ?: ""))
            }
        )
    }

    private fun fillInfo(response: Response?, geoObject: GeoObject, searchResults: SectionedListView) {
        addGeneralInfo(response, geoObject, searchResults)
        addInternalInfo(geoObject, searchResults)
        addAttributionInfo(geoObject, searchResults)
        addUriInfo(geoObject, searchResults)
        addRelatedAdvertsInfo(geoObject, searchResults)

        fillObjectCard(geoObject, searchResults)
    }

    private fun addGeneralInfo(response: Response?, geoObject: GeoObject, searchResults: SectionedListView) {
        val items = mutableListOf(
                ItemWithDetails(geoObject.name, "name"),
                ItemWithDetails(geoObject.descriptionText, "description")
        )
        response?.let {
            items.add(ItemWithDetails(it.metadata.reqid, "reqid"))
        }
        searchResults.addSection("General", items)
    }

    private fun addAttributionInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        searchResults.addSection("Attribution references", geoObject.aref.map {
            ItemWithDetails(it)
        })
        searchResults.addSection("Attributions", geoObject.attributionMap.map {
            ItemWithDetails(it.key, it.value.author?.name ?: "")
        })
    }

    private fun addUriInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<UriObjectMetadata>() ?: return
        searchResults.addSection("URIs", metadata.uris.map { ItemWithDetails(it.value) })
    }

    private fun addRelatedAdvertsInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<RelatedAdvertsObjectMetadata>() ?: return
        searchResults.addSection("Related adverts on map", metadata.placesOnMap.map {
            ItemWithDetails(it.name, it.address ?: "")
        })
        searchResults.addSection("Related adverts on card", metadata.placesOnCard.map {
            ItemWithDetails(it.name, it.address ?: "")
        })
    }

    private fun addInternalInfo(geoObject: GeoObject, searchResults: SectionedListView) {
        val metadata = geoObject.metadata<SearchObjectMetadata>() ?: return
        searchResults.addSection("Internal", ItemWithDetails(metadata.logId, "logId"))
    }

    protected val searchListener: SearchListener = object : SearchListener {
        override fun onSearchError(error: Error) {
            showErrorMessage(this@CardSearchActivity, error)
            searchBox.setProgress(false)
        }

        override fun onSearchResponse(response: Response) {
            searchBox.setProgress(false)
            informOnEmptyResults(this@CardSearchActivity, response.results)
            if (response.collection.children.isEmpty()) {
                return
            }
            val geoObject = response.collection.children[0].obj

            fillInfo(response, geoObject!!, searchResults)
        }
    }

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) {
            searchBox.setProgress(true)
            searchResults.clearItems()

            session?.cancel()
            session = createSession()
        }
    }
}
