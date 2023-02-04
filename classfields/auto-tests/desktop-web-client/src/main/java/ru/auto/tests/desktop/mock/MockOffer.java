package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockOffer {

    public static final String OWNER_CAR_EXAMPLE = "mocksConfigurable/offer/OwnerOfferCar.json";
    public static final String CAR_EXAMPLE = "mocksConfigurable/offer/OfferCarFromSalon.json";
    public static final String CAR_PRIVATE_SELLER = "mocksConfigurable/offer/OfferCarPrivateSeller.json";
    public static final String CAR_DEALER = "mocksConfigurable/offer/OfferCarDealer.json";

    private static final String METRO_FILI = "mocksConfigurable/offer/MetroFili.json";

    private static final String OFFER = "offer";
    private static final String ADDITIONAL_INFO = "additional_info";
    private static final String CAR_INFO = "car_info";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject response;

    private MockOffer(String pathToTemplate) {
        this.response = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockOffer mockOffer(String pathToTemplate) {
        return new MockOffer(pathToTemplate);
    }

    public static MockOffer ownerCar() {
        return mockOffer(OWNER_CAR_EXAMPLE);
    }

    @Step("Добавляем acceptable_for_garage = «{isAcceptable}»")
    public MockOffer setAcceptableForGarage(boolean isAcceptable) {
        JsonObject garageInfo = new JsonObject();
        garageInfo.addProperty("acceptable_for_garage", isAcceptable);

        response.getAsJsonObject(OFFER).getAsJsonObject(ADDITIONAL_INFO).add("garage_info", garageInfo);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockOffer setId(String id) {
        response.getAsJsonObject(OFFER).addProperty("id", id);
        return this;
    }

    @Step("Добавляем engine_type = «{engineType}»")
    public MockOffer setEngineType(String engineType) {
        response.getAsJsonObject(OFFER).getAsJsonObject(CAR_INFO).addProperty("engine_type", engineType);
        response.getAsJsonObject(OFFER).getAsJsonObject(CAR_INFO).getAsJsonObject("tech_param")
                .addProperty("engine_type", engineType);
        return this;
    }

    @Step("Добавляем other_offers_show_info.encrypted_user_id = «{userId}»")
    public MockOffer setEncryptedUserId(String userId) {
        JsonObject otherOffersShowInfo = new JsonObject();
        otherOffersShowInfo.addProperty("encrypted_user_id", userId);

        response.getAsJsonObject(OFFER).getAsJsonObject(ADDITIONAL_INFO)
                .add("other_offers_show_info", otherOffersShowInfo);
        return this;
    }

    @Step("Добавляем метро «Фили» к моку оффера")
    public MockOffer setMetroFili() {
        JsonArray metro = new JsonArray();
        metro.add(new GsonBuilder().create().fromJson(getResourceAsString(METRO_FILI), JsonObject.class));

        response.getAsJsonObject(OFFER).getAsJsonObject("seller").getAsJsonObject("location").add("metro", metro);
        return this;
    }

    @Step("Добавляем цену = «{price}»")
    public MockOffer setPrice(int price) {
        response.getAsJsonObject(OFFER).getAsJsonObject("price_info").addProperty("rur_price", price);
        return this;
    }

}
