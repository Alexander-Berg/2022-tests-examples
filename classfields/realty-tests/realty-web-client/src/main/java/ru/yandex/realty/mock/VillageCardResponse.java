package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class VillageCardResponse {

    public static final String VILLAGE_CARD_TEMPLATE = "mock/villageCard.json";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockVillage> villages;

    private VillageCardResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static VillageCardResponse villageCardTemplate() {
        return new VillageCardResponse(VILLAGE_CARD_TEMPLATE);
    }

    public String build() {
        return template.toString();
    }

    public VillageCardResponse setId(String id) {
        template.getAsJsonObject("response").getAsJsonObject("village").addProperty("id", id);
        return this;
    }
}
