package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockRegions {

    private static final String REGIONS = "mock/regions.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject regions;

    private MockRegions(String pathToTemplate) {
        this.regions = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockRegions regionsTemplate() {
        return new MockRegions(REGIONS);
    }

    public String build() {
        return regions.toString();
    }

}
