package com.yandex.mail.testopithecus.steps

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

open class PermissionActions(private val device: UiDevice) {

    companion object {
        const val resourceNamePermission = "com.android.permissioncontroller:id/content_container"
        const val resourceNameAllowButton = "com.android.permissioncontroller:id/permission_allow_button"
        const val resourceNameDenyButton = "com.android.permissioncontroller:id/permission_deny_button"
        const val resourceNameDenyAndDontAsk = "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button"
    }

    fun waitPermissionWindow() {
        onView(ViewMatchers.withResourceName("content_container")).perform()
    }

    fun hasPermissionWindow(): Boolean {
        if (device.has("content_edit")) {
            return false
        }
        return device.hasObject(By.res(resourceNamePermission))
    }

    fun pressAllow(timeout: Long = DEFAULT_TIMEOUT) {
        device.findManyByFullResourceName(resourceNameAllowButton)?.click()
    }

    fun pressDeny(timeout: Long = DEFAULT_TIMEOUT) {
        device.findManyByFullResourceName(resourceNameDenyButton)?.click()
    }

    fun pressDenyAndDontAsk(timeout: Long = DEFAULT_TIMEOUT) {
        device.findManyByFullResourceName(resourceNameDenyAndDontAsk)?.click()
    }
}
