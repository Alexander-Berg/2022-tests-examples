package com.yandex.metrokit.testapp.controller

import com.yandex.metrokit.geometry.Point
import com.yandex.metrokit.scheme_window.surface.PlacemarkSurfaceObject
import com.yandex.metrokit.scheme_window.surface.ZoomLevel
import com.yandex.metrokit.testapp.common.EmptyImageProvider
import com.yandex.runtime.image.ImageProvider

interface PlacemarkData {
    val scaleMode: PlacemarkSurfaceObject.ScaleMode
    fun image(zoom: ZoomLevel): ImageProvider
    fun anchor(zoom: ZoomLevel): Point
    fun opacity(zoom: ZoomLevel): Float
}

data class ScreenPlacemarkData(
        private val image: ImageProvider,
        private val anchor: Point,
        private val opacity: Float
) : PlacemarkData {
    override val scaleMode = PlacemarkSurfaceObject.ScaleMode.SCREEN
    override fun image(zoom: ZoomLevel) = image
    override fun anchor(zoom: ZoomLevel) = anchor
    override fun opacity(zoom: ZoomLevel) = opacity
}

class PlanePlacemarkData(
        zoomToImage: Map<Float, ImageProvider>,
        private val anchor: Point
) : PlacemarkData {
    override val scaleMode = PlacemarkSurfaceObject.ScaleMode.ZOOM_LEVEL
    override fun image(zoom: ZoomLevel) = zoomToImageWithDefault.getValue(zoom.value)
    override fun anchor(zoom: ZoomLevel) = anchor
    override fun opacity(zoom: ZoomLevel) = if (zoom.value !in zoomToImageWithDefault) 0f else 1f

    private val zoomToImageWithDefault = zoomToImage.withDefault { EmptyImageProvider }
}
