package com.yandex.maps.testapp.suggest

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ToggleButton
import com.google.gson.GsonBuilder
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.maps.testapp.*
import com.yandex.maps.testapp.search.*
import com.yandex.runtime.Error
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.search.encodable.SuggestItemEncodable
import com.yandex.maps.testapp.search.test.toPrettyJson

class SuggestActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    companion object {
        private val CITIES = hashMapOf(
            R.id.suggest_city_msk   to Point(55.75, 37.62),
            R.id.suggest_city_spb   to Point(59.95, 30.30),
            R.id.suggest_city_kiev  to Point(50.45, 30.52),
            R.id.suggest_city_minsk to Point(53.90, 27.56),
            -1                      to Point(55.75, 37.62) // default - MSK
        )
    }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val suggestSession by lazy { searchManager.createSuggestSession() }
    private val searchBox by lazy { find<SearchBox>(R.id.search_view) }
    private val citySelector by lazy { find<RadioGroup>(R.id.suggest_city_selector) }
    private val jsonSwitch by lazy { find<ToggleButton>(R.id.suggest_json_toggle) }
    private val textView by lazy { find<TextView>(R.id.suggest_text_view) }
    private val options = makeSearchOptions(SearchType.GEO.value or
        SearchType.BIZ.value,
        ExtendedSearchType.TRANSIT.value
    ).setOrigin("mobile-maps-searchnearby-text")
    private val suggestOptions = SuggestOptions().setSuggestTypes(
        SuggestType.GEO.value or
        SuggestType.BIZ.value or
            ExtendedSearchType.TRANSIT.value
    )

    private var session: Session? = null

    @Suppress("UNUSED_PARAMETER")
    fun onCitySelected(view: View) = requestSuggest(searchBox.text)

    private fun searchWindow() = makeBoundingBox(CITIES[citySelector.checkedRadioButtonId]!!, 0.2)

    private val suggestListener = object : SuggestSession.SuggestListener {
        override fun onResponse(items: List<SuggestItem>) {
            updateSuggest(items.map { SuggestResult(it) })
            textView.text = toPrettyJson(items.map { SuggestItemEncodable(it) })
        }

        override fun onError(error: Error) {
            searchBox.setProgress(false)
            showErrorMessage(this@SuggestActivity, error)
        }
    }

    private val searchListener = object : SearchListener {
        override fun onSearchError(error: Error) {
            searchBox.setProgress(false)
            Utils.showError(this@SuggestActivity, error)
            session = null
        }

        override fun onSearchResponse(response: Response) {
            searchBox.setProgress(false)
            Utils.showMessage(this@SuggestActivity, "Search request sent and will be added to history")
            session = null
        }
    }

    private val jsonSwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                searchBox.setSuggestVisibility(View.GONE)
                textView.visibility = View.VISIBLE
            } else {
                searchBox.setSuggestVisibility(View.VISIBLE)
                textView.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.suggest)
        searchBox.setListener(object : BaseSearchBoxListener() {
            override fun onTextChanged(text: String) = requestSuggest(text)
            override fun onSubmit(text: String) = startSearch(text)
        })
        searchBox.hideOptions()
        jsonSwitch.setOnCheckedChangeListener(jsonSwitchListener)
        requestSuggest("")
    }

    private fun requestSuggest(text: String) {
        searchBox.setProgress(true)
        suggestSession.suggest(text, searchWindow(), suggestOptions, suggestListener)
    }

    private fun updateSuggest(results: List<SuggestResult>) {
        searchBox.setProgress(false)
        searchBox.setSuggest(results)
    }

    private fun startSearch(text: String) {
        searchBox.setProgress(true)
        session = searchManager.submit(text, searchWindow().asGeometry(), options, searchListener)
    }
}
