package com.yandex.mail.testopithecus

import android.os.Bundle
import android.system.Os
import com.facebook.testing.screenshot.ScreenshotRunner
import com.karumi.shot.AndroidStorageInfo
import com.karumi.shot.compose.ComposeScreenshotRunner
import io.qameta.allure.android.runners.AllureAndroidJUnitRunner

class AllureScreenRunner() : AllureAndroidJUnitRunner() {

    override fun onCreate(args: Bundle) {
        super.onCreate(args)
        configureFacebookLibFolder()
        ScreenshotRunner.onCreate(this, args)
        ComposeScreenshotRunner.onCreate(this)
    }

    override fun finish(resultCode: Int, results: Bundle?) {
        ScreenshotRunner.onDestroy()
        ComposeScreenshotRunner.onDestroy()
        super.finish(resultCode, results)
    }

// This env var configuration is needed to make facebook's library
    // point at the folder we can use in API 29+ to save screenshots.
    // Without this hack, their library would only work on API 28-.
    private fun configureFacebookLibFolder() {
        Os.setenv("EXTERNAL_STORAGE", AndroidStorageInfo.storageBaseUrl, true)
    }
}
