package com.yandex.maps.testapp.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.search.encodable.CollectionEntryMetadataEncodable
import com.yandex.maps.testapp.search.encodable.CollectionResultMetadataEncodable
import com.yandex.maps.testapp.search.test.toPrettyJson
import com.yandex.runtime.Error

class CollectionSearchActivity: TestAppActivity() {
    private val defaultSearchBoxText = "ymapsbm1://collection?id=gde-est-pelmeni-v-moskve"

    private val searchBox by lazy { find<SearchBox>(R.id.collection_search_box) }
    private val textView by lazy { find<TextView>(R.id.collection_search_text) }
    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)
    private var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_collection_search)
        window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        find<View>(R.id.search_options).hide()

        searchBox.text = defaultSearchBoxText
        searchBox.setListener(searchBoxListener)
        searchBoxListener.onSubmit(searchBox.text)
    }

    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private val searchListener: SearchListener = object : SearchListener {
        override fun onSearchError(error: Error) {
            showErrorMessage(this@CollectionSearchActivity, error)
            searchBox.setProgress(false)
        }

        override fun onSearchResponse(response: Response) {
            searchBox.setProgress(false)
            informOnEmptyResults(this@CollectionSearchActivity, response.results)
            if (response.collection.children.isEmpty()) {
                return
            }
            setText(response)
        }
    }

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) {
            searchBox.setProgress(true)
            textView.text = ""

            session?.cancel()
            session = searchManager.searchByURI(
                searchBox.text,
                SearchOptions(),
                searchListener
            )
        }
    }

    @SuppressLint("SetTextI18n")
    fun setText(response: Response) {
        val metadata = response.metadata.collectionResultMetadata

        textView.text = listOf(toPrettyJson(
                metadata?.let { CollectionResultMetadataEncodable(it) }
            ))
            .union(response.collection.children
                .mapNotNull { it.obj?.metadata<CollectionEntryMetadata>() }
                .map { toPrettyJson(CollectionEntryMetadataEncodable(it)) }
            )
            .joinToString(separator = "\n")
    }
}
