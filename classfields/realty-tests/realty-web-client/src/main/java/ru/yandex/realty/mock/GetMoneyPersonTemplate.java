package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class GetMoneyPersonTemplate {

    public static final String GET_MONEY_PERSON_TEMPLATE = "mock/getMoneyPersonTemplate.json";

    private JsonObject template;

    private GetMoneyPersonTemplate(String pathToTemplate) {
        template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static GetMoneyPersonTemplate getMoneyPersonTemplate() {
        return new GetMoneyPersonTemplate(GET_MONEY_PERSON_TEMPLATE);
    }

    public JsonObject getJson() {
        return template;
    }
    public String build() {
        return template.toString();
    }
}
