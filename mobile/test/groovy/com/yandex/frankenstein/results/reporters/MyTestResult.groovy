package com.yandex.frankenstein.results.reporters

import org.gradle.api.tasks.testing.TestResult

class MyTestResult implements TestResult {

    private final ResultType mResultType
    private final List<Throwable> mExceptions

    MyTestResult(final ResultType resultType, final String... messages) {
        mResultType = resultType
        mExceptions = messages.collect { new Throwable(it) }
    }

    @Override
    ResultType getResultType() {
        return mResultType
    }

    @Override
    Throwable getException() {
        if (mExceptions) {
            return mExceptions.first()
        }
        return null
    }

    @Override
    List<Throwable> getExceptions() {
        return mExceptions
    }

    @Override
    long getStartTime() {
        return 0
    }

    @Override
    long getEndTime() {
        return 0
    }

    @Override
    long getTestCount() {
        return 0
    }

    @Override
    long getSuccessfulTestCount() {
        return 0
    }

    @Override
    long getFailedTestCount() {
        return 0
    }

    @Override
    long getSkippedTestCount() {
        return 0
    }
}
