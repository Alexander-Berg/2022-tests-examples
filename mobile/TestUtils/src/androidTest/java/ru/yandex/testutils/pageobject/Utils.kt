package ru.yandex.testutils.pageobject

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

fun UiDevice.waitObject(selector: BySelector, longTimeout: Long): UiObject2? {
    return wait(Until.findObject(selector), longTimeout)
}

fun UiDevice.requireObject(selector: BySelector, longTimeout: Long): UiObject2 {
    waitObject(selector, longTimeout)
    return findObject(selector)!!
}