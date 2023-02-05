package com.yandex.frankenstein.annotations.handlers;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.runner.Description;

public class TestInfoEncoder {

    @NotNull
    public String encode(final int testCaseId, @NotNull final Description testDescription) {
        final JSONObject container = convertToJson(testDescription, testCaseId);
        return container.toString();
    }

    @NotNull
    private JSONObject convertToJson(@NotNull final Description testDescription, final int testCaseId) {
        final JSONObject container = new JSONObject();
        container.put("method", testDescription.getDisplayName());
        container.put("name", testDescription.getMethodName());
        container.put("class", testDescription.getClassName());
        container.put("case id", testCaseId);

        return container;
    }
}
