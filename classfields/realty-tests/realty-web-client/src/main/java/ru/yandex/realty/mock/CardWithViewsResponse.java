package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class CardWithViewsResponse {

    public static final String CARD_WITH_VIEWS = "mock/cardWithViews.json";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private MockOffer offer;

    private CardWithViewsResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static CardWithViewsResponse cardWithViewsTemplate() {
        return new CardWithViewsResponse(CARD_WITH_VIEWS);
    }

    public String build() {
        template.add("response", offer.getOffer());
        return template.toString();
    }
}
