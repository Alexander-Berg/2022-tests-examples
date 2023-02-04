package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class VillagePointSearchTemplate {

    private static final String RESPONSE = "response";
    private static final String POINTS = "points";
    public static final String PATH_TO_MOCK_VILLAGE_POINT_SEARCH_JSON = "mock/villagePointSearchTemplate.json";


    private JsonObject template;

    private VillagePointSearchTemplate(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static VillagePointSearchTemplate villagePointSearchTemplate() {
        return new VillagePointSearchTemplate(PATH_TO_MOCK_VILLAGE_POINT_SEARCH_JSON);
    }

    public VillagePointSearchTemplate setId(String id) {
        template.getAsJsonObject(RESPONSE).getAsJsonArray("points").get(0).getAsJsonObject().addProperty("id", id);
        template.getAsJsonObject(RESPONSE).getAsJsonObject("searchContext").getAsJsonArray("villages").get(0)
                .getAsJsonObject().addProperty("id", id);
        return this;
    }

    public String build() {
        return template.toString();
    }
}
