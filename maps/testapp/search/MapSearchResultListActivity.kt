package com.yandex.maps.testapp.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.search_layer.RequestType
import com.yandex.mapkit.search.search_layer.SearchLayer
import com.yandex.mapkit.search.search_layer.SearchResultListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.runtime.Error

class MapSearchResultListActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private val searchResults by lazy { find<SearchResultsBox>(R.id.map_search_results_list_box) }
    private val searchResultListener = object: SearchResultListener {
        override fun onPresentedResultsUpdate() {}

        override fun onSearchError(error: Error, requestType: RequestType) {
            showErrorMessage(this@MapSearchResultListActivity, error)
        }

        override fun onSearchStart(requestType: RequestType) {}

        override fun onAllResultsClear() {}

        override fun onSearchSuccess(requestType: RequestType) {
            fillSearchResults(MapSearchActivity.searchLayerVar)
        }
    }

    private fun fillSearchResults(searchLayer: SearchLayer?) {
        searchResults.clear()
        searchLayer?.searchResultsList?.forEach { searchResults.append(it) }
        searchResults.hasMore = searchLayer?.hasNextPage() ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_map_search_result_list)

        val searchLayer = MapSearchActivity.searchLayerVar
        searchLayer?.addSearchResultListener(searchResultListener)
        fillSearchResults(searchLayer)
        searchResults.fetchMoreFunction = fun() { searchLayer?.fetchNextPage() }

        searchResults.onItemClickFunction = fun(geoObjectId: String, geoObject: GeoObject) {
            val metadata = geoObject.metadata<CollectionObjectMetadata>()
            if (metadata != null) {
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("collectionUri", metadata.collection.uri)
                )
            } else {
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("selectedGeoObjectId", geoObjectId)
                )
            }
            finish()
        }
    }
}
