package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCabinetListing {

    private static final String CABINET_LISTING = "mock/cabinetListing.json";
    private static final String CABINET_LISTING_TEMPLATE = "mock/cabinetListingTemplate.json";

    private static final String PRESET = "preset";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject cabinetListing;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockCabinetSnippet> offers;

    private MockCabinetListing(String pathToTemplate) {
        this.cabinetListing = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCabinetListing cabinetListingResponse() {
        return new MockCabinetListing(CABINET_LISTING_TEMPLATE);
    }

    public static MockCabinetListing cabinetListingExample() {
        return new MockCabinetListing(CABINET_LISTING);
    }

    @Step("Добавляем args.preset = «{preset}»")
    public MockCabinetListing setPreset(String preset) {
        JsonObject actualPreset = new JsonObject();
        JsonArray availablePresets = new JsonArray();
        actualPreset.addProperty(PRESET, preset);
        actualPreset.addProperty("count", 1);
        availablePresets.add(actualPreset);

        cabinetListing.add("availablePresets", availablePresets);
        cabinetListing.getAsJsonObject("args").addProperty(PRESET, preset);
        return this;
    }

    public String build() {
        if (offers != null) {
            JsonArray offersArray = new JsonArray();
            offers.forEach(o -> offersArray.add(o.getSnippet()));
            cabinetListing.addProperty("totalCount", offersArray.size());
            cabinetListing.add("snippets", offersArray);
        }

        return cabinetListing.toString();
    }

}
