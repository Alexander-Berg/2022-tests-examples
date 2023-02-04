package com.yandex.maps.testapp.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.search.encodable.ResponseEncodable
import com.yandex.maps.testapp.search.test.toPrettyJson
import com.yandex.runtime.Error

class JsonSearchActivity: TestAppActivity() {
    private val defaultSearchBoxText = "москва"

    private val scrollView by lazy { find<ScrollView>(R.id.json_search_list_scroller) }
    private val searchBox by lazy { find<SearchBox>(R.id.json_search_box) }
    private val textView by lazy { find<TextView>(R.id.json_search_text) }
    private val editText by lazy { find<EditText>(R.id.json_search_bar) }
    private val searchButton by lazy { find<ImageButton>(R.id.json_search_button) }
    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private var session: Session? = null
    private var searchTextIndex: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_json_search)
        window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        find<View>(R.id.search_options).hide()

        searchBox.text = defaultSearchBoxText
        searchBox.setListener(searchBoxListener)
        searchButton.setOnClickListener(searchButtonListener)
        editText.addTextChangedListener(textWatcher)
        searchBoxListener.onSubmit(searchBox.text)
    }

    override fun onStopImpl(){}
    override fun onStartImpl(){}

    private fun resetSearchBar() {
        searchTextIndex = null
        searchButton.setImageResource(android.R.drawable.ic_menu_search)
    }

    private val searchListener: SearchListener = object : SearchListener {
        override fun onSearchError(error: Error) {
            showErrorMessage(this@JsonSearchActivity, error)
            searchBox.setProgress(false)
        }

        override fun onSearchResponse(response: Response) {
            searchBox.setProgress(false)
            informOnEmptyResults(this@JsonSearchActivity, response.results)
            textView.text = toPrettyJson(ResponseEncodable(response))
        }
    }

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) {
            searchBox.setProgress(true)
            textView.text = ""
            resetSearchBar()

            session?.cancel()
            session = searchManager.submit(
                searchBox.text,
                Geometry.fromPoint(Point(55.7, 37.5)),
                SearchOptions().setSnippets(ALL_SNIPPETS),
                searchListener
            )
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {}
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            resetSearchBar()
        }
    }

    private val searchButtonListener = View.OnClickListener {
        val needle = editText.text.toString()
        val haystack = textView.text.toString()
        val index = haystack.indexOf(
            needle,
            searchTextIndex ?: 0,
            ignoreCase = true
        )
        if (index != -1) {
            searchTextIndex = index + needle.length
            searchButton.setImageResource(android.R.drawable.ic_media_next)
            val y = textView.layout.run {
                getLineTop(getLineForOffset(index))
            }
            scrollView.scrollTo(0, y)
            textView.text = highlightMatches(needle, haystack)
        }
    }
}
