package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockUserOffer {

    public static final String APARTMENT_SELL_USER_OFFERS = "mock/useroffers/ApartmentSellUserOffers.json";
    public static final String SELL_NEW_BUILDING_SECONDARY_USER_OFFERS = "";
    public static final String SELL_HOUSE_USER_OFFERS = "mock/useroffers/HouseSellUserOffers.json";
    public static final String SELL_COMMERCIAL_USER_OFFERS = "mock/useroffers/CommercialSellUserOffers.json";
    public static final String SELL_NEW_SECONDARY_USER_OFFERS = "";
    public static final String SELL_ROOM_USER_OFFERS = "mock/useroffers/RoomSellUserOffers.json";
    public static final String SELL_LOT_USER_OFFERS = "mock/useroffers/LotSellUserOffers.json";
    public static final String SELL_GARAGE_USER_OFFERS = "mock/useroffers/GarageSellUserOffers.json";

    public static final String RENT_APARTMENT_USER_OFFERS = "mock/useroffers/ApartmentRentUserOffers.json";
    public static final String RENT_BY_DAY_USER_OFFERS = "";
    public static final String RENT_ROOM_USER_OFFERS = "mock/useroffers/RoomRentUserOffers.json";
    public static final String RENT_HOUSE_USER_OFFERS = "mock/useroffers/HouseRentUserOffers.json";
    public static final String RENT_GARAGE_USER_OFFERS = "mock/useroffers/GarageRentUserOffers.json";
    public static final String RENT_COMMERCIAL_USER_OFFERS = "mock/useroffers/CommercialRentUserOffers.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject offer;

    private MockUserOffer(String offer) {
        this.offer = new GsonBuilder().create().fromJson(getResourceAsString(offer), JsonObject.class);
    }

    public static MockUserOffer mockUserOffer(String pathToOffer) {
        return new MockUserOffer(pathToOffer);
    }

    @Step("Получаем id оффера")
    public String getOfferId() {
        return offer.getAsJsonPrimitive("id").getAsString();
    }
}
