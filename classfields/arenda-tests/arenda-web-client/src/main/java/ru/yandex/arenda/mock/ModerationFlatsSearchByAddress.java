package ru.yandex.arenda.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class ModerationFlatsSearchByAddress {

    public static final String SEARCH_BY_ADDRESS_RESPONSE_PATH = "mock/moderation/flats/searchbyaddress/search_by_address_stub.json";

    private JsonObject template;

    private ModerationFlatsSearchByAddress(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static ModerationFlatsSearchByAddress searchByAddressTemplate() {
        return new ModerationFlatsSearchByAddress(SEARCH_BY_ADDRESS_RESPONSE_PATH);
    }

    public ModerationFlatsSearchByAddress addSecondFlat() {
        JsonObject secondFlat = new GsonBuilder().create().fromJson(getResourceAsString(
                "mock/moderation/flats/searchbyaddress/second_flat.json"), JsonObject.class);
        template.getAsJsonObject("response").getAsJsonArray("flats").add(secondFlat);
        return this;
    }

    public ModerationFlatsSearchByAddress add3OfflineShowings() {
        JsonObject showings = new GsonBuilder().create().fromJson(getResourceAsString(
                "mock/moderation/flats/searchbyaddress/three_showings_offline.json"), JsonObject.class);
        template.getAsJsonObject("response").getAsJsonArray("flats").get(0).getAsJsonObject().add("showings", showings);
        return this;
    }

    public ModerationFlatsSearchByAddress add3OnlineShowings() {
        JsonObject showings = new GsonBuilder().create().fromJson(getResourceAsString(
                "mock/moderation/flats/searchbyaddress/three_showings_online.json"), JsonObject.class);
        template.getAsJsonObject("response").getAsJsonArray("flats").get(0).getAsJsonObject().add("showings", showings);
        return this;
    }

    public String build() {
        return template.toString();
    }
}
