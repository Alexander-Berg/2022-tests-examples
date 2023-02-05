package com.yandex.frankenstein.annotations.handlers;

import com.yandex.frankenstein.annotations.TestCaseId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import java.util.List;

public class TestCaseIdHandler {

    public int getTestCaseId(@NotNull final FrameworkMethod method, @NotNull final List<Object> testParameters) {
        return getTestCaseId(method.getAnnotation(TestCaseId.class),
                method.getName(), method.getDeclaringClass(), testParameters);
    }

    public int getTestCaseId(@NotNull final Description description, @NotNull final List<Object> testParameters) {
        return getTestCaseId(description.getAnnotation(TestCaseId.class),
                description.getMethodName(), description.getTestClass(), testParameters);
    }

    private int getTestCaseId(@Nullable final TestCaseId methodTestCaseIdAnnotation,
                              @NotNull final String testName, @NotNull final Class<?>  testClass,
                              @NotNull final List<Object> testParameters) {
        if (testParameters.isEmpty() == false) {
            final Object firstParameter = testParameters.get(0);
            if (firstParameter instanceof String) {
                try {
                    return Integer.parseInt((String) firstParameter);
                } catch (final NumberFormatException ignored) {}
            }
        }

        TestCaseId testCaseIdAnnotation = methodTestCaseIdAnnotation;
        if (testCaseIdAnnotation == null) {
            testCaseIdAnnotation = testClass.getAnnotation(TestCaseId.class);
        }

        final int testCaseId;
        if (testCaseIdAnnotation != null) {
            testCaseId = testCaseIdAnnotation.value();
        } else {
            throw new IllegalStateException("No test case id for " + testName);
        }

        return testCaseId;
    }
}
