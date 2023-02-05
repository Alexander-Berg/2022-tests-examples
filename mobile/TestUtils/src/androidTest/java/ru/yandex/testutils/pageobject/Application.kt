package ru.yandex.testutils.pageobject

import android.content.Intent
import androidx.test.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

abstract class Application(val appPackageId: String, private var startingScreen: ScreenObject, private val launchTimeout: Long) {

    val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

    fun launch() {
        device.pressHome()
        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), launchTimeout)

        val context = InstrumentationRegistry.getContext()
        val arguments = InstrumentationRegistry.getArguments()
        val intent = context.packageManager.getLaunchIntentForPackage(appPackageId)!!
        val intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.addFlags(intentFlags).putExtras(arguments)
        context.startActivity(intent)

        device.wait(Until.hasObject(By.pkg(appPackageId).depth(0)), launchTimeout)
    }

    fun <T : ScreenObject> resetScreen(newCurrentScreen: T, action: (T.() -> Any) = { }) {
        startingScreen = newCurrentScreen
        execute { action(assertIsOn()) }
    }

    fun execute(action: ScreenObject.() -> Any) {
        val returnedValue = action(startingScreen)
        if (returnedValue is ScreenObject) {
            startingScreen = returnedValue
        }
    }
}