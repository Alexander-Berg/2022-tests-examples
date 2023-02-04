package com.yandex.maps.testapp.search

import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.search.AdvertRouteListener
import com.yandex.mapkit.search.BillboardListener
import com.yandex.mapkit.search.BillboardObjectMetadata
import com.yandex.mapkit.search.SearchFactory
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.TestAppActivity
import com.yandex.maps.testapp.search.encodable.BillboardObjectMetadataEncodable

class RouteAdvertManagerActivity : TestAppActivity() {
    override fun onStopImpl(){}
    override fun onStartImpl(){}

    companion object {
        private val CITY_ROUTES = hashMapOf(
                R.id.route_advert_manager_city_msk   to Polyline(listOf(Point(55.756888, 37.615071), Point(55.764656, 37.605244))),
                R.id.route_advert_manager_city_spb   to Polyline(listOf(Point(59.937128, 30.312444), Point(59.931011, 30.361025)))
        )
        private val CITY_WINDOWS = hashMapOf(
                R.id.route_advert_manager_city_msk   to BoundingBox(Point(55.756888, 37.605244), Point(55.764656, 37.615071)),
                R.id.route_advert_manager_city_spb   to BoundingBox(Point(59.931011, 30.312444), Point(59.937128, 30.361025))
        )
        private const val ADVERT_PAGE_ID = "maps_search_test"
    }
    private val billboardListText by lazy { find<TextView>(R.id.billboard_list_text) }
    private val routeAdvertManager = SearchFactory.getInstance().createBillboardRouteManager(ADVERT_PAGE_ID)
    private val windowAdvertManager = SearchFactory.getInstance().createBillboardWindowManager(ADVERT_PAGE_ID)
    private val cities by lazy { find<RadioGroup>(R.id.route_advert_manager_city_selector) }
    private val managers by lazy { find<RadioGroup>(R.id.advert_manager_selector) }

    private fun advertObjects(): List<GeoObject> {
        return if (managers.checkedRadioButtonId == R.id.route_advert)
            routeAdvertManager.advertObjects
        else
            windowAdvertManager.advertObjects
    }

    private fun showAdvertObjects() {
        billboardListText.text = advertObjects()
            .mapNotNull { it.metadata<BillboardObjectMetadata>() }
            .joinToString("\n\n") { toJson(BillboardObjectMetadataEncodable(it)) }
    }

    private val routeAdvertListener = AdvertRouteListener { showAdvertObjects() }
    private val windowAdvertListener = BillboardListener { showAdvertObjects() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_route_advert_manager)

        routeAdvertManager.addListener(routeAdvertListener)
        windowAdvertManager.addListener(windowAdvertListener)

        doSearch()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCitySelected(view: View) = doSearch()

    @Suppress("UNUSED_PARAMETER")
    fun onAdvertManagerSelected(view: View) {
        billboardListText.text = ""
        doSearch()
    }

    private fun doSearch() {
        if (managers.checkedRadioButtonId == R.id.route_advert) {
            routeAdvertManager.setRoute(CITY_ROUTES[cities.checkedRadioButtonId]!!)
        } else {
            windowAdvertManager.resetSearchArea()
            windowAdvertManager.setSearchArea(CITY_WINDOWS[cities.checkedRadioButtonId]!!)
        }
    }
}
