package com.edadeal.android

import com.edadeal.android.util.Log

class TestApp : App() {

    // Because unit tests with Robolectric started to fail after upgrading Gradle plugin.
    // Related issue: https://github.com/robolectric/robolectric/issues/3836
    override fun isInTestProcess() = true

    override fun onCreate() {
        super.onCreate()
        Log.isDebugEnabled = false
        Log.isErrorEnabled = false
    }
}
