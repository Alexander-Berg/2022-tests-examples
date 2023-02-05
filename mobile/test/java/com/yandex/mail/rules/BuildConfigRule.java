package com.yandex.mail.rules;

import com.yandex.mail.BuildConfig;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.annotation.NonNull;

public class BuildConfigRule implements TestRule {

    @Override @NonNull
    public Statement apply(@NonNull  Statement base, @NonNull  Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                boolean runOnlyOnDebugBuilds = description.getAnnotation(RunOnlyOnDebugBuilds.class) != null;
                boolean runOnlyOnReleaseBuilds = description.getAnnotation(RunOnlyOnReleaseBuilds.class) != null;

                if (runOnlyOnDebugBuilds && !BuildConfig.DEBUG) {
                    return;
                } else if (runOnlyOnReleaseBuilds && BuildConfig.DEBUG) {
                    return;
                }

                base.evaluate();
            }
        };
    }
}
