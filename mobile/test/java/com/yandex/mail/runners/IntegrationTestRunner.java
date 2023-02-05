package com.yandex.mail.runners;

import com.yandex.mail.TestMailApplication;
import com.yandex.mail.shadows.MyShadowContentResolver;
import com.yandex.mail.shadows.ShadowAsyncDifferConfig;
import com.yandex.mail.shadows.ShadowFirebaseMessaging;
import com.yandex.mail.shadows.ShadowHmsInstanceId;
import com.yandex.mail.shadows.ShadowResourcesCompat;
import com.yandex.mail.shadows.ShadowShortcutBadger;

import org.junit.runners.model.InitializationError;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;

/**
 * Test runner for complex, integration tests, sets up {@link com.yandex.mail.TestMailApplication} as the test target.
 *
 * Consider using {@link UnitTestRunner} if possible.
 */
public class IntegrationTestRunner extends YandexMailRobolectricRunner {

    public IntegrationTestRunner(@NonNull Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    @NonNull
    protected Config buildGlobalConfig() {
        return preBuildGlobalConfig()
                .setApplication(TestMailApplication.class)
                .setManifest("AndroidManifest.xml")
                .setShadows(new Class[]{
                        ShadowResourcesCompat.class,
                        ShadowShortcutBadger.class,
                        ShadowFirebaseMessaging.class,
                        ShadowHmsInstanceId.class,
                        MyShadowContentResolver.class,
                        ShadowAsyncDifferConfig.class,
                })
                .build();
    }

    @NonNull
    public static TestMailApplication app() {
        return (TestMailApplication) RuntimeEnvironment.application;
    }
}
