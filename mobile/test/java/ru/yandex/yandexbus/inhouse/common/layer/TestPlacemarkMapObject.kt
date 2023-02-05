package ru.yandex.yandexbus.inhouse.common.layer

import android.graphics.PointF
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.Callback
import com.yandex.mapkit.map.CompositeIcon
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectDragListener
import com.yandex.mapkit.map.ModelStyle
import com.yandex.mapkit.map.PlacemarkAnimation
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.AnimatedImageProvider
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.model.AnimatedModelProvider
import com.yandex.runtime.model.ModelProvider
import com.yandex.runtime.ui_view.ViewProvider
import ru.yandex.yandexbus.inhouse.utils.ui.UiUtils

class TestPlacemarkMapObject(
    private val parent: MapObjectCollection,
    private var geometry: Point,
    private var image: ImageProvider? = null,
    private var style: IconStyle? = null,
    private var view: ViewProvider? = null
) : TestMapObject(), PlacemarkMapObject {

    private var direction = 0f
    private var opacity = UiUtils.ALPHA_OPAQUE

    fun performTap() {
        tapListeners.forEach { it.onMapObjectTap(this, geometry) }
    }

    override fun getParent() = parent

    override fun getGeometry() = geometry

    override fun setGeometry(geometry: Point) {
        this.geometry = geometry
    }

    override fun getDirection() = direction

    override fun setDirection(direction: Float) {
        this.direction = direction
    }

    override fun getOpacity() = opacity

    override fun setOpacity(opacity: Float) {
        this.opacity = opacity
    }
    
    override fun useCompositeIcon() = TestCompositeIcon()

    override fun useAnimation() = TestPlacemarkAnimation()
    
    override fun setIconStyle(style: IconStyle) {
        this.style = style
    }

    override fun setIcon(image: ImageProvider) {
        this.image = image
    }

    override fun setIcon(image: ImageProvider, style: IconStyle) {
        setIcon(image)
        setIconStyle(style)
    }

    override fun setIcon(image: ImageProvider, onFinished: Callback) {
        setIcon(image)
        onFinished.onTaskFinished()
    }

    override fun setIcon(image: ImageProvider, style: IconStyle, onFinished: Callback) {
        setIcon(image)
        setIconStyle(style)
        onFinished.onTaskFinished()
    }

    override fun setView(view: ViewProvider) {
        this.view = view
    }

    override fun setView(view: ViewProvider, style: IconStyle) {
        setView(view)
        setIconStyle(style)
    }

    override fun setView(view: ViewProvider, onFinished: Callback) {
        setView(view)
        onFinished.onTaskFinished()
    }

    override fun setView(view: ViewProvider, style: IconStyle, onFinished: Callback) {
        setView(view)
        setIconStyle(style)
        onFinished.onTaskFinished()
    }

    override fun setModel(modelProvider: ModelProvider, style: ModelStyle) {
        // no-op
    }

    override fun setModel(modelProvider: ModelProvider, style: ModelStyle, onFinished: Callback) {
        // no-op
    }

    override fun setModelStyle(modelStyle: ModelStyle) {
        // no-op
    }

    override fun setScaleFunction(points: MutableList<PointF>) {
        // no-op
    }

    override fun setDragListener(dragListener: MapObjectDragListener?) {
        // no-op
    }

    override fun setVisible(visible: Boolean, animation: Animation, onFinished: Callback?) {
        onFinished?.onTaskFinished()
    }
}

class TestCompositeIcon: CompositeIcon {

    override fun removeIcon(name: String) {
        // no-op
    }

    override fun removeAll() {
        // no-op
    }

    override fun setIconStyle(name: String, style: IconStyle) {
        // no-op
    }

    override fun setIcon(name: String, image: ImageProvider, style: IconStyle) {
        // no-op
    }

    override fun setIcon(name: String, image: ImageProvider, style: IconStyle, onFinished: Callback) {
        // no-op
    }

    override fun isValid() = true
}

class TestPlacemarkAnimation: PlacemarkAnimation {

    private var isReversed = false

    override fun isReversed() = isReversed

    override fun setReversed(reversed: Boolean) {
        isReversed = reversed
    }

    override fun isValid() = true

    override fun setIcon(image: AnimatedImageProvider, style: IconStyle) {
        // no-op
    }

    override fun setIcon(image: AnimatedImageProvider, style: IconStyle, onFinished: Callback) {
        // no-op
    }

    override fun setIconStyle(style: IconStyle) {
        // no-op
    }

    override fun setModel(model: AnimatedModelProvider, style: ModelStyle) {
        // no-op
    }

    override fun setModel(model: AnimatedModelProvider, style: ModelStyle, onFinished: Callback) {
        // no-op
    }

    override fun setModelStyle(style: ModelStyle) {
        // no-op
    }

    override fun play() {
        // no-op
    }

    override fun play(onFinished: Callback) {
        // no-op
    }

    override fun stop() {
        // no-op
    }

    override fun pause() {
        // no-op
    }

    override fun resume() {
        // no-op
    }
}