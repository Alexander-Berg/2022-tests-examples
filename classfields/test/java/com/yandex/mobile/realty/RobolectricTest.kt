package com.yandex.mobile.realty

import android.app.Application
import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.yandex.mobile.verticalcore.utils.AppHelper
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * @author rogovalex on 19/02/2019.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [21],
    packageName = BuildConfig.APPLICATION_ID,
    manifest = Config.NONE,
    application = TestApplication::class,
    qualifiers = "ru"
)
abstract class RobolectricTest {

    protected lateinit var context: Context

    @Before
    fun baseSetup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        AppHelper.setupApp(application)
        context = ContextThemeWrapper(application, R.style.AppTheme)
    }
}
