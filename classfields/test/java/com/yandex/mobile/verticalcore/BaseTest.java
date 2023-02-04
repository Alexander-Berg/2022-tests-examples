package com.yandex.mobile.verticalcore;

import android.app.Application;
import android.content.Context;

import com.yandex.mobile.verticalcore.utils.AppHelper;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 *
 * @author ironbcc on 28.04.2015.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    application = TestApplication.class,
    manifest = Config.NONE,
    packageName = BuildConfig.APPLICATION_ID,
    sdk = 21
)
public abstract class BaseTest {

    protected Context context;

    @Before
    public void baseSetup() {
        Application application = RuntimeEnvironment.application;
        context = application;
        AppHelper.setupApp(application);
    }
}
