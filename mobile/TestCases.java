package com.yandex.frankenstein.settings;

import com.yandex.frankenstein.io.ResourceReader;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestCases {

    @NotNull private final Map<Integer, JSONObject> mTestCases = new HashMap<>();
    private final boolean mValid;

    public TestCases(@NotNull final String testCasesFile, @NotNull final ResourceReader resourceReader) {
        boolean success = true;
        try {
            final JSONArray testCases = new JSONArray(resourceReader.readAsString(testCasesFile));
            IntStream.range(0, testCases.length()).mapToObj(testCases::getJSONObject)
                    .forEach(testCase -> mTestCases.put(testCase.getInt("id"), testCase));
        } catch (final Exception e) {
            success = false;
        }

        mValid = success;
    }

    public boolean isValid() {
        return mValid;
    }

    public boolean has(final int testCaseId) {
        return mTestCases.containsKey(testCaseId);
    }

    @NotNull
    public JSONObject getTestCase(final int testCaseId) {
        return mTestCases.computeIfAbsent(testCaseId, k -> new JSONObject());
    }

    @NotNull
    public List<String> getAttribute(final int testCaseId, @NotNull final String attribute) {
        return Optional.ofNullable(mTestCases.get(testCaseId))
                .map(testCase -> testCase.getJSONObject("attributes").optJSONArray(attribute))
                .map(this::toList)
                .orElse(Collections.emptyList());
    }

    @NotNull
    private List<String> toList(@NotNull final JSONArray array) {
        return IntStream.range(0, array.length()).mapToObj(array::getString).collect(Collectors.toList());
    }
}
