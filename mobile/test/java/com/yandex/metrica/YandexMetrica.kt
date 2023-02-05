package com.yandex.metrica

import android.app.Application
import com.yandex.metrica.profile.UserProfile

object YandexMetrica {

    var userProfileReported = false
    var activityTrackingEnabled = false

    fun clear() {
        userProfileReported = false
        activityTrackingEnabled = false
    }

    @JvmStatic
    fun reportUserProfile(profile: UserProfile) {
        userProfileReported = true
    }

    @JvmStatic
    fun enableActivityAutoTracking(application: Application) {
        activityTrackingEnabled = true
    }
}
