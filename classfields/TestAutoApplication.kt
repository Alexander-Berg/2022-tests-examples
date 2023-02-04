package ru.auto.ara.core

import android.content.Context
import com.yandex.metrica.push.YandexMetricaPush
import com.yandex.metrica.push.common.core.PushServiceController
import com.yandex.metrica.push.common.core.PushServiceControllerProvider
import ru.auto.ara.AutoApplication
import ru.auto.ara.core.di.module.TestMainProvider
import ru.auto.ara.core.di.module.TestPhotoUploadModule
import ru.auto.ara.di.module.ApplicationProvider
import ru.auto.ara.plugin.SafeCorePlugin

class TestAutoApplication: AutoApplication() {

    override fun initComponents() {
        initComponents(
            ApplicationProvider(this, TestPhotoUploadModule(this), TestPassportApi())
        ) { deps -> TestMainProvider(deps) }
    }

    override fun setupWebViews() { //переопределен метод, без вызова супер метода,
        // т.к. повторный вызов при инициализации вызывает краш в WebView.setDataDirectorySuffix(directorySuffix);
        // т.к. метод был вызван ранее
    }

    override fun getMetricaPushPlugin(): SafeCorePlugin = object : SafeCorePlugin() {
        override fun onSafeSetup(cxt: Context?) {
            YandexMetricaPush.init(applicationContext, StubPushServiceControllerProvider())
        }

        override fun name(): String = "TestPushLibPlugin"
    }

    private class StubPushServiceControllerProvider : PushServiceControllerProvider {
        override fun getPushServiceController(): PushServiceController = object : PushServiceController {
            override fun register(): Boolean = false

            override fun getToken(): String? = null

            override fun getTitle(): String = "Stub"
        }
    }
}
