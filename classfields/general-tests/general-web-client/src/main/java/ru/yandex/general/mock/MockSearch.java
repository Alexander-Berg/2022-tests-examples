package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.simple.JSONArray;

import java.util.List;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.utils.Utils.getRandomOfferId;

public class MockSearch {

    private static final String LISTING_CATEGORY_TEMPLATE = "mock/listing/listingTemplate.json";
    private static final String LISTING_REZUME_TEMPLATE = "mock/listing/listingRezumeTemplate.json";
    public static final String LISTING_VAKANCII_RAZRABOTCHIK_TEMPLATE = "mock/listing/vakanciiRazrabotchikListingTemplate.json";
    public static final String TEXT_SEARCH_TEMPLATE = "mock/listing/textSearchTemplate.json";


    private static final String ADULT_LISTING_EXAMPLE = "mock/listing/adultListingExample.json";
    private static final String FULL_LISTING = "mock/listing/fullListing.json";
    private static final String FINAL_CATEGORY_LISTING = "mock/listing/finalCategoryListing.json";

    private static final String LISTING = "listing";
    private static final String STATISTICS = "statistics";

    @Getter
    @Setter
    private JsonObject search;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private List<MockListingSnippet> offers;

    private MockSearch(String pathToTemplate) {
        this.search = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockSearch mockSearch(String pathToSearch) {
        return new MockSearch(pathToSearch);
    }

    public static MockSearch listingCategoryResponse() {
        return new MockSearch(LISTING_CATEGORY_TEMPLATE);
    }

    public static MockSearch listingRezumeResponse() {
        return new MockSearch(LISTING_REZUME_TEMPLATE);
    }

    public static MockSearch adultListingResponse() {
        return new MockSearch(ADULT_LISTING_EXAMPLE);
    }

    public static MockSearch finalCategoryListingResponse() {
        return new MockSearch(FINAL_CATEGORY_LISTING);
    }

    public static MockSearch categoryListingExample() {
        return new MockSearch(FULL_LISTING);
    }

    @Step("Добавляем category.forAdults = «{forAdults}»")
    public MockSearch setCategoryForAdults(boolean forAdults) {
        search.getAsJsonObject("category").addProperty("forAdults", forAdults);
        return this;
    }

    @Step("Добавляем статистику цены min = «{min}», max = «{max}»")
    public MockSearch setPriceStatictics(int min, int max) {
        JsonObject price = new JsonObject();
        price.addProperty("min", min);
        price.addProperty("max", max);
        search.getAsJsonObject(STATISTICS).add("price", price);
        return this;
    }

    @Step("Добавляем статистику цены = null")
    public MockSearch setNullPriceStatictics() {
        search.getAsJsonObject(STATISTICS).add("price", null);
        return this;
    }

    @Step("Добавляем кол-во продавцов = «{count}»")
    public MockSearch setSellersCount(int count) {
        search.getAsJsonObject(STATISTICS).addProperty("sellersCount", count);
        return this;
    }

    @Step("Добавляем «{count}» офферов")
    public MockSearch addOffers(int count) {
        offers = new JSONArray();
        for (int i = 0; i < count; i++) {
            offers.add(mockSnippet(BASIC_SNIPPET).getMockSnippet().setId(getRandomOfferId()));
        }
        return this;
    }

    @Step("Добавляем request.text = «{text}»")
    public MockSearch setRequestText(String text) {
        search.getAsJsonObject("request").addProperty("text", text);
        return this;
    }

    public String build() {
        if (offers != null) {
            JsonArray offersArray = new JsonArray();
            offers.forEach(o -> offersArray.add(o.getSnippet()));
            search.getAsJsonObject(LISTING).addProperty("totalCount", offersArray.size());
            search.getAsJsonObject(LISTING).add("snippets", offersArray);
        }

        return search.toString();
    }

}
