package ru.yandex.yandexmaps.navi.adapters.test.env

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bluelinelabs.conductor.Router
import ru.yandex.yandexmaps.common.conductor.BaseController

class RootControllerForTest : BaseController() {
    override fun performInjection() = Unit

    lateinit var mainRouter: Router
    lateinit var placecardRouter: Router

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val rootFrameLayout = FrameLayout(inflater.context)
        val mainFrameLayout = FrameLayout(inflater.context).apply {
            id = 41
            mainRouter = getChildRouter(this)
        }
        val placecardFrameLayout = FrameLayout(inflater.context).apply {
            id = 42
            placecardRouter = getChildRouter(this)
        }
        rootFrameLayout.addView(mainFrameLayout)
        rootFrameLayout.addView(placecardFrameLayout)
        return rootFrameLayout
    }
}
