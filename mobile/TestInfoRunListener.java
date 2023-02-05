package com.yandex.frankenstein.runner.listener;

import com.yandex.frankenstein.annotations.handlers.IgnoredTestInfoEncoder;
import com.yandex.frankenstein.annotations.handlers.TestCaseIdHandler;
import com.yandex.frankenstein.annotations.handlers.TestInfoEncoder;
import com.yandex.frankenstein.annotations.handlers.TestInfoTransmitter;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.FrameworkMethod;

import java.util.List;

public class TestInfoRunListener extends RunListener {
    @NotNull private final TestInfoTransmitter mTestInfoTransmitter = new TestInfoTransmitter();
    @NotNull private final TestInfoEncoder mTestInfoEncoder;
    @NotNull private final IgnoredTestInfoEncoder mIgnoredTestInfoEncoder;
    @NotNull private final TestCaseIdHandler mTestCaseIdHandler;
    @NotNull private final List<Object> mTestParameters;


    public TestInfoRunListener(@NotNull final List<Object> testParameters) {
        this(new TestInfoEncoder(), new IgnoredTestInfoEncoder(), new TestCaseIdHandler(), testParameters);
    }

    public TestInfoRunListener(@NotNull final TestInfoEncoder testInfoEncoder,
                        @NotNull final IgnoredTestInfoEncoder ignoredTestInfoEncoder,
                        @NotNull final TestCaseIdHandler testCaseIdHandler,
                        @NotNull final List<Object> testParameters) {
        mTestInfoEncoder = testInfoEncoder;
        mIgnoredTestInfoEncoder = ignoredTestInfoEncoder;
        mTestCaseIdHandler = testCaseIdHandler;
        mTestParameters = testParameters;
    }

    @Override
    public void testStarted(final Description description) {
        transmitInfo(description);
    }

    @Override
    public void testIgnored(final Description description) {
        transmitInfo(description);
    }

    public void testIgnored(final FrameworkMethod method, final String reason) {
        final int testCaseId = mTestCaseIdHandler.getTestCaseId(method, mTestParameters);
        mTestInfoTransmitter.transmit("FRANKENSTEIN IGNORED AUTOTEST",
                mIgnoredTestInfoEncoder.encode(testCaseId, reason));
    }

    private void transmitInfo(final Description description) {
        final int testCaseId = mTestCaseIdHandler.getTestCaseId(description, mTestParameters);
        mTestInfoTransmitter.transmit("FRANKENSTEIN AUTOTEST", mTestInfoEncoder.encode(testCaseId, description));
    }
}
