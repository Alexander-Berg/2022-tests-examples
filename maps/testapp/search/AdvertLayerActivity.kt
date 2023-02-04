package com.yandex.maps.testapp.search

import android.os.Bundle
import com.yandex.mapkit.Animation
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.BillboardObjectMetadata
import com.yandex.mapkit.search.advert_layer.*
import com.yandex.maps.testapp.Utils
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.map.MapBaseActivity

class AdvertLayerActivity : MapBaseActivity() {
    private val route = Polyline(listOf(
        Point(55.756888, 37.615071),
        Point(55.764656, 37.605244)
    ))
    private val map by lazy { mapview.map }
    private val assetProvider = AdvertLayerAssetProvider()
    private val advertLayer by lazy {
        SearchFactory.getInstance().createAdvertLayer(
            "maps_search_test",
            mapview.mapWindow,
            assetProvider)
    }

    private val advertLayerListener = object : AdvertLayerListener {
        override fun onAdvertPinShown(geoObject: GeoObject) {}
        override fun onAdvertPinHidden(geoObject: GeoObject) {}
        override fun onAdvertPinTapped(geoObject: GeoObject) {
            Utils.showMessage(this@AdvertLayerActivity,
                geoObject.metadata<BillboardObjectMetadata>()?.title + "\n" +
                geoObject.metadata<BillboardObjectMetadata>()?.address)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.search_advert_layer)
        super.onCreate(savedInstanceState)

        val msc = CameraPosition(Point(55.756888, 37.615071), 15.0f, 0.0f, 0.0f)
        map.move(msc, Animation(Animation.Type.LINEAR, 0.0f)) {}

        advertLayer.addListener(advertLayerListener)

        advertLayer.setRoute(route)

        with (map.mapObjects.addPolyline(route)) {
            setStrokeColor(0xff008000.toInt())
            strokeWidth = 3.0f
        }
    }
}
