package com.yandex.maps.testapp.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.RadioGroup
import android.widget.ToggleButton
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.*
import com.yandex.mapkit.search.FilterCollectionUtils.createFilterCollectionBuilder
import com.yandex.mapkit.search.Session.SearchListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.runtime.Error

class ListSearchActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    companion object {
        private val CITIES = hashMapOf(
            R.id.search_city_msk   to Point(55.75, 37.62),
            R.id.search_city_spb   to Point(59.95, 30.30),
            R.id.search_city_kiev  to Point(50.45, 30.52),
            R.id.search_city_minsk to Point(53.90, 27.56),
            -1                     to Point(55.75, 37.62) // default - MSK
        )
    }

    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val searchBox by lazy { find<SearchBox>(R.id.search_view) }
    private val searchResults by lazy { find<SearchResultsBox>(R.id.search_results) }
    private val cities by lazy { find<RadioGroup>(R.id.search_city_selector) }
    private val sortFilterButton by lazy { find<View>(R.id.search_options) }
    private val searchOptions by lazy { getSearchOptions(this).setDirectPageId("3897") }
    private val disableSpellingCorrectionButton by lazy { find<Button>(R.id.disable_spelling_correction) }
    private val primaryRequestFiltersButton by lazy { find<ToggleButton>(R.id.primary_request_filters) }
    private val primaryRequestFilters = createFilterCollectionBuilder().run {
        addEnumFilter("rating_threshold", listOf("gt4.5"))
        build()
    }

    private var session: Session? = null
    private var filters: Filters? = null
    private var sortByDistance: Boolean = false

    private fun searchPoint() = CITIES[cities.checkedRadioButtonId]!!.asGeometry()

    fun doSearch(text: String, disableSpellingCorrection: Boolean = false) {
        searchResults.clear()
        searchResults.hasMore = true

        session?.cancel()
        val searchOptions = searchOptions
            .setUserPosition(CITIES[cities.checkedRadioButtonId])
            .setSnippets(ALL_SNIPPETS)
            .setSearchTypes(ALL_SEARCH_TYPES)
            .setExperimentalSnippets(listOf("afisha_json/1.x"))
        searchOptions.disableSpellingCorrection = disableSpellingCorrection
        searchOptions.filters = if (primaryRequestFiltersButton.isChecked)
            primaryRequestFilters
            else null
        session = searchManager.submit(text, searchPoint(), searchOptions, searchListener)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCitySelected(view: View) = doSearch(searchBox.text)

    private val searchListener = object : SearchListener {
        override fun onSearchError(error: Error) {
            searchResults.hasMore = false
            showErrorMessage(this@ListSearchActivity, error)
            sortFilterButton.hide()
        }

        override fun onSearchResponse(response: Response) {
            val metadata = response.metadata

            sortByDistance = false
            metadata.sort?.type?.let {
                sortByDistance = it == SortType.DISTANCE
            }

            informOnEmptyResults(this@ListSearchActivity, response.results)
            searchResults.hasMore = session!!.hasNextPage()
            searchResults.append(response.collection.children)
            val businessResultMetadata = metadata.businessResultMetadata
            if (businessResultMetadata != null) {
                sortFilterButton.show()
                filters = Filters(businessResultMetadata.businessFilters)
            } else {
                sortFilterButton.hide()
            }

            if (metadata.correctedRequestText == null) {
                disableSpellingCorrectionButton.hide()
            } else {
                disableSpellingCorrectionButton.show()
                searchBox.text = metadata.correctedRequestText
                disableSpellingCorrectionButton.text = "Fixed a typo «${metadata.requestText}»"
                disableSpellingCorrectionButton.tag = metadata.requestText
            }
        }
    }

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) = doSearch(text)
        override fun onOptionsClick() {
            val i = Intent(this@ListSearchActivity, ListSearchSettingsActivity::class.java)
            i.putExtra("filters", filters)
            i.putExtra("sortByDistance", sortByDistance)
            startActivityForResult(i, 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_list_search)

        searchBox.text = "кафе"
        searchBox.setListener(searchBoxListener)

        window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        sortFilterButton.hide()
        disableSpellingCorrectionButton.hide()

        searchResults.fetchMoreFunction = fun () {
            if (session?.hasNextPage() == true) {
                session!!.fetchNextPage(searchListener)
            }
        }
        searchResults.onItemClickFunction = fun (_: String, obj: GeoObject) {
            SearchFactory.getInstance().searchLogger().logGeoObjectCardShown(GeoObjectCardType.MAIN, GeoObjectCardSource.SEARCH_RESULTS_LIST, obj)
            val intent = Intent(this@ListSearchActivity, when {
                obj.metadata<ToponymObjectMetadata>() != null -> ToponymSearchActivity::class.java
                obj.metadata<TransitObjectMetadata>() != null -> TransitSearchActivity::class.java
                else -> CompanySearchActivity::class.java
            })
            intent.putExtra("useIntentGeoObject", true)
            CardSearchActivity.intentGeoObject = obj
            startActivity(intent)
        }

        disableSpellingCorrectionButton.setOnClickListener {
            searchBox.text = it.tag as String
            doSearch(searchBox.text, disableSpellingCorrection = true)
        }

        primaryRequestFiltersButton.setOnClickListener {
            doSearch(searchBox.text)
        }

        doSearch(searchBox.text)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // We can get here only for business responses
            @Suppress("UNCHECKED_CAST")
            val filters = data!!.getSerializableExtra("filters") as? Filters

            session?.let {
                searchResults.clear()
                searchResults.hasMore = true
                if (data.getBooleanExtra("sortByDistance", false)) {
                    it.setSortByDistance(searchPoint())
                } else {
                    it.resetSort()
                }
                it.setSearchOptions(searchOptions.setFilters(
                    filters?.toFilterCollection()
                ))
                it.resubmit(searchListener)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }
}
