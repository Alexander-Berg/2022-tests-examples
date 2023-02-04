package com.yandex.maps.testapp.search

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.yandex.mapkit.Animation
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.BoundingBoxHelper
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.search.*
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.map.MapBaseActivity
import com.yandex.maps.testapp.search.encodable.BillboardObjectMetadataEncodable

class RouteBannerActivity: MapBaseActivity() {

    private var routePoints = mutableListOf(
            Point(55.6, 37.5),
            Point(55.7, 38.2))

    enum class Source(val text: String) {
        Via("Via"),
        ZeroSpeed("Zero speed"),
        BillboardRoute("Billboard route"),
        BillboardWindow("Billboard window")
    }

    private var banners: List<GeoObject>? = null
    private var source: Source? = null

    private val resultsButton by lazy { find<Button>(R.id.banner_results_button) }

    private var viaSession: ViaBannerSession? = null
    private var zsbSession: ZeroSpeedBannerSession? = null

    private val map by lazy { mapview.map }

    private val advertPageId by lazy {
        getSearchOptions(this).advertPageId ?: "maps"
    }

    private val logger by lazy {
        SearchFactory.getInstance().billboardLogger()
    }

    private val viaBannerManager by lazy {
        SearchFactory.getInstance().createViaBannerManager(advertPageId)
    }

    private val zsbManager by lazy {
        SearchFactory.getInstance().createZeroSpeedBannerManager(advertPageId)
    }

    private val billboardRouteManager by lazy {
        SearchFactory.getInstance().createBillboardRouteManager(advertPageId)
    }

    private val billboardWindowManager by lazy {
        SearchFactory.getInstance().createBillboardWindowManager(advertPageId)
    }

    private val billboardRouteListener = AdvertRouteListener {
        setResults(billboardRouteManager.advertObjects, Source.BillboardRoute)
    }

    private val billboardWindowListener = BillboardListener {
        setResults(billboardWindowManager.advertObjects, Source.BillboardWindow)
    }

    private val mapInputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            appendPoint(point)
        }

        override fun onMapLongTap(map: Map, point: Point) {
            displayMenu()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.search_route_banner)
        super.onCreate(savedInstanceState)

        map.addInputListener(mapInputListener)

        billboardRouteManager.addListener(billboardRouteListener)
        billboardWindowManager.addListener(billboardWindowListener)

        val msk = CameraPosition(Point(55.756888, 37.615071), 15.0f, 0.0f, 0.0f)
        map.move(msk, Animation(Animation.Type.LINEAR, 0.0f)) {}

        updateMapObjects()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRouteBannerMenuClick(view: View) = displayMenu()

    @Suppress("UNUSED_PARAMETER")
    fun onRouteBannerResultsClick(view: View) {
        val list = (banners ?: listOf())
            .mapNotNull { it.metadata<BillboardObjectMetadata>() }
            .map { toJson(BillboardObjectMetadataEncodable(it)) }
            .toTypedArray()

        val intent = Intent(this@RouteBannerActivity, TextListActivity::class.java)
        intent.putExtra(TextListActivity.TEXT_LIST_EXTRA, list)
        startActivity(intent)
    }

    fun appendPoint(point: Point) {
        routePoints.add(point)
        updateMapObjects()
    }

    enum class MenuItem(val text: String) {
        Clear("Clear"),
        Via("Request via-banner"),
        ZeroSpeed("Request Zero-speed banner"),
        RouteBillboards("Request route billboards"),
        WindowBillboards("Request window billboards"),
        Cancel("Cancel")
    }

    fun displayMenu() {
        val items = MenuItem.values().map { it.text }.toTypedArray()

        AlertDialog.Builder(this)
                .setTitle("Tap on the map to draw custom route points\n" +
                        "Advert page id: $advertPageId")
                .setItems(items) { _, which ->
                    when (MenuItem.values()[which]) {
                        MenuItem.Clear -> clearRoute()
                        MenuItem.Via -> requestViaBanner()
                        MenuItem.ZeroSpeed -> requestZeroSpeedBanner()
                        MenuItem.RouteBillboards -> requestRouteBillboards()
                        MenuItem.WindowBillboards -> requestWindowBillboards()
                        MenuItem.Cancel -> {}
                    }
                }
                .create()
                .show()
    }

    private fun clearRoute() {
        billboardRouteManager.resetRoute()
        billboardWindowManager.resetSearchArea()

        viaSession?.cancel()
        viaSession = null

        zsbSession?.cancel()
        zsbSession = null

        routePoints.clear()
    }

    private fun clearResults() {
        banners = null
        source = null

        resultsButton.text = "..."
        resultsButton.isEnabled = false

        updateMapObjects()
    }

    private fun setResult(banner: GeoObject?, source: Source) {
        var result = listOf<GeoObject>()
        if (banner != null) {
            result = listOf(banner)
        }
        setResults(result, source)
    }

    @SuppressLint("SetTextI18n")
    private fun setResults(banners: List<GeoObject>, source: Source) {
        this.banners = banners
        this.source = source

        resultsButton.text = "${source.text}: ${banners.size}"
        resultsButton.isEnabled = true

        banners.forEach {
            when (source) {
                Source.Via -> logger.logBannerShow(it)
                Source.ZeroSpeed -> logger.logBannerShow(it)
                else -> {}
            }
        }

        updateMapObjects()
    }

    private fun requestViaBanner() {
        clearResults()

        viaSession = viaBannerManager.requestViaBanner(
                Polyline(routePoints)) { banner ->
            viaSession = null
            setResult(banner, Source.Via)
        }
    }

    private fun requestZeroSpeedBanner() {
        clearResults()

        if (routePoints.count() < 1) {
            return
        }

        zsbSession = zsbManager.requestZeroSpeedBanner(
                routePoints[0], Polyline(routePoints)) { banner ->
            zsbSession = null
            setResult(banner, Source.ZeroSpeed)
        }
    }

    private fun requestRouteBillboards() {
        clearResults()

        billboardRouteManager.setRoute(Polyline(routePoints))
    }

    private fun requestWindowBillboards() {
        clearResults()

        val boundingBox = BoundingBoxHelper.getBounds(Polyline(routePoints))
        billboardWindowManager.setSearchArea(boundingBox)
    }

    private fun updateMapObjects() {
        map.mapObjects.clear()

        if (routePoints.isNotEmpty()) {
            map.mapObjects.addPlacemark(routePoints[0])
        }

        if (routePoints.count() > 1) {
            map.mapObjects.addPlacemark(routePoints.last())
        }

        with (map.mapObjects.addPolyline(Polyline(routePoints))) {
            setStrokeColor(0xff008000.toInt())
            strokeWidth = 3.0f
        }

        banners?.forEach {
            it.geometry.firstOrNull()?.point?.let {
                map.mapObjects.addPlacemark(it)
            }
        }
    }
}

