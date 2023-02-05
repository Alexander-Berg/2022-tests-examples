package ru.yandex.autotests.mobile.disk.android.infrastructure.suites;

import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.AllTests;

public class FeatureAllTests extends AllTests {
    public FeatureAllTests(Class<?> klass) throws Throwable {
        super(klass);
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        //do not filter
        //filter used into FeatureSuite
    }
}
