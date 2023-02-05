package com.yandex.launcher

import android.content.Context
import androidx.annotation.Keep
import com.yandex.launcher.common.util.AppUtils
import com.yandex.launcher.common.util.Logger
import com.yandex.launcher.api.LauncherHost
import com.yandex.launcher.api.LauncherPreference
import com.yandex.launcher.app.TestApplication

@Keep
class LauncherHostImpl(@Suppress("UNUSED_PARAMETER") applicationContext: Context) : LauncherHost {

    private val logger = Logger.createInstance("LauncherHostImpl")

    private val host = (applicationContext as TestApplication).testLauncherHost

    init {
        AppUtils.checkAppContext(applicationContext)
        logger.d("Created")
    }

    override fun initializeLauncher() {
        host.initializeLauncher()
    }

    override fun initializeSpeechKit() {
        host.initializeSpeechKit()
    }

    override fun initializeAlice() {
        host.initializeAlice()
    }

    override fun initializeInteractor() {
        host.initializeInteractor()
    }

    override fun initializeWebBrowser() {
        host.initializeWebBrowser()
    }

    override fun onBeforeLauncherActivityOnCreate() {
        host.onBeforeLauncherActivityOnCreate()
    }

    override fun notifyInitIfNeeded() {
        host.notifyInitIfNeeded()
    }

    override fun getPreference(preference: LauncherPreference) = host.getPreference(preference)

    override fun setPreference(preference: LauncherPreference, value: String) {
        host.setPreference(preference, value)
    }

    override fun getConfig() = host.getConfig()

    override fun getMinusOneKitFactory() = host.getMinusOneKitFactory()

    override fun getPrerenderFactory() = host.getPrerenderFactory()

    override fun getSuggestToolkit() = host.getSuggestToolkit()

    override fun getUriHandlerManager() = host.getUriHandlerManager()

    override fun getSpeechKitManagerFactory() = host.getSpeechKitManagerFactory()

    override fun getAliceActivityStarter() = host.getAliceActivityStarter()

    override fun getImageCacheManager() = host.getImageCacheManager()

    override fun getSearchAppShortcutsDelegateFactory() = host.getSearchAppShortcutsDelegateFactory()

    override fun getAccountManagerFacade() = host.getAccountManagerFacade()
}