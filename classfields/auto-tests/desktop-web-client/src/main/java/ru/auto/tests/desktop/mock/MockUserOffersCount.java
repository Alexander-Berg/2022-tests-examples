package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockUserOffersCount {

    private static final String USER_OFFERS_COUNT_TEMPLATE = "mocksConfigurable/user/OffersCountExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockUserOffersCount(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserOffersCount offersCount() {
        return new MockUserOffersCount(USER_OFFERS_COUNT_TEMPLATE);
    }

    public MockUserOffersCount setCount(int count) {
        body.addProperty("count", count);
        return this;
    }

}
