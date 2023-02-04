package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockPublicProfileSnippet {

    public static final String PROFILE_BASIC_SNIPPET = "mock/publicProfile/profileBasicSnippet.json";
    public static final String PROFILE_REZUME_SNIPPET = "mock/publicProfile/profileSnippetRezume.json";
    public static final String PROFILE_SNIPPET_PREVIEW = "mock/publicProfile/profileSnippetPreview.json";

    private static final String TYPENAME = "__typename";
    private static final String PRICE_RUR = "priceRur";
    private static final String PRICE = "price";
    private static final String CURRENT_PRICE = "currentPrice";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject snippet;

    private MockPublicProfileSnippet(String pathToSnippet) {
        this.snippet = new Gson().fromJson(getResourceAsString(pathToSnippet), JsonObject.class);
    }

    public static MockPublicProfileSnippet mockSnippet(String pathToSnippet) {
        return new MockPublicProfileSnippet(pathToSnippet);
    }

    @Step("Добавляем price = «{price}»")
    public MockPublicProfileSnippet setPrice(long price) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(PRICE_RUR, price);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "InCurrency");
        return this;
    }

    @Step("Добавляем зарплату = «{sallaryPrice}»")
    public MockPublicProfileSnippet setSallaryPrice(String sallaryPrice) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Salary");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty("salaryRur", sallaryPrice);
        return this;
    }

    @Step("Устанавливаем стоимость - даром")
    public MockPublicProfileSnippet setFreePrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Free");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Устанавливаем стоимость - цена не указана")
    public MockPublicProfileSnippet setUnsetPrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Unset");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Добавляем «{count}» фото")
    public MockPublicProfileSnippet addPhoto(int count) {
        JsonArray photos = new JsonArray();
        for (int i = 0; i < count; i++) {
            JsonObject photoItem = new GsonBuilder().create().fromJson(
                    getResourceAsString(PROFILE_SNIPPET_PREVIEW), JsonObject.class);
            photos.add(photoItem);
        }
        snippet.getAsJsonObject().add("photos", photos);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockPublicProfileSnippet setId(String id) {
        snippet.addProperty("id", id);
        return this;
    }

}
