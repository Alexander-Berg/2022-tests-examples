package ru.yandex.market.test.kakao.actions

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.yandex.mapkit.map.PlacemarkMapObject
import org.hamcrest.Matcher
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.MapView
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.MarkersSelector
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.Placemark
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.test.util.getPrivateProperty

class MapPointItemClick(
    private val geoCoordinates: GeoCoordinates,
) : ViewAction {

    override fun getDescription() = "Map click on point item by coordinates $geoCoordinates"

    override fun getConstraints(): Matcher<View> = isAssignableFrom(MapView::class.java)

    override fun perform(uiController: UiController, mapView: View) {
        if (mapView !is MapView) return

        val markersSelector =
            mapView.getPrivateProperty<MapView, MarkersSelector>("markersSelector")
        val placemarksToMapObjectBindings =
            markersSelector
                ?.getPrivateProperty<MarkersSelector, Map<PlacemarkMapObject, Placemark>>("placemarksToMapObjectBindings")
                .orEmpty()

        val (placemarkOnMap, placemark) = placemarksToMapObjectBindings.filter { (_, placemark) ->
            placemark.coordinates == geoCoordinates
        }.entries.first()

        markersSelector?.selectMarker(
            placemark = placemark,
            placemarkOnMap = placemarkOnMap,
        )

        uiController.loopMainThreadUntilIdle()
    }
}
