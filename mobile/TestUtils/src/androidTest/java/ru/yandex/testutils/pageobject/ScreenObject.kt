package ru.yandex.testutils.pageobject

import androidx.test.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

abstract class ScreenObject(private val backNavigation: ScreenObject? = null) {


    abstract val appPackageId: String

    protected val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

    fun <T> assertIsOn(): T {
        try {
            @Suppress("UNCHECKED_CAST")
            return this as T
        } catch (castException: ClassCastException) {
            throw InvalidScreenException(javaClass)
        }
    }

    fun goBack(): ScreenObject {
        if (backNavigation == null) {
            throw IllegalStateException("Cannot go back from ${this::class.simpleName}")
        }
        device.pressBack()
        return backNavigation
    }
}