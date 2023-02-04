package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDealerPriceOffer {

    public static final String PRICE_OFFER_EXAMPLE = "mocksConfigurable/dealer/PriceOfferExample.json";

    private static final String DISCOUNT_OPTIONS = "discount_options";

    @Getter
    @Setter
    private JsonObject body;

    private MockDealerPriceOffer(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockDealerPriceOffer dealerPriceOffer() {
        return new MockDealerPriceOffer(PRICE_OFFER_EXAMPLE);
    }

    @Step("Добавляем price = «{price}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setPrice(int price) {
        body.getAsJsonObject("price_info").addProperty("price", price);
        return this;
    }

    @Step("Добавляем tradein = «{tradein}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setTradein(int tradein) {
        body.getAsJsonObject(DISCOUNT_OPTIONS).addProperty("tradein", tradein);
        return this;
    }

    @Step("Добавляем insurance = «{insurance}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setInsurance(int insurance) {
        body.getAsJsonObject(DISCOUNT_OPTIONS).addProperty("insurance", insurance);
        return this;
    }

    @Step("Добавляем credit = «{credit}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setCredit(int credit) {
        body.getAsJsonObject(DISCOUNT_OPTIONS).addProperty("credit", credit);
        return this;
    }

    @Step("Добавляем max_discount = «{maxDiscount}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setMaxDiscount(int maxDiscount) {
        body.getAsJsonObject(DISCOUNT_OPTIONS).addProperty("max_discount", maxDiscount);
        return this;
    }

    @Step("Добавляем feedprocessor_unique_id = «{feedprocessorUniqueId}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setFeedprocessorUniqueId(String feedprocessorUniqueId) {
        body.addProperty("feedprocessor_unique_id", feedprocessorUniqueId);
        return this;
    }

    @Step("Добавляем id = «{id}» к моку оффера для отчёта по ценам")
    public MockDealerPriceOffer setId(String id) {
        body.addProperty("id", id);
        return this;
    }

    @Step("Убираем скидки у мока оффера для отчёта по ценам")
    public MockDealerPriceOffer setNoDiscounts() {
        JsonObject discountOptions = new JsonObject();

        body.add("discount_options", discountOptions);
        return this;
    }

}
