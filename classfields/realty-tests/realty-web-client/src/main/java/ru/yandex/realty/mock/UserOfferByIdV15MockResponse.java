package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class UserOfferByIdV15MockResponse {

    public static final String CARD_TEMPLATE = "mock/getUserOfferByIdV15template.json";

    private JsonObject template;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockOffer> offers;

    private UserOfferByIdV15MockResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static UserOfferByIdV15MockResponse userOfferByIdV15Template() {
        return new UserOfferByIdV15MockResponse(CARD_TEMPLATE);
    }

    public String build() {
        JsonArray array = new JsonArray();
        offers.forEach(o -> array.add(o.getOffer()));
        template.getAsJsonObject("response").add("offers", array);
        return template.toString();
    }
}
