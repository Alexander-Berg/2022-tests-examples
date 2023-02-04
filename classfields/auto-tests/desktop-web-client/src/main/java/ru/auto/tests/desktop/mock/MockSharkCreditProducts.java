package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_GEO_ID;

public class MockSharkCreditProducts {

    public static final String CREDIT_PRODUCTS_EXAMPLE = "mocksConfigurable/shark/CreditProductsExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject response;

    private MockSharkCreditProducts(String pathToTemplate) {
        this.response = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockSharkCreditProducts creditProducts() {
        return new MockSharkCreditProducts(CREDIT_PRODUCTS_EXAMPLE);
    }

    public static JsonObject requestGeoBaseIdsBody() {
        JsonArray geobaseIds = new JsonArray();
        geobaseIds.add(Integer.valueOf(MOSCOW_GEO_ID));

        JsonObject byGeo = new JsonObject();
        byGeo.add("geobase_ids", geobaseIds);

        JsonObject body = new JsonObject();
        body.add("by_geo", byGeo);

        return body;
    }

    public static JsonObject requestAllBody() {
        JsonObject all = new JsonObject();
        JsonObject body = new JsonObject();
        body.add("all", all);
        return body;
    }

}
