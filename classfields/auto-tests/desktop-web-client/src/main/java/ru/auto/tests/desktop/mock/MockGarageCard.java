package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static java.lang.String.format;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockGarageCard {

    public static final String USER_CARD_OFFER_EXAMPLE = "mocksConfigurable/garage/UserCardOffer.json";
    public static final String USER_CARD_OFFER_REQUEST = "mocksConfigurable/garage/UserCardOfferPutRequest.json";
    public static final String TESLA_MODEL_3_GARAGE_CARD = "mocksConfigurable/garage/TeslaModel3GarageCard.json";
    public static final String TESLA_MODEL_3_REQUEST_TO_DREAM_CAR = "mocksConfigurable/garage/TeslaModel3RequestToDreamCar.json";
    public static final String GARAGE_PUBLIC_CARD = "mocksConfigurable/garage/GarageCardPublic.json";
    public static final String GARAGE_ARTICLE_CARD = "mocksConfigurable/garage/GarageArticleCard.json";

    private static final String CARD = "card";
    private static final String OFFER_INFO = "offer_info";
    private static final String VEHICLE_INFO = "vehicle_info";
    private static final String CAR_INFO = "car_info";
    private static final String BODY_TYPE = "body_type";
    private static final String REGION_INFO = "region_info";
    private static final String TAX = "tax";
    private static final String PREDICT = "predict";
    private static final String PRICE_PREDICT = "price_predict";

    public static final String EX_CAR = "EX_CAR";
    public static final String DREAM_CAR = "DREAM_CAR";
    public static final String CURRENT_CAR = "CURRENT_CAR";

    public static final String OK = "OK";
    public static final String CAN_BE_CLARIFIED = "CAN_BE_CLARIFIED";
    public static final String NOT_ENOUGH_DATA  = "NOT_ENOUGH_DATA";

    public static final String SEDAN = "SEDAN";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockGarageCard(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockGarageCard garageCard(String pathToTemplate) {
        return new MockGarageCard(pathToTemplate);
    }

    public static MockGarageCard garageCardOffer() {
        return garageCard(USER_CARD_OFFER_EXAMPLE);
    }

    public static MockGarageCard garageCardRequest() {
        return garageCard(USER_CARD_OFFER_REQUEST);
    }

    public static MockGarageCard garageArticleCard() {
        return garageCard(GARAGE_ARTICLE_CARD);
    }

    @Step("Добавляем id = «{id}» в моке карточки гаража")
    public MockGarageCard setId(String id) {
        body.getAsJsonObject(CARD).addProperty("id", id);
        body.getAsJsonObject(CARD).addProperty("share_url",
                format("https://test.avto.ru/garage/share/%s", id));
        return this;
    }

    @Step("Добавляем offer_info.price = «{price}» в моке карточки гаража")
    public MockGarageCard setPrice(int price) {
        body.getAsJsonObject(CARD).getAsJsonObject(OFFER_INFO).addProperty("price", price);
        return this;
    }

    @Step("Добавляем offer_info.offer_id = «{offerId}» в моке карточки гаража")
    public MockGarageCard setOfferId(String offerId) {
        body.getAsJsonObject(CARD).getAsJsonObject(OFFER_INFO).addProperty("offer_id", offerId);
        return this;
    }

    @Step("Добавляем card_type = «{cardType}» в моке карточки гаража")
    public MockGarageCard setCardType(String cardType) {
        JsonObject cardTypeInfo = new JsonObject();
        cardTypeInfo.addProperty("card_type", cardType);

        body.getAsJsonObject(CARD).add("card_type_info", cardTypeInfo);
        return this;
    }

    @Step("Добавляем стоимость налога = «{tax}» в моке карточки гаража")
    public MockGarageCard setTaxAmount(int tax) {
        body.getAsJsonObject(CARD).getAsJsonObject(TAX).addProperty(TAX, tax);
        return this;
    }

    @Step("Добавляем имя региона в предложном падеже = «{regionName}» в моке карточки гаража")
    public MockGarageCard setTaxRegionInfoNamePrepositional(String regionName) {
        body.getAsJsonObject(CARD).getAsJsonObject(TAX).getAsJsonObject(REGION_INFO)
                .addProperty("prepositional", regionName);
        return this;
    }

    @Step("Добавляем block_state = «{status}» в моке карточки гаража")
    public MockGarageCard setTaxBlockState(String status) {
        JsonObject block_state = new JsonObject();
        block_state.addProperty("status", status);

        body.getAsJsonObject(CARD).getAsJsonObject(TAX).add("block_state", block_state);
        return this;
    }

    @Step("Добавляем имя региона = «{regionName}» в моке карточки гаража")
    public MockGarageCard setTaxRegionInfoName(String regionName) {
        body.getAsJsonObject(CARD).getAsJsonObject(TAX).getAsJsonObject(REGION_INFO)
                .addProperty("name", regionName);
        return this;
    }

    @Step("Добавляем registration_region, с id = «{id}» и name = «{name}» в моке карточки гаража")
    public MockGarageCard setRegistrationRegion(int id, String name) {
        JsonObject registrationRegion = new JsonObject();
        registrationRegion.addProperty("id", id);
        registrationRegion.addProperty("name", name);

        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).add("registration_region", registrationRegion);
        return this;
    }

    @Step("Добавляем tech_param_id = «{techParamId}» в моке карточки гаража")
    public MockGarageCard setTechParamId(String techParamId) {
        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).getAsJsonObject(CAR_INFO)
                .addProperty("tech_param_id", techParamId);
        return this;
    }

    @Step("Добавляем tech_param.power = «{power}» в моке карточки гаража")
    public MockGarageCard setPower(int power) {
        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).getAsJsonObject(CAR_INFO)
                .getAsJsonObject("tech_param").addProperty("power", power);
        return this;
    }

    @Step("Добавляем body_type = «{bodyType}» в моке карточки гаража")
    public MockGarageCard setBodyType(String bodyType) {
        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).getAsJsonObject(CAR_INFO)
                .addProperty(BODY_TYPE, bodyType);
        return this;
    }

    @Step("Убираем body_type в моке карточки гаража")
    public MockGarageCard removeBodyType() {
        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).getAsJsonObject(CAR_INFO).remove(BODY_TYPE);
        return this;
    }

    @Step("Добавляем название марки = «{name}»")
    public MockGarageCard setMarkName(String name) {
        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).getAsJsonObject(CAR_INFO)
                .getAsJsonObject("mark_info").addProperty("name", name);
        return this;
    }

    @Step("Добавляем название модели = «{name}»")
    public MockGarageCard setModelName(String name) {
        body.getAsJsonObject(CARD).getAsJsonObject(VEHICLE_INFO).getAsJsonObject(CAR_INFO)
                .getAsJsonObject("model_info").addProperty("name", name);
        return this;
    }

    @Step("Добавляем цену выкупа = «{price}»")
    public MockGarageCard setTradeinPrice(int price) {
        int priceDiff = 10000;
        int priceProm = price - priceDiff;
        int priceTo = price + priceDiff;

        body.getAsJsonObject(CARD).getAsJsonObject(PRICE_PREDICT).getAsJsonObject(PREDICT)
                .getAsJsonObject("tradein_dealer_matrix_buyout").addProperty("from", priceProm);
        body.getAsJsonObject(CARD).getAsJsonObject(PRICE_PREDICT).getAsJsonObject(PREDICT)
                .getAsJsonObject("tradein_dealer_matrix_buyout").addProperty("to", priceTo);
        return this;
    }

    @Step("Добавляем market.price = «{price}»")
    public MockGarageCard setMarketPrice(int price) {
        body.getAsJsonObject(CARD).getAsJsonObject(PRICE_PREDICT).getAsJsonObject(PREDICT)
                .getAsJsonObject("market").addProperty("price", price);
        return this;
    }

    @Step("Добавляем added_manually = «{isAddedManually}»")
    public MockGarageCard setAddedManually(boolean isAddedManually) {
        body.addProperty("added_manually", isAddedManually);
        return this;
    }

    public MockGarageCard setStatusSuccess() {
        body.addProperty("status", "SUCCESS");
        return this;
    }

}
