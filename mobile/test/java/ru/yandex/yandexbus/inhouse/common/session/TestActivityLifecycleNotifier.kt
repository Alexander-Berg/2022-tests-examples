package ru.yandex.yandexbus.inhouse.common.session

import android.app.Activity
import android.app.Application
import android.os.Bundle

class TestActivityLifecycleNotifier : ActivityLifecycleNotifier {

    private val listeners = mutableListOf<Application.ActivityLifecycleCallbacks>()

    override fun register(listener: Application.ActivityLifecycleCallbacks) {
        listeners.add(listener)
    }

    override fun unregister(listener: Application.ActivityLifecycleCallbacks) {
        listeners.remove(listener)
    }

    fun activityCreated(activity: Activity, savedInstanceState: Bundle?) {
        listeners.forEach { it.onActivityCreated(activity, savedInstanceState) }
    }

    fun activityStarted(activity: Activity) {
        listeners.forEach { it.onActivityStarted(activity) }
    }

    fun activityResumed(activity: Activity) {
        listeners.forEach { it.onActivityResumed(activity) }
    }

    fun activityPaused(activity: Activity) {
        listeners.forEach { it.onActivityPaused(activity) }
    }

    fun activityStopped(activity: Activity) {
        listeners.forEach { it.onActivityStopped(activity) }
    }

    fun activitySaveInstanceState(activity: Activity, outState: Bundle?) {
        listeners.forEach { it.onActivitySaveInstanceState(activity, outState) }
    }

    fun activityDestroyed(activity: Activity) {
        listeners.forEach { it.onActivityDestroyed(activity) }
    }
}
