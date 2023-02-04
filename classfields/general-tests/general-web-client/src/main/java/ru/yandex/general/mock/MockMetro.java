package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockMetro {

    private static final String METRO = "mock/metro.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject metro;

    private MockMetro(String pathToTemplate) {
        this.metro = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockMetro metroTemplate() {
        return new MockMetro(METRO);
    }

    public String build() {
        return metro.toString();
    }

}
