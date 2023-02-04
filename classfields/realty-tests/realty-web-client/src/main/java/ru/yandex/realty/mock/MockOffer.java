package ru.yandex.realty.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.utils.RealtyUtils.getObjectFromJson;

public class MockOffer {

    public static final String RAISED = "raised";
    public static final String PREMIUM = "premium";
    public static final String PROMOTED = "promoted";
    public static final String TURBOSALE = "turboSale";

    public static final String SELL_APARTMENT = "mock/card/SellApartment.json";
    public static final String SELL_NEW_FLAT_SPEC_PROJECT = "mock/card/SellNewFlatSpecProject.json";
    public static final String SELL_APARTMENT_IN_SITE = "mock/card/SellApartmentInSite.json";
    public static final String SELL_NEW_BUILDING_SECONDARY = "mock/card/SellNewBuildingSecondary.json";
    public static final String SELL_HOUSE = "mock/card/SellHouse.json";
    public static final String SELL_TOWN_HOUSE = "mock/card/SellTownHouse.json";
    public static final String SELL_DUPLEX = "mock/card/SellDuplex.json";
    public static final String SELL_PART_HOUSE = "mock/card/SellPartHouse.json";
    public static final String SELL_COMMERCIAL_WAREHOUSE = "mock/card/SellCommercialWarehouse.json";
    public static final String SELL_NEW_SECONDARY = "mock/card/SellNewSecondary.json";
    public static final String SELL_ROOM = "mock/card/SellRoom.json";
    public static final String SELL_LOT = "mock/card/SellLot.json";
    public static final String SELL_GARAGE = "mock/card/SellGarage.json";
    public static final String SELL_COMMERCIAL = "mock/card/SellCommercialLot.json";
    public static final String SELL_COMMERCIAL_OFFICE = "mock/card/SellCommercialOffice.json";
    public static final String SELL_COMMERCIAL_RETAIL = "mock/card/SellCommercialRetail.json";
    public static final String SELL_COMMERCIAL_FREE_PURPOSE = "mock/card/SellCommercialFreePurpose.json";
    public static final String SELL_COMMERCIAL_MANUFACTURING = "mock/card/SellCommercialManufacturing.json";
    public static final String SELL_COMMERCIAL_PUBLIC_CATERING = "mock/card/SellCommercialPublicCatering.json";
    public static final String SELL_COMMERCIAL_BUSINESS = "mock/card/SellCommercialBusiness.json";

    public static final String SELL_APARTMENT_AD_AGENCY = "mock/card/SellApartmentAdAgency.json";

    public static final String RENT_APARTMENT = "mock/card/RentApartment.json";
    public static final String RENT_BY_DAY = "mock/card/RentByDay.json";
    public static final String RENT_ROOM = "mock/card/RentRoom.json";
    public static final String RENT_HOUSE = "mock/card/RentHouse.json";
    public static final String RENT_GARAGE = "mock/card/RentGarage.json";
    public static final String RENT_COMMERCIAL = "mock/card/RentCommercialLot.json";
    public static final String RENT_COMMERCIAL_WAREHOUSE = "mock/card/RentCommercialWarehouse.json";
    public static final String RENT_COMMERCIAL_OFFICE = "mock/card/RentCommercialOffice.json";
    public static final String RENT_COMMERCIAL_RETAIL = "mock/card/RentCommercialRetail.json";
    public static final String RENT_COMMERCIAL_FREE_PURPOSE = "mock/card/RentCommercialFreePurpose.json";
    public static final String RENT_COMMERCIAL_MANUFACTURING = "mock/card/RentCommercialManufacturing.json";
    public static final String RENT_COMMERCIAL_PUBLIC_CATERING = "mock/card/RentCommercialPublicCatering.json";
    public static final String RENT_COMMERCIAL_AUTO_REPAIR = "mock/card/RentCommercialAutoRepair.json";
    public static final String RENT_COMMERCIAL_HOTEL = "mock/card/RentCommercialHotel.json";
    public static final String RENT_COMMERCIAL_BUSINESS = "mock/card/RentCommercialBusiness.json";
    public static final String RENT_COMMERCIAL_LEGAL_ADDRESS = "mock/card/RentCommercialLegalAddress.json";

    public static final String SELL_APARTMENT_WITH_NARROW_PHOTO = "mock/card/SellApartmentNarrowPhoto.json";

    private static final String SITE_INFO = "siteInfo";
    private static final String DEVELOPERS = "developers";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject offer;

    private MockOffer(String offer) {
        this.offer = new GsonBuilder().create().fromJson(getResourceAsString(offer), JsonObject.class);
    }

    public static MockOffer mockOffer(String pathToOffer) {
        return new MockOffer(pathToOffer);
    }

    @Step("Создаем шаблон мока с параметром «extImages»")
    public MockOffer setExtImages() {
        offer.add("extImages", getObjectFromJson(JsonObject.class, "mock/card/extImages.json"));
        return this;
    }

    @Step("Создаем шаблон мока с параметром «predictions»")
    public MockOffer setPredictions() {
        offer.add("predictions", getObjectFromJson(JsonObject.class, "mock/card/predictions.json"));
        return this;
    }

    @Step("Создаем оффер с улугой {service}")
    public MockOffer setService(String service) {
        offer.getAsJsonObject("vas").addProperty(service, "true");
        return this;
    }

    @Step("Добавляем siteId = {siteId} в building")
    public MockOffer setBuildingSiteId(int siteId) {
        offer.getAsJsonObject("building").addProperty("siteId", siteId);
        return this;
    }

    @Step("Делаем оффер усттаревшим")
    public MockOffer setObsolete(LocalDateTime date) {
        offer.addProperty("active", "false");
        offer.addProperty("obsolete", "true");
        offer.addProperty("revokeDate", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return this;
    }

    @Step("Делаем цены оффера меняющимися")
    public MockOffer setIncreasedPrice() {
        offer.getAsJsonObject("history").add("prices", getObjectFromJson(JsonArray.class, "mock/card/prices.json"));
        offer.add("price", getObjectFromJson(JsonObject.class, "mock/card/increasedPrice.json"));
        return this;
    }

    @Step("Делаем цены оффера меняющимися")
    public MockOffer setDecreasedPrice() {
        offer.getAsJsonObject("history").add("prices", getObjectFromJson(JsonArray.class, "mock/card/prices.json"));
        offer.add("price", getObjectFromJson(JsonObject.class, "mock/card/decreasedPrice.json"));
        return this;
    }

    @Step("Добавляем номер {phone}")
    public MockOffer addPhoneNumber(String phone) {
        JsonObject phoneObject = new JsonObject();
        phoneObject.addProperty("phone", phone);
        offer.getAsJsonObject("author").getAsJsonArray("phoneNumbers").add(phoneObject);
        return this;
    }

    @Step("Добавляем номер {phone}")
    public MockOffer addRedirectPhones(boolean state) {
        offer.getAsJsonObject("author").addProperty("redirectPhones", state);
        return this;
    }

    @Step("Добавляем «Онлайн показ»")
    public MockOffer addRemoteView() {
        JsonObject remoteReview = new JsonObject();
        remoteReview.addProperty("onlineShow", true);
        offer.add("remoteReview", remoteReview);
        return this;
    }

    @Step("Добавляем ссылку на видео")
    public MockOffer addVideoLink(String videoLink) {
        JsonObject remoteReview = new JsonObject();
        remoteReview.addProperty("youtubeVideoReviewUrl", videoLink);
        offer.add("remoteReview", remoteReview);
        return this;
    }

    @Step("Добавляем predictedPriceAdvice LOW")
    public MockOffer addPredictedPriceAdviceLow() {
        JsonObject predictedPriceAdvice = new JsonObject();
        predictedPriceAdvice.addProperty("summary", "LOW");
        offer.getAsJsonObject("predictions").add("predictedPriceAdvice", predictedPriceAdvice);
        return this;
    }

    @Step("Добавляем «freeReportAccessibility» = FRA_READY")
    public MockOffer addFreeReportAccessibility() {
        offer.addProperty("freeReportAccessibility", "FRA_READY");
        return this;
    }

    @Step("Добавляем «withExcerpt» = true")
    public MockOffer addYandexRen() {
        offer.addProperty("yandexRent", true);
        return this;
    }

    @Step("Получаем id оффера")
    public String getOfferId() {
        return offer.getAsJsonPrimitive("offerId").getAsString();
    }

    @Step("Получаем список телефонов")
    public List<String> getPhoneList() {
        List<String> phoneList = newArrayList();
        offer.getAsJsonObject("author").getAsJsonArray("phoneNumbers").forEach(
                e -> phoneList.add(e.getAsJsonObject().getAsJsonPrimitive("phone").getAsString()));
        return phoneList;
    }

    @Step("Получаем адрес оффера")
    public String getOfferAddress() {
        return offer.getAsJsonObject("location").getAsJsonPrimitive("geocoderAddress").getAsString();
    }

    @Step("Добавляем inexactMatch.price")
    public MockOffer setInexactMatchPrice(String price) {
        JsonArray priceArray = new JsonArray();
        priceArray.add(price);
        JsonObject inexactMatch = new JsonObject();
        inexactMatch.add("price", priceArray);
        offer.add("inexactMatch", inexactMatch);
        return this;
    }

    @Step("Добавляем price.value")
    public MockOffer setPrice(int price) {
        offer.getAsJsonObject("price").addProperty("value", price);
        return this;
    }

    @Step("Добавляем offerId = «{offerId}»")
    public MockOffer setOfferId(String offerId) {
        offer.addProperty("offerId", offerId);
        return this;
    }

    @Step("Очищаем фото")
    public MockOffer clearPhotos() {
        offer.remove("totalImages");
        offer.remove("minicardImages");
        offer.remove("mainImages");
        offer.remove("fullImages");
        offer.remove("alikeImages");
        offer.remove("cosmicImages");
        offer.remove("appMiddleImages");
        offer.remove("appLargeImages");
        offer.remove("appMiniSnippetImages");
        offer.remove("appSmallSnippetImages");
        offer.remove("appMiddleSnippetImages");
        offer.remove("appLargeSnippetImages");
        offer.remove("extImages");
        return this;
    }

    @Step("Добавляем id = «{id}» застройщика")
    public MockOffer setDeveloperId(String id) {
        offer.getAsJsonObject(SITE_INFO).getAsJsonArray(DEVELOPERS).get(0).getAsJsonObject().addProperty("id", id);
        return this;
    }

    @Step("Добавляем name = «{name}» застройщика")
    public MockOffer setDeveloperName(String name) {
        offer.getAsJsonObject(SITE_INFO).getAsJsonArray(DEVELOPERS).get(0).getAsJsonObject().addProperty("name", name);
        return this;
    }

    @Step("Получаем урл фото с планом")
    public String getPlanPhotoUrl() {
        return offer.getAsJsonObject("extImages").getAsJsonObject("IMAGE_PLAN").getAsJsonArray("origImages").get(0)
                .getAsString();
    }

}
