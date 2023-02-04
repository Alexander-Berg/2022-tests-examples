package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockAutoruDealer {

    public static final String AUTORU_DEALER_BMW_EXAMPLE = "mocksConfigurable/dealer/AutoruDealerBMWExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    private JsonObject body;

    private MockAutoruDealer(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockAutoruDealer mockAutoruDealer(String pathToTemplate) {
        return new MockAutoruDealer(pathToTemplate);
    }

    public static MockAutoruDealer autoruDealer() {
        return mockAutoruDealer(AUTORU_DEALER_BMW_EXAMPLE);
    }

}
