package ru.yandex.market.test.kakao.views

import android.view.View
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapView
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class KMapView : KBaseView<KMapView> {

    constructor(function: ViewBuilder.() -> Unit) : super(function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    private val numberFormat by lazy {
        DecimalFormat("#.###", DecimalFormatSymbols(Locale.US)).apply { roundingMode = RoundingMode.HALF_UP }
    }

    fun checkPosition(latitude: Double, longitude: Double) {
        view.check { view, notFoundException ->
            if (view is MapView) {
                assertPoints(latitude, longitude, view.map.cameraPosition.target)
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    }

    private fun assertPoints(latitude: Double, longitude: Double, actualPoint: Point) {
        val actualLatitude = actualPoint.latitude.format()
        val actualLongitude = actualPoint.longitude.format()
        assert(actualLatitude == latitude.format())
        assert(actualLongitude == longitude.format())
    }

    private fun Double.format() = numberFormat.format(this)
}