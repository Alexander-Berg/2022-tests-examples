package com.yandex.mail.testopithecus.feature.impl

import android.app.UiAutomation
import androidx.test.uiautomator.UiDevice
import com.yandex.xplat.testopithecus.Rotatable
import io.qameta.allure.kotlin.Allure

class RotatableImpl(private val device: UiDevice) : Rotatable {

    override fun isInLandscape(): Boolean {
        return device.displayRotation == UiAutomation.ROTATION_FREEZE_90
    }

    override fun rotateToLandscape() {
        Allure.step("Переворачиваем телефон в горизонтальный режим") {
            device.setOrientationLeft()
        }
    }

    override fun rotateToPortrait() {
        Allure.step("Переворачиваем телефон в портретный режим") {
            device.setOrientationNatural()
        }
    }
}
