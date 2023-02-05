package ru.yandex.yandexnavi.projected.testapp.ui

import android.view.View
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.ScreenRect
import com.yandex.navikit.ui.RectProvider

class RectProviderImpl(private val view: View) : RectProvider {
    private var rectVisible = true

    override fun getRect(): ScreenRect? {
        val origin = IntArray(2)
        view.getLocationInWindow(origin)
        return ScreenRect(
            ScreenPoint(origin[0].toFloat(), origin[1].toFloat()),
            ScreenPoint(view.width.toFloat() + origin[0].toFloat(), view.height.toFloat() + origin[1].toFloat())
        )
    }

    override fun isRectVisible(): Boolean {
        return rectVisible
    }

    fun setRectVisible(visible: Boolean) {
        rectVisible = visible
    }
}
