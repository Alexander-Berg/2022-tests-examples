package com.yandex.mail.runners;

import org.junit.runners.model.InitializationError;

import androidx.annotation.Nullable;

/**
 * Truly lightweight test runner
 */
public class UnitTestRunner extends IntegrationTestRunner {

    public UnitTestRunner(@Nullable Class<?> testClass) throws InitializationError {
        super(testClass);
    }

//    @Override
//    @NonNull
//    protected Config buildGlobalConfig() {
//        return new Config.Builder()
//                .setApplication(Application.class)
//                .setPackageName("com.yandex.mail")
//                .setManifest(Config.NONE)
//                .setSdk(21)
//                .build();
//    }
//
//    @NonNull
//    public static Application app() {
//        return RuntimeEnvironment.application;
//    }
}
