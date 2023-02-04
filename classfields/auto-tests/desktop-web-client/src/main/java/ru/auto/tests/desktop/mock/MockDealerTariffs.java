package ru.auto.tests.desktop.mock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import static java.util.Arrays.stream;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockDealerTariffs {

    private static final String TARIFFS = "tariffs";

    @Getter
    @Setter
    private JsonObject body;

    private MockDealerTariffs() {
        JsonObject body = new JsonObject();
        body.addProperty("status", "SUCCESS");
        body.addProperty("editable", true);

        JsonArray tariffs = new JsonArray();
        body.add(TARIFFS, tariffs);

        this.body = body;
    }

    public static MockDealerTariffs dealerTariffs() {
        return new MockDealerTariffs();
    }

    public MockDealerTariffs setTariffs(MockDealerTariff... tariffs) {
        stream(tariffs).forEach(tariff -> body.getAsJsonArray(TARIFFS).add(getJsonObject(tariff.getBody())));
        return this;
    }

}
