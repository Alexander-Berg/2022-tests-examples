package ru.yandex.yandexmaps.navi.adapters.test.env

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import ru.yandex.yandexmaps.common.conductor.getCurrentController
import ru.yandex.yandexmaps.common.conductor.push

private const val ROOT_ID = 42

class ActivityForTests : Activity() {

    private lateinit var activityRouter: Router

    val mainRouter: Router
        get() = (activityRouter.getCurrentController() as RootControllerForTest).mainRouter

    val placecardRouter: Router
        get() = (activityRouter.getCurrentController() as RootControllerForTest).placecardRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.id = ROOT_ID
        setContentView(root)
        activityRouter = Conductor.attachRouter(this, root, savedInstanceState)
        activityRouter.push(RootControllerForTest())
    }
}
