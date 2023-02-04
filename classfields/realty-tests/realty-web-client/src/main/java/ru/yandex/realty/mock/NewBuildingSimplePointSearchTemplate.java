package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class NewBuildingSimplePointSearchTemplate {

    public static final String PATH_TO_MOCK_NEWBUILDING_SIMPLE_POINT_SEARCH_JSON = "mock/newbuildingSimplePointSearch.json";
    private static final String RESPONSE = "response";
    private static final String POINTS = "points";


    private JsonObject template;

    private NewBuildingSimplePointSearchTemplate(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static NewBuildingSimplePointSearchTemplate newBuildingSimplePointSearchTemplate() {
        return new NewBuildingSimplePointSearchTemplate(PATH_TO_MOCK_NEWBUILDING_SIMPLE_POINT_SEARCH_JSON);
    }

    public NewBuildingSimplePointSearchTemplate setId(int id) {
        template.getAsJsonObject(RESPONSE).getAsJsonArray("points").get(0).getAsJsonObject()
                .addProperty("id", id);
        template.getAsJsonObject(RESPONSE).getAsJsonObject("searchContext").getAsJsonArray("sites").get(0).getAsJsonObject()
                .addProperty("id", id);
        return this;
    }

    public String build() {
        return template.toString();
    }
}
