package com.yandex.mail.testopithecus.feature.impl

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.OPTIMAL_SWIPE_SPEED_IN_PIXELS
import com.yandex.mail.testopithecus.steps.findManyByFullResourceName
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.xplat.testopithecus.AppRunningState
import com.yandex.xplat.testopithecus.ApplicationRunningState

class ApplicationRunningStateImpl(private val device: UiDevice) : ApplicationRunningState {
    override fun getApplicationRunningState(): AppRunningState {
        val appProcessInfo = RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)

        return when (appProcessInfo.importance) {
            RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> AppRunningState.runningForeground
            RunningAppProcessInfo.IMPORTANCE_CACHED -> AppRunningState.runningBackground
            RunningAppProcessInfo.REASON_UNKNOWN -> AppRunningState.unknown
            RunningAppProcessInfo.IMPORTANCE_GONE -> AppRunningState.notRunning
            else -> throw AssertionError("State of app not recognize. Code `appProcessInfo.importance` is ${appProcessInfo.importance}")
        }
    }

    override fun changeApplicationRunningState(state: AppRunningState) {
        when (state) {
            AppRunningState.runningBackground -> device.pressHome()
            AppRunningState.runningForeground -> {
                val icon = "com.google.android.apps.nexuslauncher:id/icon"
                val task = "com.google.android.apps.nexuslauncher:id/task_name"
                val launcher = "com.google.android.apps.nexuslauncher:id/drag_layer"
                val clearAll = "com.google.android.apps.nexuslauncher:id/clear_all"
                val appName = getTextFromResources(R.string.app_name)

                device.pressRecentApps()
                while (!device.hasObject(By.res(clearAll))) {
                    device.findManyByFullResourceName(icon)!!.click()
                    if (appName.startsWith(device.findManyByFullResourceName(task)!!.text)) {
                        device.pressBack()
                        device.findManyByFullResourceName(icon)!!.parent!!.click()
                        return
                    }
                    device.findManyByFullResourceName(launcher)!!.swipe(Direction.RIGHT, 0.7f, OPTIMAL_SWIPE_SPEED_IN_PIXELS)
                }
            }
            else -> throw AssertionError("Don't recognize state of app: $state")
        }
    }
}
