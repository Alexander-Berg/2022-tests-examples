package ru.yandex.yandexmaps.navi.adapters.test.env

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import com.bluelinelabs.conductor.Controller
import ru.yandex.yandexmaps.common.conductor.setTopBottomSlidePanelsAnimation

internal class StubController : Controller() {

    init {
        setTopBottomSlidePanelsAnimation()
    }

    var tag: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return Space(activity)
    }
}
