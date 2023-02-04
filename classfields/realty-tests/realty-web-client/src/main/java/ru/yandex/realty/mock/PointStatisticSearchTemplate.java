package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class PointStatisticSearchTemplate {

    public static final String POINT_SEARCH_TEMPLATE = "mock/pointStatisticSearchTemplate.json";
    private static final String RESPONSE = "response";
    private static final String POINTS = "points";

    private JsonObject template;

    private PointStatisticSearchTemplate(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static PointStatisticSearchTemplate pointStatisticSearchTemplate() {
        return new PointStatisticSearchTemplate(POINT_SEARCH_TEMPLATE);
    }

    public String build() {
        return template.toString();
    }
}
