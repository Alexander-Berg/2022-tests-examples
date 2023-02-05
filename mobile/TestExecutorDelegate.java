package com.yandex.frankenstein.runner;

import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import java.util.List;

public interface TestExecutorDelegate {
    void prepare(@NotNull final FrameworkMethod method,
                 @NotNull final List<Object> testParameters,
                 @NotNull final Description description);
    void before();
    void after();
}
