package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDealerPriceGroups {

    public static final String PRICE_GROUPS_EXAMPLE = "mocksConfigurable/dealer/PriceGroupsExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    private JsonObject body;

    @Getter
    private List<MockDealerPriceGroup> priceGroupsList;

    private MockDealerPriceGroups() {
        priceGroupsList = new ArrayList<>();
        body = new JsonObject();
    }

    public static MockDealerPriceGroups dealerPriceGroups() {
        return new MockDealerPriceGroups();
    }

    public static MockDealerPriceGroups dealerPriceGroups(String pathToTemplate) {
        return new MockDealerPriceGroups().setBody(
                new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class));
    }

    public MockDealerPriceGroups setPriceGroups(MockDealerPriceGroup... priceGroups) {
        priceGroupsList.addAll(Arrays.asList(priceGroups));
        return this;
    }

    public JsonObject build() {
        if (priceGroupsList.size() > 0) {
            JsonArray priceGroups = new JsonArray();
            priceGroupsList.forEach(priceGroup -> priceGroups.add(priceGroup.getBody()));

            body.add("groups", priceGroups);
        }

        return body;
    }

}
