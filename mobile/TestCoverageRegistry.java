package com.yandex.frankenstein;

import com.yandex.frankenstein.steps.TestCoverageStep;

@SuppressWarnings("PMD.ClassNamingConventions")
public final class TestCoverageRegistry {

    private static volatile TestCoverageStep sTestCoverageStep;

    private TestCoverageRegistry() {}

    public static TestCoverageStep coverage() {
        return sTestCoverageStep;
    }

    public static void registerInstance(final TestCoverageStep testCoverageStep) {
        sTestCoverageStep = testCoverageStep;
    }
}
