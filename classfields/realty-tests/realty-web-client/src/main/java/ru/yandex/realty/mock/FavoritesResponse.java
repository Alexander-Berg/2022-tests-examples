package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class FavoritesResponse {

    public static final String FAVORITES_TEMPLATE = "mock/favoritesTemplate.json";

    private JsonObject template;

    private FavoritesResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static FavoritesResponse favoritesTemplate() {
        return new FavoritesResponse(FAVORITES_TEMPLATE);
    }

    @Step("Добавляем «{itemId}» в списки избранного")
    public FavoritesResponse addItem(String itemId) {
        template.getAsJsonObject("response").getAsJsonArray("actual").add(itemId);
        template.getAsJsonObject("response").getAsJsonArray("relevant").add(itemId);
        return this;
    }

    public String build() {
        return template.toString();
    }
}
