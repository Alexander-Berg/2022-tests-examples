package com.yandex.metrokit.testapp.controller

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.util.Property
import android.view.animation.OvershootInterpolator
import com.yandex.metrokit.geometry.Box
import com.yandex.metrokit.geometry.Point
import com.yandex.metrokit.scheme_window.surface.PlacemarkSurfaceObject
import com.yandex.metrokit.scheme_window.surface.Surface
import com.yandex.metrokit.scheme_window.surface.TrackingObject
import com.yandex.metrokit.scheme_window.surface.TrackingObjectListener

class StationPinController(
        private val surface: Surface,
        private val placemarkData: PlacemarkData
) {
    private data class PlacemarkObjectBound(
            val placemark: PlacemarkSurfaceObject,
            val trackingObject: TrackingObject,
            var animator: Animator
    )

    private var placemarkObjectBound: PlacemarkObjectBound? = null

    private val trackingObjectListener = TrackingObjectListener {
        placemarkObjectBound?.let(this::updatePin)
    }

    fun dispose() {
        disposePlacemarkObjectBound()
    }

    fun pin(stationId: String, animated: Boolean) {
        disposePlacemarkObjectBound()

        val trackingObject: TrackingObject? = surface.trackStation(stationId)
        val currentSurfaceObject = trackingObject?.currentSurfaceObject

        if (trackingObject != null && currentSurfaceObject != null) {
            trackingObject.setSelected(true, null)
            trackingObject.addListener(trackingObjectListener)

            val zoomLevel = trackingObject.zoomLevel

            val placemark = surface.userCollection.addPlacemark(
                    placemarkData.image(zoomLevel),
                    currentSurfaceObject.bbox.center(),
                    PLACEMARK_INITIAL_ANCHOR,
                    PLACEMARK_INITIAL_OPACITY,
                    placemarkData.scaleMode,
                    false,
                    20f
            )

            val appearAnimator = AnimatorSet()
                    .also {
                        it.playTogether(
                                ObjectAnimator.ofObject(
                                        placemark,
                                        PROPERTY_PLACEMARK_ANCHOR,
                                        PointEvaluator,
                                        placemarkData.anchor(zoomLevel)
                                ).also { it.interpolator = OvershootInterpolator() },
                                ObjectAnimator.ofFloat(
                                        placemark,
                                        PROPERTY_PLACEMARK_OPACITY,
                                        placemarkData.opacity(zoomLevel)
                                )
                        )
                    }
                    .also { it.duration = if (!animated) 0 else return@also }
                    .also { it.start() }

            placemarkObjectBound = PlacemarkObjectBound(placemark, trackingObject, appearAnimator)
        }
    }

    fun unpin(animated: Boolean) {
        placemarkObjectBound?.let {
            val placemark = it.placemark

            it.animator.cancel()

            val disappearAnimator = AnimatorSet()
                    .also {
                        it.playTogether(
                                ObjectAnimator.ofObject(
                                        placemark,
                                        PROPERTY_PLACEMARK_ANCHOR,
                                        PointEvaluator,
                                        PLACEMARK_INITIAL_ANCHOR
                                ),
                                ObjectAnimator.ofFloat(
                                        placemark,
                                        PROPERTY_PLACEMARK_OPACITY,
                                        PLACEMARK_INITIAL_OPACITY
                                )
                        )
                    }
                    .also { it.duration = if (!animated) 0 else return@also }
                    .also { it.start() }

            it.animator = disappearAnimator

            it.trackingObject.setSelected(false, null)
        }
    }

    private fun disposePlacemarkObjectBound() {
        placemarkObjectBound?.let {
            it.animator.removeAllListeners()
            it.animator.end()
            it.placemark.also(surface.userCollection::remove)
            it.trackingObject.setSelected(false, null)
            it.trackingObject.removeListener(trackingObjectListener)
            placemarkObjectBound = null
        }
    }

    private fun updatePin(bound: PlacemarkObjectBound) {
        bound.trackingObject.currentSurfaceObject?.let { currentSurfaceObject ->
            bound.placemark.position = currentSurfaceObject.bbox.center()
            val zoomLevel = bound.trackingObject.zoomLevel
            bound.placemark.setImage(placemarkData.image(zoomLevel))
        }
    }

    private fun Box.center() = Point((min.x + max.x) / 2.0f, (min.y + max.y) / 2.0f)

    private companion object {
        val PLACEMARK_INITIAL_ANCHOR = Point(0.5f, 0.5f)
        val PLACEMARK_INITIAL_OPACITY = 0.0f

        val PROPERTY_PLACEMARK_ANCHOR = Property.of(PlacemarkSurfaceObject::class.java, Point::class.java, "anchor")!!
        val PROPERTY_PLACEMARK_OPACITY = Property.of(PlacemarkSurfaceObject::class.java, Float::class.java, "opacity")!!

        private object PointEvaluator : TypeEvaluator<Point> {
            override fun evaluate(fraction: Float, startValue: Point, endValue: Point): Point {
                val x = startValue.x + fraction * (endValue.x - startValue.x)
                val y = startValue.y + fraction * (endValue.y - startValue.y)
                return Point(x, y)
            }
        }
    }
}
