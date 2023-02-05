package ru.yandex.yandexnavi.projected.testapp.impl

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.yandex.mapkit.MapKitFactory
import com.yandex.navikit.guidance.Guidance
import ru.yandex.yandexnavi.projected.platformkit.lifecycle.ProjectedLifecycleCallbacks
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifecycleCallbacksImpl @Inject constructor(
    private val guidance: Guidance
) : Application.ActivityLifecycleCallbacks, ProjectedLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity) {
        onPause()
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity) {
        onResume()
    }

    override fun onProjectedCreate() {
    }

    override fun onProjectedStart() {
    }

    override fun onProjectedResume() {
        onResume()
    }

    override fun onProjectedPause() {
        onPause()
    }

    override fun onProjectedStop() {
    }

    override fun onProjectedDestroy() {
    }

    private fun onResume() {
        Timber.d("AndroidAuto.AutomotiveApp.Lifecycle Navilib onResume")
        MapKitFactory.getInstance().onStart()
        guidance.onStart()
    }

    private fun onPause() {
        Timber.d("AndroidAuto.AutomotiveApp.Lifecycle Navilib onPause")
        MapKitFactory.getInstance().onStop()
        guidance.onPause(true)
    }
}
