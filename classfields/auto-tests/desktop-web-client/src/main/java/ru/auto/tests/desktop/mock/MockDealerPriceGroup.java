package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockDealerPriceGroup {

    public static final String PRICE_GROUP_EXAMPLE = "mocksConfigurable/dealer/PriceGroupExample.json";

    private static final String DEALER = "dealer";
    private static final String COMPETITOR = "competitor";
    private static final String COUNTER = "counter";
    private static final String PRICE_RANGE = "price_range";
    private static final String IN_STOCK = "in_stock";
    private static final String ON_ORDER = "on_order";
    private static final String MIN_PRICE_DISCOUNT = "min_price_discount";
    private static final String MIN_PRICE = "min_price";
    private static final String MAX_PRICE = "max_price";
    private static final String DISCOUNT_STATISTIC = "discount_statistic";

    public static final String UNIQUE_OFFERS = "UNIQUE_OFFERS";
    public static final String NO_DISCOUNTS = "NO_DISCOUNTS";
    public static final String ABSENT_OFFERS = "ABSENT_OFFERS";

    @Getter
    @Setter
    private JsonObject body;

    private MockDealerPriceGroup(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockDealerPriceGroup dealerPriceGroup() {
        return new MockDealerPriceGroup(PRICE_GROUP_EXAMPLE);
    }

    @Step("Добавляем mark = «{mark}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setMark(String mark) {
        body.addProperty("mark", mark);
        return this;
    }

    @Step("Добавляем mark_id = «{markId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setMarkId(String markId) {
        body.addProperty("mark_id", markId);
        return this;
    }

    @Step("Добавляем model = «{model}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setModel(String model) {
        body.addProperty("model", model);
        return this;
    }

    @Step("Добавляем model_id = «{modelId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setModelId(String modelId) {
        body.addProperty("model_id", modelId);
        return this;
    }

    @Step("Добавляем super_gen = «{superGen}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setSuperGen(String superGen) {
        body.addProperty("super_gen", superGen);
        return this;
    }

    @Step("Добавляем super_gen_id = «{superGenId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setSuperGenId(String superGenId) {
        body.addProperty("super_gen_id", superGenId);
        return this;
    }

    @Step("Добавляем configuration = «{configuration}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setConfiguration(String configuration) {
        body.addProperty("configuration", configuration);
        return this;
    }

    @Step("Добавляем configuration_id = «{configurationId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setConfigurationId(String configurationId) {
        body.addProperty("configuration_id", configurationId);
        return this;
    }

    @Step("Добавляем tech_param = «{techParam}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setTechParam(String techParam) {
        body.addProperty("tech_param", techParam);
        return this;
    }

    @Step("Добавляем tech_param_id = «{techParamId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setTechParamId(String techParamId) {
        body.addProperty("tech_param_id", techParamId);
        return this;
    }

    @Step("Добавляем engine_type = «{engineType}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setEngineType(String engineType) {
        body.addProperty("engine_type", engineType);
        return this;
    }

    @Step("Добавляем transmission = «{transmission}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setTransmission(String transmission) {
        body.addProperty("transmission", transmission);
        return this;
    }

    @Step("Добавляем drive = «{drive}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDrive(String drive) {
        body.addProperty("drive", drive);
        return this;
    }

    @Step("Добавляем complectation = «{complectation}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setComplectation(String complectation) {
        body.addProperty("complectation", complectation);
        return this;
    }

    @Step("Добавляем complectation_id = «{complectationId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setComplectationId(String complectationId) {
        body.addProperty("complectation_id", complectationId);
        return this;
    }

    @Step("Добавляем body_type = «{bodyType}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setBodyType(String bodyType) {
        body.addProperty("body_type", bodyType);
        return this;
    }

    @Step("Добавляем year = «{year}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setYear(int year) {
        body.addProperty("year", year);
        return this;
    }

    @Step("Добавляем dealer.counter.in_stock = «{inStock}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDealerInStock(int inStock) {
        body.getAsJsonObject(DEALER).getAsJsonObject(COUNTER).addProperty(IN_STOCK, inStock);
        return this;
    }

    @Step("Добавляем dealer.counter.on_order = «{onOrder}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDealerOnOrder(int onOrder) {
        body.getAsJsonObject(DEALER).getAsJsonObject(COUNTER).addProperty(ON_ORDER, onOrder);
        return this;
    }

    @Step("Добавляем dealer.price_range.min_price_discount = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDealerMinPriceDiscount(int price) {
        body.getAsJsonObject(DEALER).getAsJsonObject(PRICE_RANGE).addProperty(MIN_PRICE_DISCOUNT, price);
        return this;
    }

    @Step("Добавляем dealer.price_range.min_price = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDealerMinPrice(int price) {
        body.getAsJsonObject(DEALER).getAsJsonObject(PRICE_RANGE).addProperty(MIN_PRICE, price);
        return this;
    }

    @Step("Добавляем dealer.price_range.max_price = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDealerMaxPrice(int price) {
        body.getAsJsonObject(DEALER).getAsJsonObject(PRICE_RANGE).addProperty(MAX_PRICE, price);
        return this;
    }

    @Step("Добавляем competitor.counter.in_stock = «{inStock}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setCompetitorInStock(int inStock) {
        body.getAsJsonObject(COMPETITOR).getAsJsonObject(COUNTER).addProperty(IN_STOCK, inStock);
        return this;
    }

    @Step("Добавляем competitor.counter.on_order = «{onOrder}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setCompetitorOnOrder(int onOrder) {
        body.getAsJsonObject(COMPETITOR).getAsJsonObject(COUNTER).addProperty(ON_ORDER, onOrder);
        return this;
    }

    @Step("Добавляем competitor.price_range.min_price_discount = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setCompetitorMinPriceDiscount(int price) {
        body.getAsJsonObject(COMPETITOR).getAsJsonObject(PRICE_RANGE).addProperty(MIN_PRICE_DISCOUNT, price);
        return this;
    }

    @Step("Добавляем competitor.price_range.min_price = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setCompetitorMinPrice(int price) {
        body.getAsJsonObject(COMPETITOR).getAsJsonObject(PRICE_RANGE).addProperty(MIN_PRICE, price);
        return this;
    }

    @Step("Добавляем competitor.price_range.max_price = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setCompetitorMaxPrice(int price) {
        body.getAsJsonObject(COMPETITOR).getAsJsonObject(PRICE_RANGE).addProperty(MAX_PRICE, price);
        return this;
    }

    @Step("Добавляем discount_statistic.max_trade_in = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setMaxTradeIn(int price) {
        body.getAsJsonObject(DISCOUNT_STATISTIC).addProperty("max_trade_in", price);
        return this;
    }

    @Step("Добавляем discount_statistic.max_discount = «{price}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setMaxDiscount(int price) {
        body.getAsJsonObject(DISCOUNT_STATISTIC).addProperty("max_discount", price);
        return this;
    }

    @Step("Добавляем discount_statistic.dealer_id_max_discount = «{dealerId}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setDealerIdMaxDiscount(int dealerId) {
        body.getAsJsonObject(DISCOUNT_STATISTIC).addProperty("dealer_id_max_discount", dealerId);
        return this;
    }

    @Step("Добавляем preset = «{preset}» к моку группы объявлений отчёта по ценам")
    public MockDealerPriceGroup setPreset(String preset) {
        body.addProperty("preset", preset);
        return this;
    }

}
