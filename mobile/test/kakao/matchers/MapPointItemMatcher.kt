package ru.yandex.market.test.kakao.matchers

import android.view.View
import com.yandex.mapkit.map.PlacemarkMapObject
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.checkout.pickup.multiple.PickupPointOnMapVo
import ru.yandex.market.checkout.pickup.multiple.PickupPointStyle
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.MapView
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.MarkersSelector
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.MedicinePickupPointPlacemark
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.PickupPointPlacemark
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.Placemark
import ru.yandex.market.clean.presentation.feature.checkout.map.view.map.RedeliveryPickupPointPlacemark
import ru.yandex.market.test.util.getPrivateProperty

class MapPointItemMatcher(
    private val percentValue: String?,
    private val gradient: Gradient,
    private val pickupStyle: PickupPointStyle,
    // If list is empty then check all displayed points
    private val checkedPointNames: Set<String> = emptySet()
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("Check points with style $pickupStyle")
    }

    override fun matchesSafely(mapView: View): Boolean {
        if (mapView !is MapView) return false

        val displayedPoints = getDisplayedPointsVo(mapView, pickupStyle).toMutableSet()
        if (displayedPoints.isEmpty()) return false

        val pointsForCheck = if (checkedPointNames.isNotEmpty()) {
            if (!displayedPoints.map { it.name }.containsAll(checkedPointNames)) return false
            displayedPoints.filter { point ->
                checkedPointNames.contains(point.name)
            }
        } else {
            displayedPoints
        }

        return pointsForCheck.all { displayedPoint ->
            isDisplayPointValid(displayedPoint)
        }
    }

    private fun isDisplayPointValid(displayedPoint: PickupPointOnMapVo): Boolean {
        return displayedPoint.cashbackPercent == percentValue &&
            displayedPoint.cashbackGradient == gradient &&
            displayedPoint.style == pickupStyle

    }

    private fun getDisplayedPointsVo(mapView: MapView, style: PickupPointStyle): List<PickupPointOnMapVo> {
        val markersSelector =
            mapView.getPrivateProperty<MapView, MarkersSelector>("markersSelector")
        val placemarksToMapObjectBindings =
            markersSelector?.getPrivateProperty<MarkersSelector, Map<PlacemarkMapObject, Placemark>>("placemarksToMapObjectBindings")

        val pickupPointOnMapVos = if (style == PickupPointStyle.MEDICINE_POINT) {
            placemarksToMapObjectBindings?.values
                ?.filterIsInstance(MedicinePickupPointPlacemark::class.java)
                ?.map { it.pickupPointVo } ?: emptyList()
        } else {
            val pickupPointPlacemarks = placemarksToMapObjectBindings?.values
                ?.filterIsInstance(PickupPointPlacemark::class.java)
                .orEmpty()

            val redeliveryPickupPointPlacemark = placemarksToMapObjectBindings?.values
                ?.filterIsInstance(RedeliveryPickupPointPlacemark::class.java)
                .orEmpty()

            (pickupPointPlacemarks + redeliveryPickupPointPlacemark).map { it.pickupPointVo }
        }
        return pickupPointOnMapVos
    }
}
