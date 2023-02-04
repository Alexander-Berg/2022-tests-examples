package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCalltracking {

    public static final String CALLTRACKING_EXAMPLE = "mocksConfigurable/calltracking/CalltrackingExample.json";
    public static final String CALLTRACKING_FILTERED_EXAMPLE = "mocksConfigurable/calltracking/CalltrackingFilteredExample.json";
    public static final String CALLTRACKING_REQUEST = "mocksConfigurable/calltracking/CalltrackingRequest.json";

    public static final String TEXT_FILTER = "text_filter";
    public static final String WEBSEARCH_QUERY = "websearch_query";
    public static final String DOMAIN = "domain";
    public static final String TARGET = "TARGET";
    public static final String SOURCE = "SOURCE";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockCalltracking(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCalltracking mockCalltracking(String pathToTemplate) {
        return new MockCalltracking(pathToTemplate);
    }

    public static MockCalltracking calltrackingExample() {
        return mockCalltracking(CALLTRACKING_EXAMPLE);
    }

    public static MockCalltracking calltrackingFiltered() {
        return mockCalltracking(CALLTRACKING_FILTERED_EXAMPLE);
    }

    public static JsonObject getCalltrackingRequest() {
        return new GsonBuilder().create().fromJson(getResourceAsString(CALLTRACKING_REQUEST), JsonObject.class);
    }

}
