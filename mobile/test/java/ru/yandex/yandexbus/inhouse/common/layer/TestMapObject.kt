package ru.yandex.yandexbus.inhouse.common.layer

import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener

abstract class TestMapObject : MapObject {

    private val _tapListeners = mutableListOf<MapObjectTapListener>()
    protected val tapListeners: List<MapObjectTapListener> = _tapListeners

    private var zIndex = 0f
    private var isDraggable = false
    private var isVisible = false
    private var userData: Any? = null

    override fun isValid() = true

    override fun getZIndex() = zIndex

    override fun setZIndex(zIndex: Float) {
        this.zIndex = zIndex
    }

    override fun isDraggable() = isDraggable

    override fun setDraggable(draggable: Boolean) {
        isDraggable = draggable
    }

    override fun getUserData() = userData

    override fun setUserData(userData: Any?) {
        this.userData = userData
    }

    override fun isVisible() = isVisible

    override fun setVisible(visible: Boolean) {
        isVisible = visible
    }

    override fun addTapListener(tapListener: MapObjectTapListener) {
        _tapListeners.add(tapListener)
    }

    override fun removeTapListener(tapListener: MapObjectTapListener) {
        _tapListeners.remove(tapListener)
    }
}
