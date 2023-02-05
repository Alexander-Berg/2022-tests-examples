package com.yandex.mail;

import android.app.Application;

import static com.yandex.mail.util.ShadowLogUtils.configureTestShadowLog;

/**
 * We have to use a fake Application with a different class from {@link Application}.
 *
 * See {@link org.robolectric.DefaultTestLifecycle} for more details.
 */
public class UnitTestApplication extends Application {

    @Override
    public void onCreate() {
        configureTestShadowLog();
        super.onCreate();
    }
}
