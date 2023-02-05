package ru.yandex.autotests.mobile.disk.android

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*

class StepsLocator {
    @Inject
    lateinit var navigationPageSteps: NavigationPageSteps

    @Inject
    lateinit var adbSteps: AdbSteps

    @Inject
    lateinit var loginSteps: LoginSteps

    @Inject
    lateinit var badCarmaSteps: BadCarmaSteps
}

interface StepsLocatorHolder {
    var locator: StepsLocator
}

interface Navigation : StepsLocatorHolder {
    fun onNavigationPage(action: NavigationAction) = locator.navigationPageSteps.action()
}

interface Adb : StepsLocatorHolder {
    fun withAdb(action: AdbAction) = locator.adbSteps.action()
}

interface Login : StepsLocatorHolder {
    fun onLogin(action: LoginAction) = locator.loginSteps.action()
}

interface BadCarma : StepsLocatorHolder {
    fun withBadCarma(action: BadCarmaAction) = locator.badCarmaSteps.action()
}

interface DiskTest : Navigation, Adb

interface LoginDiskTest : DiskTest, Login, BadCarma
