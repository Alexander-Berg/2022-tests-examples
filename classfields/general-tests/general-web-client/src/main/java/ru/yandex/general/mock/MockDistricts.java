package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDistricts {

    private static final String DISTRICTS = "mock/districts.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject districts;

    private MockDistricts(String pathToTemplate) {
        this.districts = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockDistricts districtsTemplate() {
        return new MockDistricts(DISTRICTS);
    }

    public String build() {
        return districts.toString();
    }

}
