package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class VillageSearchResponse {

    public static final String VILLAGE_SEARCH_TEMPLATE = "mock/village/villageSearchTemplate.json";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockVillage> villages;

    private VillageSearchResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static VillageSearchResponse villageSearchTemplate() {
        return new VillageSearchResponse(VILLAGE_SEARCH_TEMPLATE);
    }

    public String build() {
        JsonArray array = new JsonArray();
        villages.forEach(o -> array.add(o.getVillage()));
        template.getAsJsonObject("response").add("snippets", array);
        return template.toString();
    }
}
