package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockUserOffersMarkModels {

    public static final String MARK_MODELS_BMW_EXAMPLE = "mocksConfigurable/markModels/MarkModelsBMW.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    private JsonObject body;

    private MockUserOffersMarkModels(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserOffersMarkModels mockUserOffersMarkModels(String pathToTemplate) {
        return new MockUserOffersMarkModels(pathToTemplate);
    }

    public static MockUserOffersMarkModels markModelsBMW() {
        return mockUserOffersMarkModels(MARK_MODELS_BMW_EXAMPLE);
    }

}
