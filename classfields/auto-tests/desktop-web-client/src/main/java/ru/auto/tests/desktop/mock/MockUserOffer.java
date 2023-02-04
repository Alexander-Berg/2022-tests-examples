package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.consts.SaleServices.VasProduct;
import ru.auto.tests.desktop.mock.beans.offer.Service;
import ru.auto.tests.desktop.mock.beans.offer.auction.Auction;
import ru.auto.tests.desktop.mock.beans.offer.auction.Segment;
import ru.auto.tests.desktop.mock.beans.offer.seller.Phones;
import ru.auto.tests.desktop.mock.beans.stub.Query;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Auction.auction;
import static ru.auto.tests.desktop.mock.beans.offer.auction.CurrentState.currentState;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Segment.segment;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;

public class MockUserOffer {

    public static final String USER_OFFER_CAR_EXAMPLE = "mocksConfigurable/user/UserOfferCar.json";
    public static final String USER_OFFER_MOTO_EXAMPLE = "mocksConfigurable/user/UserOfferMoto.json";
    public static final String USER_OFFER_TRUCK_EXAMPLE = "mocksConfigurable/user/UserOfferTruck.json";
    public static final String USER_OFFER_CAR_AFTER_PUBLISH_EXAMPLE =
            "mocksConfigurable/user/UserOfferCarAfterPublish.json";

    private static final String BASE_SERVICE_PRICES = "mocksConfigurable/user/ServicePrices.json";
    private static final String SERVICE_PRICES_FOR_AUCTION = "mocksConfigurable/user/ServicePricesForAuction.json";

    private static final String SERVICE_PRICES = "service_prices";
    private static final String ADDITIONAL_INFO = "additional_info";
    private static final String TOTAL_COUNT = "total_count";
    private static final String ACTIONS = "actions";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockUserOffer(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserOffer mockUserOffer(String pathToTemplate) {
        return new MockUserOffer(pathToTemplate);
    }

    public static MockUserOffer car() {
        return mockUserOffer(USER_OFFER_CAR_EXAMPLE);
    }

    public static MockUserOffer carAfterPublish() {
        return mockUserOffer(USER_OFFER_CAR_AFTER_PUBLISH_EXAMPLE);
    }

    public static MockUserOffer moto() {
        return mockUserOffer(USER_OFFER_MOTO_EXAMPLE);
    }

    public static MockUserOffer truck() {
        return mockUserOffer(USER_OFFER_TRUCK_EXAMPLE);
    }

    public MockUserOffer setServices(Service... serviceList) {
        JsonArray services = new JsonArray();
        stream(serviceList).forEach(service -> services.add(getJsonObject(service)));

        body.add("services", services);
        return this;
    }

    public MockUserOffer setPhones(Phones... phonesList) {
        JsonArray phones = new JsonArray();
        stream(phonesList).forEach(phone -> phones.add(getJsonObject(phone)));

        body.getAsJsonObject("seller").add("phones", phones);
        return this;
    }

    public static Service service(VasProduct serviceName) {
        return Service.service().setService(serviceName)
                .setActive(true)
                .setProlongable(false)
                .setCreateDate(getDateDaysFromNow(0))
                .setExpireDate(getDateDaysFromNow(3));
    }

    public static Auction getAuctionWithRandomValues() {
        final int basePrice = getRandomBetween(1, 100);
        final int step = basePrice * 50;
        final int minBid = step * 4;
        final int currentBid = minBid * 2;
        final int maxBid = currentBid * 2;

        return auction().setCurrentState(
                currentState()
                        .setBasePrice(basePrice)
                        .setCurrentBid(currentBid)
                        .setMinBid(minBid)
                        .setMaxBid(maxBid)
                        .setOneStep(step));
    }

    public static List<Segment> getSegmentsWithCurrent(int segmentNumber){
        List<Segment> segments = asList(
                segment(35),
                segment(25),
                segment(20),
                segment(15),
                segment(5));

        segments.get(segmentNumber).setCurrent(true);
        return segments;
    }

    public static String getDateDaysFromNow(int daysCount) {
        long date = Timestamp.valueOf(LocalDate.parse(LocalDate.now().toString()).plusDays(daysCount)
                .atStartOfDay()).getTime();
        return Long.toString(date);
    }

    public MockUserOffer addHighPriorityForServicePrice(VasProduct serviceName) {
        body.getAsJsonArray(SERVICE_PRICES).get(getServicePriceIndexByName(serviceName)).getAsJsonObject()
                .addProperty("recommendation_priority", 15);
        return this;
    }

    public MockUserOffer addProlongationAllowedForServicePrice(VasProduct serviceName) {
        body.getAsJsonArray(SERVICE_PRICES).get(getServicePriceIndexByName(serviceName)).getAsJsonObject()
                .addProperty("prolongation_allowed", true);
        return this;
    }

    public MockUserOffer addProlongationForcedNotTogglable(VasProduct serviceName) {
        body.getAsJsonArray(SERVICE_PRICES).get(getServicePriceIndexByName(serviceName)).getAsJsonObject()
                .addProperty("prolongation_forced_not_togglable", true);
        return this;
    }

    private int getServicePriceIndexByName(VasProduct serviceName) {
        Optional<Integer> servicePriceIndex = Optional.empty();
        JsonArray servicePrices = body.getAsJsonArray(SERVICE_PRICES);

        for (int i = 0; i < servicePrices.size(); i++) {
            JsonObject servicePrice = servicePrices.get(i).getAsJsonObject();

            if (servicePrice.get("service").getAsString().equals(serviceName.getValue())) {
                servicePriceIndex = Optional.of(i);
                break;
            }
        }

        if (!servicePriceIndex.isPresent()) {
            throw new RuntimeException(format("Не удалось найти service = «%s» в моке user offers", serviceName));
        }

        return servicePriceIndex.get();
    }

    public MockUserOffer setBaseServicePrices() {
        JsonArray servicePrices = new GsonBuilder().create().fromJson(
                getResourceAsString(BASE_SERVICE_PRICES), JsonArray.class);
        body.add(SERVICE_PRICES, servicePrices);
        return this;
    }

    public MockUserOffer setServicePricesForAuction() {
        JsonArray servicePrices = new GsonBuilder().create().fromJson(
                getResourceAsString(SERVICE_PRICES_FOR_AUCTION), JsonArray.class);
        body.add(SERVICE_PRICES, servicePrices);
        return this;
    }

    @Step("Добавляем id = «{id}»")
    public MockUserOffer setId(String id) {
        body.addProperty("id", id);
        return this;
    }

    public MockUserOffer setAuction(Auction auction) {
        body.add("auction", getJsonObject(auction));
        return this;
    }

    public MockUserOffer setStatus(String status) {
        body.addProperty("status", status);
        return this;
    }

    public MockUserOffer setOfferActionsActivate(boolean isActivate) {
        body.getAsJsonObject(ACTIONS).addProperty("activate", isActivate);
        return this;
    }

    public MockUserOffer setActualizeDate(String date) {
        body.getAsJsonObject(ADDITIONAL_INFO).addProperty("actualize_date", date);
        return this;
    }

    public MockUserOffer setExpireDate(String date) {
        body.getAsJsonObject(ADDITIONAL_INFO).addProperty("expire_date", date);
        return this;
    }

    @Step("Добавляем позицию в поиске = «{position}», всего офферов = «{totalCount}»")
    public MockUserOffer setRelevanceSearchPosition(int position, int totalCount) {
        JsonObject relevancePosition = new JsonObject();
        relevancePosition.addProperty("sort", "RELEVANCE");
        relevancePosition.addProperty("position", position);
        relevancePosition.addProperty(TOTAL_COUNT, totalCount);

        JsonArray positions = new JsonArray();
        positions.add(relevancePosition);

        JsonObject searchPosition = new JsonObject();
        searchPosition.addProperty(TOTAL_COUNT, totalCount);
        searchPosition.add("positions", positions);

        JsonArray searchPositions = new JsonArray();
        searchPositions.add(searchPosition);

        body.getAsJsonObject(ADDITIONAL_INFO).add("search_positions", searchPositions);
        return this;
    }

    public JsonObject getOffer() {
        JsonObject offer = new JsonObject();
        offer.add("offer", body);
        return offer;
    }

    public static Query getQuery() {
        return query().setPageSize("20")
                .setStatus("active")
                .setSort("cr_date-desc")
                .setWithDailyCounters("true")
                .setDailyCountersDays("30");
    }

}
