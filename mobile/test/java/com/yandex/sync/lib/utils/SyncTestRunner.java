package com.yandex.sync.lib.utils;

import com.yandex.sync.lib.BuildConfig;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;

public class SyncTestRunner extends RobolectricTestRunner {
    /**
     * Creates a runner to run {@code testClass}. Looks in your working directory for your AndroidManifest.xml file
     * and res directory by default. Use the {@link Config} annotation to configure.
     *
     * @param testClass the test class to be run
     * @throws InitializationError if junit says so
     */
    public SyncTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }


    @Override
    @NonNull
    protected Config buildGlobalConfig() {
        return new Config.Builder()
                .setConstants(BuildConfig.class)
                .setPackageName("com.yandex.sync.lib")
                .setManifest("AndroidManifest.xml")
                .setSdk(23)
                .build();
    }
}
