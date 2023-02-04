package com.yandex.mobile.job;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

import mockit.internal.startup.Startup;

/**
 * TODO: Add destription
 *
 * @author ironbcc on 16.04.2015.
 */
public class JMockitRobolectricTestRunner extends RobolectricTestRunner {

    static { Startup.initializeIfPossible(); }

    /**
     * Constructs a new instance of the test runner.
     *
     * @throws InitializationError if the test class is malformed
     */
    public JMockitRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }
}
