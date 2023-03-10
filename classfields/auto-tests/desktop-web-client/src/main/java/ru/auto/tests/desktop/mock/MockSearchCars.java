package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import ru.auto.tests.desktop.mock.beans.offer.auction.Segment;
import ru.auto.tests.desktop.mock.beans.stub.Query;

import java.util.List;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.beans.offer.auction.Segment.segment;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.getJsonArray;

public class MockSearchCars {

    private static final String SEARCH_OFFERS_EXAMPLE = "mocksConfigurable/search/SearchOffersCarsExample.json";
    public static final String SEARCH_OFFERS_TESLA_MODEL_3 = "mocksConfigurable/search/SearchOffersCarsTeslaModel3.json";
    public static final String SEARCH_PROMO_ELECTRO_CARS = "mocksConfigurable/search/SearchPromoElectroCars.json";
    public static final String SEARCH_PROFESSIONAL_SELLER_OFFER = "mocksConfigurable/search/SearchOffersWithProfessionalSeller.json";
    public static final String DEALER_AUCTION_CARS_OFFERS_EXAMPLE = "mocksConfigurable/cabinet/DealerAuctionCarsUsedListingOffersExample.json";

    private static final String OFFERS = "offers";
    private static final String CAR_INFO = "car_info";
    private static final String SELLER = "seller";
    private static final String ADDITIONAL_INFO = "additional_info";
    private static final String PROMO_CAMPAIGN_LISTING_FIELDS = "promo_campaign_listing_fields";

    @Getter
    @Setter
    private JsonObject body;

    private MockSearchCars(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockSearchCars searchOffers(String pathToTemplate) {
        return new MockSearchCars(pathToTemplate);
    }

    public static MockSearchCars searchOffersCarsExample() {
        return searchOffers(SEARCH_OFFERS_EXAMPLE);
    }

    @Step("?????????????????? status = ??{statusOffer}?? ?? ?????????????? ????????????")
    public MockSearchCars setStatusOffer(String statusOffer) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().addProperty("status", statusOffer);
        return this;
    }

    @Step("?????????????????? rur_price = ??{rurPrice}?? ?? ?????????????? ????????????")
    public MockSearchCars setRurPrice(int rurPrice) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject("price_info")
                .addProperty("rur_price", rurPrice);
        return this;
    }

    @Step("?????????????????? documents.year = ??{year}?? ?? ?????????????? ????????????")
    public MockSearchCars setYear(int year) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject("documents")
                .addProperty("year", year);
        return this;
    }

    @Step("?????????????????? mark_info.name = ??{markName}?? ?? ?????????????? ????????????")
    public MockSearchCars setMarkName(String markName) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(CAR_INFO).getAsJsonObject("mark_info")
                .addProperty("name", markName);
        return this;
    }

    @Step("?????????????????? model_info.name = ??{modelName}?? ?? ?????????????? ????????????")
    public MockSearchCars setModelName(String modelName) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(CAR_INFO).getAsJsonObject("model_info")
                .addProperty("name", modelName);
        return this;
    }

    @Step("?????????????????? tech_param.nameplate = ??{techParam}?? ?? ?????????????? ????????????")
    public MockSearchCars setTechparamNameplate(String techParam) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(CAR_INFO).getAsJsonObject("tech_param")
                .addProperty("nameplate", techParam);
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(CAR_INFO).getAsJsonObject("tech_param")
                .addProperty("human_name", techParam);
        return this;
    }

    @Step("?????????????????? super_gen.name = ??{superGen}?? ?? ?????????????? ????????????")
    public MockSearchCars setSuperGenName(String superGen) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(CAR_INFO).getAsJsonObject("super_gen")
                .addProperty("name", superGen);
        return this;
    }

    @Step("?????????????????? state.mileage = ??{mileage}?? ?? ?????????????? ????????????")
    public MockSearchCars setMileage(int mileage) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject("state")
                .addProperty("mileage", mileage);
        return this;
    }

    @Step("?????????????????? region_info.name = ??{regionName}?? ?? ?????????????? ????????????")
    public MockSearchCars setRegionName(String regionName) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(SELLER).getAsJsonObject("location")
                .getAsJsonObject("region_info").addProperty("name", regionName);
        return this;
    }

    @Step("?????????????????? id = ??{id}?? ?? ?????????????? ????????????")
    public MockSearchCars setId(String id) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().addProperty("id", id);
        return this;
    }

    @Step("?????????????????? total_offers_count = ??{totalOffersCount}??")
    public MockSearchCars setTotalOffersCount(int totalOffersCount) {
        body.getAsJsonObject("pagination").addProperty("total_offers_count", totalOffersCount);
        return this;
    }

    @Step("?????????????? search_parameters.catalog_filter")
    public MockSearchCars setEmptyCatalogFilterSearchParameters() {
        JsonArray catalogFilter = new JsonArray();
        body.getAsJsonObject("search_parameters").add("catalog_filter", catalogFilter);
        return this;
    }

    @Step("?????????????????? seller.name = ??{sellerName}?? ?? ?????????????? ????????????")
    public MockSearchCars setSellerName(String sellerName) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(SELLER).addProperty("name", sellerName);
        return this;
    }

    @Step("?????????????????? other_offers_show_info.encrypted_user_id = ??{userId}?? ?? ?????????????? ????????????")
    public MockSearchCars setEncryptedUserId(String userId) {
        JsonObject otherOffersShowInfo = new JsonObject();
        otherOffersShowInfo.addProperty("encrypted_user_id", userId);

        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(ADDITIONAL_INFO)
                .add("other_offers_show_info", otherOffersShowInfo);
        return this;
    }

    @Step("?????????????????? days_in_stock = ??{daysInStock}?? ?? ?????????????? ????????????")
    public MockSearchCars setDaysInStock(int daysInStock) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(ADDITIONAL_INFO)
                .addProperty("days_in_stock", daysInStock);
        return this;
    }

    @Step("?????????????????? days_without_calls = ??{daysWithoutCalls}?? ?? ?????????????? ????????????")
    public MockSearchCars setDaysWithoutCalls(int daysWithoutCalls) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(PROMO_CAMPAIGN_LISTING_FIELDS)
                .addProperty("days_without_calls", daysWithoutCalls);
        return this;
    }

    @Step("?????????????????? bid_forecast = ??{bidForecast}?? ?? ?????????????? ????????????")
    public MockSearchCars setBidForecast(int bidForecast) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(PROMO_CAMPAIGN_LISTING_FIELDS)
                .addProperty("bid_forecast", bidForecast);
        return this;
    }

    @Step("?????????????????? max_interest_bid_diff = ??{maxInterestBidDiff}?? ?? ?????????????? ????????????")
    public MockSearchCars setMaxInterestBidDiff(int maxInterestBidDiff) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(PROMO_CAMPAIGN_LISTING_FIELDS)
                .addProperty("max_interest_bid_diff", maxInterestBidDiff);
        return this;
    }

    @Step("?????????????????? exchange = ??{isExchange}?? ?? ?????????????? ????????????")
    public MockSearchCars setExchange(boolean isExchange) {
        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(ADDITIONAL_INFO)
                .addProperty("exchange", isExchange);
        return this;
    }

    @Step("?????????????????? ???????????????? ??{segmentIndex}?? percent = ??{segmentPercent}?? ?? current = true, ?? ?????????????? ????????????")
    public MockSearchCars setSegment(int segmentIndex, int segmentPercent) {
        List<Segment> segments = asList(
                segment(35),
                segment(25),
                segment(20),
                segment(15),
                segment(5)
        );

        segments.get(segmentIndex).setCurrent(true).setPercent(segmentPercent);

        body.getAsJsonArray(OFFERS).get(0).getAsJsonObject().getAsJsonObject(PROMO_CAMPAIGN_LISTING_FIELDS)
                .add("interest_segments_forecast", getJsonArray(segments));
        return this;
    }

    @Step("?????????????????? ???????????? ??????????, ?????????????????? ??????????????")
    public MockSearchCars leaveSingleOffer() {
        JsonArray offers = new JsonArray();
        offers.add(body.getAsJsonArray(OFFERS).get(0));

        body.add(OFFERS, offers);
        return this;
    }

    public static Query getSearchOffersQuery() {
        return query()
                .setCategory("cars")
                .setContext("listing")
                .setPage("1")
                .setRid("225")
                .setSort("fresh_relevance_1-desc");
    }

    public static Query getSearchPromoElectroQuery() {
        return query()
                .setCategory("cars")
                .setContext("listing")
                .setPage("1")
                .setRid("225")
                .setSort("promo_cars_order-desc")
                .setEngineGroup("ELECTRO")
                .setGroupBy("CONFIGURATION")
                .setInStock("ANY_STOCK")
                .setOfferGrouping("false")
                .setStateGroup("ALL");
    }

    public static Query getSearchOffersUsedQuery() {
        return query()
                .setCategory("cars")
                .setContext("listing")
                .setGeoRadius("200")
                .setOfferGrouping("false")
                .setPageSize("37")
                .setPage("1")
                .setRid("213")
                .setSort("fresh_relevance_1-desc")
                .setWithDelivery("BOTH")
                .setStateGroup("USED");
    }

}
