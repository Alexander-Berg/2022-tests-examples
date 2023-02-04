package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class CardMockResponse {

    public static final String CARD_TEMPLATE = "mock/cardTemplate.json";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockOffer> offers;

    private CardMockResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static CardMockResponse cardTemplate() {
        return new CardMockResponse(CARD_TEMPLATE);
    }

    public String build() {
        JsonArray array = new JsonArray();
        offers.forEach(o -> array.add(o.getOffer()));
        template.getAsJsonArray("data").get(0).getAsJsonObject().add("offers", array);
        return template.toString();
    }
}
