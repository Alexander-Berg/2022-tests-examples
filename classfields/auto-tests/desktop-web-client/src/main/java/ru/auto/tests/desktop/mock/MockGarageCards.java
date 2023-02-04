package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockGarageCards {

    public static final String GARAGE_CARDS_REQUEST = "mocksConfigurable/garage/GarageCardsRequest.json";

    private static final String LISTING = "listing";

    @Getter
    @Setter
    JsonObject body;

    @Getter
    private List<MockGarageCard> garageCards;

    private MockGarageCards() {
        garageCards = new ArrayList<>();
        JsonArray listing = new JsonArray();
        JsonObject body = new JsonObject();

        body.add(LISTING, listing);
        this.body = body;
    }

    public static MockGarageCards garageCards() {
        return new MockGarageCards();
    }

    public MockGarageCards setCards(MockGarageCard... garageCards) {
        this.garageCards.addAll(Arrays.asList(garageCards));
        return this;
    }

    public JsonObject build() {
        garageCards.forEach(card -> body.getAsJsonArray(LISTING).add(card.getBody().get("card")));
        body.addProperty("status", "SUCCESS");
        return body;
    }

    public static JsonObject getGarageCardsRequest() {
        return new GsonBuilder().create().fromJson(getResourceAsString(GARAGE_CARDS_REQUEST), JsonObject.class);
    }

}
