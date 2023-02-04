package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockPromos {

    public static final String PROMOS_EXAMPLE = "mocksConfigurable/promos/PromosExample.json";

    private static final String PARTNER_PROMOS = "partner_promos";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockPromos(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockPromos mockPromos(String pathToTemplate) {
        return new MockPromos(pathToTemplate);
    }

    public static MockPromos promos() {
        return mockPromos(PROMOS_EXAMPLE);
    }

    @Step("Добавляем url = «{url}» к айтему = «{item}» в моке промо")
    public MockPromos setUrlForItem(int item, String url) {
        body.getAsJsonArray(PARTNER_PROMOS).get(item).getAsJsonObject().addProperty("url", url);
        return this;
    }

}
