package com.yandex.frankenstein.filters.methods;

import com.yandex.frankenstein.annotations.handlers.TestCaseIdHandler;
import com.yandex.frankenstein.settings.TestCases;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.junit.runners.model.FrameworkMethod;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class TestCasesMethodFilter implements MethodFilter {

    @NotNull private final TestCases mTestCases;
    @NotNull private final JsonDigger mDigger;
    @NotNull private final Predicate<List<String>> mIsIgnoredFunction;
    @NotNull private final String mFilterDescription;
    @NotNull private final TestCaseIdHandler mTestCaseIdHandler;

    TestCasesMethodFilter(@NotNull final TestCases testCases,
                          @NotNull final List<String> path,
                          @NotNull final Predicate<List<String>> isIgnoredFunction,
                          @NotNull final String filterDescription) {
        this(testCases, new JsonDigger(path), isIgnoredFunction, filterDescription, new TestCaseIdHandler());
    }

    TestCasesMethodFilter(@NotNull final TestCases testCases,
                          @NotNull final JsonDigger digger,
                          @NotNull final Predicate<List<String>> isIgnoredFunction,
                          @NotNull final String description,
                          @NotNull final TestCaseIdHandler testCaseIdHandler) {
        mTestCases = testCases;
        mDigger = digger;
        mIsIgnoredFunction = isIgnoredFunction;
        mFilterDescription = description;
        mTestCaseIdHandler = testCaseIdHandler;
    }

    @Override
    public boolean isIgnored(@NotNull final FrameworkMethod method,
                             @NotNull final List<Object> testParameters) {
        final int testCaseId = mTestCaseIdHandler.getTestCaseId(method, testParameters);
        final JSONArray jsonArray = mDigger.get(mTestCases.getTestCase(testCaseId));
        final List<String> parameters = toList(jsonArray);
        return mIsIgnoredFunction.test(parameters);
    }

    @NotNull
    @Override
    public String getDescription() {
        return mFilterDescription;
    }

    @NotNull
    private List<String> toList(@NotNull final JSONArray array) {
        return IntStream.range(0, array.length()).mapToObj(array::getString).collect(Collectors.toList());
    }
}
