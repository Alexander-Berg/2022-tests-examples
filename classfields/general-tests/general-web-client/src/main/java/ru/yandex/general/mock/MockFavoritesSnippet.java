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

public class MockFavoritesSnippet {

    public static final String BASIC_SNIPPET = "mock/favoritesSnippets/basicSnippet.json";
    private static final String PREVIEW_PHOTO_ITEM = "mock/favoritesSnippets/previewItem.json";

    private static final String TYPENAME = "__typename";
    private static final String PRICE_RUR = "priceRur";
    private static final String PRICE = "price";
    private static final String CURRENT_PRICE = "currentPrice";
    private static final String PHOTOS = "photos";

    private String pathToSnippet;

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject snippet;

    private MockFavoritesSnippet(String pathToSnippet) {
        this.pathToSnippet = pathToSnippet;
    }

    public MockFavoritesSnippet getMockSnippet() {
        snippet = new Gson().fromJson(getResourceAsString(pathToSnippet), JsonObject.class);
        return this;
    }

    public static MockFavoritesSnippet mockSnippet(String pathToSnippet) {
        return new MockFavoritesSnippet(pathToSnippet);
    }

    @Step("Добавляем id = «{id}»")
    public MockFavoritesSnippet setId(String id) {
        snippet.addProperty("id", id);
        return this;
    }

    @Step("Добавляем offerOrigin = «{offerOrigin}»")
    public MockFavoritesSnippet setOfferOrigin(String offerOrigin) {
        snippet.addProperty("offerOrigin", offerOrigin);
        return this;
    }

    @Step("Добавляем offerVersion = «{offerVersion}»")
    public MockFavoritesSnippet setOfferVersion(String offerVersion) {
        snippet.addProperty("offerVersion", offerVersion);
        return this;
    }

    @Step("Добавляем categoryId = «{categoryId}»")
    public MockFavoritesSnippet setCategoryId(String categoryId) {
        snippet.getAsJsonObject("category").addProperty("id", categoryId);
        return this;
    }

    @Step("Добавляем mainColor = «{mainColor}»")
    public MockFavoritesSnippet setMainColor(String mainColor) {
        snippet.addProperty("mainColor", mainColor);
        return this;
    }

    @Step("Добавляем price = «{price}»")
    public MockFavoritesSnippet setPrice(long price) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(PRICE_RUR, price);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "InCurrency");
        return this;
    }

    @Step("Добавляем зарплату = «{sallaryPrice}»")
    public MockFavoritesSnippet setSallaryPrice(String sallaryPrice) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Salary");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty("salaryRur", sallaryPrice);
        return this;
    }

    @Step("Устанавливаем стоимость - даром")
    public MockFavoritesSnippet setFreePrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Free");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Устанавливаем стоимость - цена не указана")
    public MockFavoritesSnippet setUnsetPrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Unset");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Добавляем «{count}» фото")
    public MockFavoritesSnippet addPhoto(int count) {
        JsonArray photos = new JsonArray();
        for (int i = 0; i < count; i++) {
            JsonObject photoItem = new GsonBuilder().create().fromJson(
                    getResourceAsString(PREVIEW_PHOTO_ITEM), JsonObject.class);
            photos.add(photoItem);
        }
        snippet.getAsJsonObject().add("photos", photos);
        return this;
    }

    @Step("Удаляем фотографии из сохраненного оффера")
    public MockFavoritesSnippet removePhotos() {
        JsonArray photos = new JsonArray();
        snippet.add(PHOTOS, photos);
        return this;
    }

    @Step("Устанавливаем preferContactWay = «{contactWay}»")
    public MockFavoritesSnippet setPreferContactWay(String contactWay) {
        snippet.getAsJsonObject("contacts").addProperty("preferContactWay", contactWay);
        return this;
    }

}
