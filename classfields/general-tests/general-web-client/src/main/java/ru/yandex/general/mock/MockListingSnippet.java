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
import static ru.yandex.general.utils.Utils.getRandomOfferId;

public class MockListingSnippet {

    public static final String BASIC_SNIPPET = "mock/listingSnippets/basicSnippet.json";
    public static final String REZUME_SNIPPET = "mock/listingSnippets/rezumeSnippet.json";
    private static final String PREVIEW_PHOTO_ITEM = "mock/favoritesSnippets/previewItem.json";

    private static final String TYPENAME = "__typename";
    private static final String PRICE_RUR = "priceRur";
    private static final String PRICE = "price";
    private static final String CURRENT_PRICE = "currentPrice";

    public static final String SEND_WITHIN_RUSSIA = "SendWithinRussia";
    public static final String SEND_BY_COURIER = "SendByCourier";

    private String pathToSnippet;

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject snippet;

    private MockListingSnippet(String pathToSnippet) {
        this.pathToSnippet = pathToSnippet;
    }

    public MockListingSnippet getMockSnippet() {
        snippet = new Gson().fromJson(getResourceAsString(pathToSnippet), JsonObject.class);
        setId(getRandomOfferId());
        return this;
    }

    public static MockListingSnippet mockSnippet(String pathToSnippet) {
        return new MockListingSnippet(pathToSnippet);
    }

    @Step("Добавляем id = «{id}»")
    public MockListingSnippet setId(String id) {
        snippet.addProperty("id", id);
        return this;
    }

    @Step("Добавляем offerVersion = «{offerVersion}»")
    public MockListingSnippet setOfferVersion(String offerVersion) {
        snippet.addProperty("offerVersion", offerVersion);
        return this;
    }

    @Step("Добавляем categoryId = «{categoryId}»")
    public MockListingSnippet setCategoryId(String categoryId) {
        snippet.addProperty("categoryId", categoryId);
        return this;
    }

    @Step("Добавляем mainColor = «{mainColor}»")
    public MockListingSnippet setMainColor(String mainColor) {
        snippet.addProperty("mainColor", mainColor);
        return this;
    }

    @Step("Добавляем price = «{price}»")
    public MockListingSnippet setPrice(long price) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(PRICE_RUR, price);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "InCurrency");
        return this;
    }

    @Step("Добавляем зарплату = «{sallaryPrice}»")
    public MockListingSnippet setSallaryPrice(String sallaryPrice) {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Salary");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty("salaryRur", sallaryPrice);
        return this;
    }

    @Step("Устанавливаем стоимость - даром")
    public MockListingSnippet setFreePrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Free");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Устанавливаем стоимость - цена не указана")
    public MockListingSnippet setUnsetPrice() {
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).addProperty(TYPENAME, "Unset");
        snippet.getAsJsonObject(PRICE).getAsJsonObject(CURRENT_PRICE).remove(PRICE_RUR);
        return this;
    }

    @Step("Добавляем «{count}» фото")
    public MockListingSnippet addPhoto(int count) {
        JsonArray photos = new JsonArray();
        for (int i = 0; i < count; i++) {
            JsonObject photoItem = new GsonBuilder().create().fromJson(
                    getResourceAsString(PREVIEW_PHOTO_ITEM), JsonObject.class);
            photos.add(photoItem);
        }
        snippet.getAsJsonObject().add("photos", photos);
        return this;
    }

    @Step("Устанавливаем preferContactWay = «{contactWay}»")
    public MockListingSnippet setPreferContactWay(String contactWay) {
        snippet.addProperty("preferContactWay", contactWay);
        return this;
    }

    @Step("Добавляем category.forAdults = «{forAdults}»")
    public MockListingSnippet setCategoryForAdults(boolean forAdults) {
        snippet.getAsJsonObject("category").addProperty("forAdults", forAdults);
        return this;
    }

    @Step("Добавляем VAS")
    public MockListingSnippet setVas() {
        JsonArray appliedVases = new JsonArray();
        JsonObject vas = new JsonObject();
        vas.addProperty("vasType", "Raise");
        vas.addProperty("vasId", "raise_1");
        appliedVases.add(vas);
        snippet.add("appliedVases", appliedVases);
        return this;
    }

    @Step("Добавляем название «{title}»")
    public MockListingSnippet setTitle(String title) {
        snippet.addProperty("title", title);
        return this;
    }

    @Step("Добавляем cardLink.url = «{url}»")
    public MockListingSnippet setCardlinkUrl(String url) {
        snippet.getAsJsonObject("cardLink").addProperty("url", url);
        return this;
    }

    @Step("Удаляем фотографии")
    public MockListingSnippet removePhotos() {
        JsonArray photos = new JsonArray();
        snippet.add("photos", photos);
        return this;
    }

    @Step("Добавляем доставку = «{delivery}»")
    public MockListingSnippet setDelivery(String delivery) {
        snippet.addProperty("deliveryType", delivery);
        return this;
    }

    @Step("Добавляем isOwner = «{isOwner}»")
    public MockListingSnippet setIsOwner(boolean isOwner) {
        snippet.addProperty("isOwner", isOwner);
        return this;
    }

    @Step("Добавляем favorite = «{isFavorite}»")
    public MockListingSnippet setFavorite(boolean isFavorite) {
        snippet.addProperty("favorite", isFavorite);
        return this;
    }

}
