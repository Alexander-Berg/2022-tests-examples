package com.yandex.maps.testapp.search

import android.os.Bundle
import android.view.View
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.runtime.Error

class ReverseSearchActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val searchResults by lazy { find<SearchResultsBox>(R.id.search_results) }
    private var session: Session? = null

    private val searchListener = object : SearchListener {
        override fun onSearchError(error: Error) = showErrorMessage(this@ReverseSearchActivity, error)
        override fun onSearchResponse(response: Response) {
            findViewById<View>(R.id.search_in_progress).hide()
            searchResults.append(response.collection.children)
            if (session!!.hasNextPage()) {
                session!!.fetchNextPage(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reverse_search)
        session = searchManager.submit(
            Point(intent.extras!!.getDouble("lat"), intent.extras!!.getDouble("lon")),
            intent.extras!!.getInt("zoom"),
            makeSearchOptions(SearchType.GEO.value, null),
            searchListener
        )
    }
}
