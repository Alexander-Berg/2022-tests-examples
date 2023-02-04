package com.yandex.maps.testapp.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.CompoundButton
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.*
import com.yandex.mapkit.geometry.geo.PolylineUtils.pointByPolylinePosition
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SortType
import com.yandex.mapkit.search.search_layer.PlacemarkListener
import com.yandex.mapkit.search.search_layer.RequestType
import com.yandex.mapkit.search.search_layer.SearchLayer
import com.yandex.mapkit.search.search_layer.SearchResultListener
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.SearchBox
import com.yandex.maps.testapp.Utils
import com.yandex.maps.testapp.map.MapBaseActivity
import com.yandex.runtime.Error
import com.yandex.runtime.logging.Logger
import kotlin.math.min

class MapSearchActivity : MapBaseActivity() {
    companion object {
        private const val COORDINATE_FACTOR = 1000000.0
        private const val MAP_SEARCH_LIST_SETTINGS_REQUEST_CODE = 1
        private const val MAP_SEARCH_RESULT_LIST_REQUEST_CODE = 2

        var searchLayerVar: SearchLayer? = null
    }
    private val map by lazy { mapview.map }
    private val searchLayer by lazy { SearchFactory.getInstance().createSearchLayer(mapview.mapWindow) }
    private val searchBox by lazy { find<SearchBox>(R.id.map_search_box) }
    private val routeSwitch by lazy { find<CompoundButton>(R.id.search_route_switch) }
    private val nextRoutePositionButton by lazy { find<Button>(R.id.next_route_pos) }
    private val assetsProviderSwitch by lazy { find<CompoundButton>(R.id.assets_provider_switch) }
    private val insetSwitch by lazy { find<CompoundButton>(R.id.search_layer_inset_switch) }
    private val showResultsSwitch by lazy { find<CompoundButton>(R.id.search_layer_show_results_switch) }
    private val highlightSwitch by lazy { find<CompoundButton>(R.id.search_layer_highlight_switch) }
    private val sortFilterButton by lazy { find<View>(R.id.search_options) }
    private val overlays by lazy { listOf<View>(
        find(R.id.mapview_top_overlay),
        find(R.id.mapview_bottom_overlay),
        find(R.id.mapview_left_overlay),
        find(R.id.mapview_right_overlay)
    ) }

    private val polylineToSortBy by lazy { loadRoute() }
    private val displayedPolyline by lazy {
        with (map.mapObjects.addPolyline(polylineToSortBy)) {
            setStrokeColor(0xff008000.toInt())
            strokeWidth = 3.0f
            this
        }
    }
    private var routePosition: PolylinePosition = PolylinePosition(0, 0.0)
    private var inactivePolyline: PolylineMapObject? = null
    private var filters: Filters? = null
    private var sortByDistance: Boolean = false

    private val searchOptions by lazy { getSearchOptions(this)
        .setResultPageSize(20)
        .setDirectPageId("3897")
        .setGeometry(true)
        .setOrigin("testapp-android-mobile-search-layer")
        .setSnippets(ALL_SNIPPETS)
    }


    private val assetsProvider by lazy { MapSearchAssetsProvider(mapview) }

    private fun logEvent(message: String) {
        // We use warn here to make messages visible in default TestApp build
        Logger.warn(message)
    }

    private val searchResultListener = object : SearchResultListener {
        override fun onSearchStart(requestType: RequestType) {
            logEvent("onSearchStart: request type is $requestType")
            searchBox.setProgress(true)
            sortFilterButton.hide()
        }

        override fun onSearchSuccess(requestType: RequestType) {
            logEvent("onSearchSuccess: request type is $requestType")
            searchBox.setProgress(false)

            informOnEmptyResults(this@MapSearchActivity, searchLayer.searchResultsList)

            val metadata = searchLayer.searchMetadata()

            sortByDistance = false
            metadata?.sort?.type?.let {
                sortByDistance = it == SortType.DISTANCE
            }

            metadata?.businessResultMetadata?.let {
                sortFilterButton.show()
                filters = Filters(it.businessFilters)
            }
        }

        override fun onSearchError(error: Error, requestType: RequestType) {
            logEvent("onSearchError: request type is $requestType")
            searchBox.setProgress(false)
            showErrorMessage(this@MapSearchActivity, error)
        }

        override fun onPresentedResultsUpdate() {}

        override fun onAllResultsClear() {}
    }

    private val placemarkListener = PlacemarkListener { item ->
        searchLayer.selectPlacemark(item.id)
        Utils.showMessage(
            this@MapSearchActivity,
            item.geoObject.name + "\n" + item.geoObject.descriptionText
        )
        true
    }

    private val searchBoxListener = object : BaseSearchBoxListener() {
        override fun onSubmit(text: String) = startSearch()

        override fun onOptionsClick() {
            val intent = Intent(this@MapSearchActivity, ListSearchSettingsActivity::class.java)
                .putExtra("filters", filters)
                .putExtra("sortByDistance", sortByDistance)
            startActivityForResult(intent, MAP_SEARCH_LIST_SETTINGS_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                MAP_SEARCH_LIST_SETTINGS_REQUEST_CODE -> {
                    val filters = data.getSerializableExtra("filters")
                    @Suppress("UNCHECKED_CAST") searchLayer.setFilterCollection(
                        (filters as? Filters)?.toFilterCollection()
                    )

                    if (data.getBooleanExtra("sortByDistance", false)) {
                        searchLayer.setSortByDistance(getCurrentPoint())
                    } else {
                        searchLayer.resetSort()
                    }

                    searchLayer.resubmit()
                }
                MAP_SEARCH_RESULT_LIST_REQUEST_CODE -> if (data.hasExtra("collectionUri")) {
                    val uri = data.getStringExtra("collectionUri")
                    searchBox.text = uri
                    startSearch()
                } else {
                    searchLayer.selectPlacemark(data.getStringExtra("selectedGeoObjectId")!!)
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private val inputListener = object : InputListener {
        override fun onMapTap(map: Map, position: Point) {
            if (searchLayer.isVisible) {
                searchLayer.deselectPlacemark()
            }
        }
        override fun onMapLongTap(map: Map, position: Point) {
            val intent = Intent(this@MapSearchActivity, ReverseSearchActivity::class.java)
            intent.putExtra("lon", position.longitude)
            intent.putExtra("lat", position.latitude)
            intent.putExtra("zoom", map.cameraPosition.zoom.toInt())
            startActivity(intent)
        }
    }

    private fun toggleVisibility(view: View): Unit {
        view.visibility = if (view.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private val routeSearchSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        displayedPolyline.isVisible = isChecked
        if (isChecked) {
            searchLayer.setSortByDistance(Geometry.fromPolyline(polylineToSortBy))
            searchLayer.setPolylinePosition(routePosition)
            updateInactivePolyline()
        } else {
            searchLayer.resetSort()
            inactivePolyline?.let { map.mapObjects.remove(it) }
            inactivePolyline = null
        }

        toggleVisibility(showResultsSwitch)
        toggleVisibility(highlightSwitch)
        toggleVisibility(assetsProviderSwitch)
        toggleVisibility(insetSwitch)
        toggleVisibility(nextRoutePositionButton)

        searchLayer.resubmit()
    }

    private val nextRoutePositionListener = View.OnClickListener {
        // Move route position forward in 1 km increments
        routePosition = Geo.advancePolylinePosition(
            polylineToSortBy,
            routePosition,
            1000.0
        )
        searchLayer.setPolylinePosition(routePosition)
        updateInactivePolyline()
        map.move(
            CameraPosition(
                pointByPolylinePosition(polylineToSortBy, routePosition),
                map.cameraPosition.zoom,
                map.cameraPosition.azimuth,
                map.cameraPosition.tilt
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    private fun updateInactivePolyline() {
        val inactivePoints = Polyline(polylineToSortBy.points
            .withIndex()
            .takeWhile { it.index <= routePosition.segmentIndex }
            .map { it.value }
            .plus(pointByPolylinePosition(polylineToSortBy, routePosition))
        )
        inactivePolyline?.let { map.mapObjects.remove(it) }
        inactivePolyline = with(map.mapObjects.addPolyline(inactivePoints)) {
            setStrokeColor(0xff808080.toInt())
            strokeWidth = 5.0f
            this
        }
    }

    private val insetSwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isChecked) {

                val minSize = min(mapview.mapWindow.height(), mapview.mapWindow.width())
                val size = min((0.4 * minSize).toInt(), 200)
                searchLayer.setInsets(size, size, size, size)
                overlays.take(2).forEach { it.layoutParams.height = size }
                overlays.takeLast(2).forEach { it.layoutParams.width = size }
                overlays.forEach {
                    it.requestLayout()
                    it.show()
                }
            } else {
                searchLayer.setInsets(0, 0, 0, 0)
                overlays.forEach { it.hide() }
            }
        }

    private val showResultsSwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            searchLayer.isVisible = isChecked
        }

    private val highlightSwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            assetsProvider.highlightAdvert = isChecked
            searchLayer.forceUpdateMapObjects()
        }

    @Suppress("UNUSED_PARAMETER")
    fun onListViewTap(view: View) = startActivityForResult(
        Intent(this@MapSearchActivity, MapSearchResultListActivity::class.java),
        MAP_SEARCH_RESULT_LIST_REQUEST_CODE
    )

    @Suppress("UNUSED_PARAMETER")
    fun onSearchLayerClearTap(view: View) = searchLayer.clear()

    private val onResetAssetsProviderListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            searchLayer.resetAssetsProvider()
        } else {
            searchLayer.setAssetsProvider(assetsProvider)
        }
        searchLayer.forceUpdateMapObjects()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.search_map_search)
        super.onCreate(savedInstanceState)

        map.addInputListener(inputListener)

        searchBox.setListener(searchBoxListener)
        searchBox.text = "cafe"

        window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        routeSwitch.setOnCheckedChangeListener(routeSearchSwitchListener)
        nextRoutePositionButton.setOnClickListener(nextRoutePositionListener)
        assetsProviderSwitch.setOnCheckedChangeListener(onResetAssetsProviderListener)
        insetSwitch.setOnCheckedChangeListener(insetSwitchListener)
        showResultsSwitch.setOnCheckedChangeListener(showResultsSwitchListener)
        showResultsSwitch.isChecked = true
        highlightSwitch.setOnCheckedChangeListener(highlightSwitchListener)
        highlightSwitch.isChecked = true

        searchLayer.addSearchResultListener(searchResultListener)
        searchLayer.addPlacemarkListener(placemarkListener)
        searchLayer.setAssetsProvider(assetsProvider)
        searchLayer.obtainAdIcons(true)

        searchLayerVar = searchLayer

        val spb = CameraPosition(Point(59.945933, 30.320045), 15.0f, 0.0f, 0.0f)
        map.move(spb, Animation(Animation.Type.LINEAR, 0.0f)) {
            // `Handler(mainLooper).post` is used to start search after
            // search_layer is notified of new search window
            Handler(mainLooper).post {
                startSearch()
            }
        }
    }

    private fun loadRoute(): Polyline {
        val routePoints = resources.getIntArray(R.array.search_route)
            .map { it / COORDINATE_FACTOR }
            .toDoubleArray()
        val builder = PolylineBuilder()
        (routePoints.indices step 2).forEach {
            builder.append(Point(routePoints[it], routePoints[it + 1]))
        }
        return builder.build()
    }

    private fun getCurrentPoint(): Geometry {
        val reg = map.visibleRegion
        val latSum = reg.bottomLeft.latitude + reg.bottomRight.latitude + reg.topLeft.latitude + reg.topRight.latitude
        val lonSum = reg.bottomLeft.longitude + reg.bottomRight.longitude + reg.topLeft.longitude + reg.topRight.longitude
        return Geometry.fromPoint(Point(latSum / 4, lonSum / 4))
    }

    private fun startSearch() {
        val uriPrefix = "ymapsbm1://"
        if (searchBox.text.startsWith(uriPrefix)) {
            searchLayer.searchByUri(searchBox.text, searchOptions)
        } else {
            searchLayer.submitQuery(searchBox.text, searchOptions)
        }
    }
}
