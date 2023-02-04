package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.BiddingAlgorithm.biddingAlgorithm;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Campaign.campaign;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Filters.filters;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MarketIndicator.marketIndicator;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.MaxPositionForPrice.maxPositionForPrice;
import static ru.auto.tests.desktop.mock.beans.promoCampaign.Period.period;
import static ru.auto.tests.desktop.utils.Utils.getISO8601Date;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.utils.Utils.getRandomVin;

public class MockPromoCampaigns {

    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockPromoCampaigns() {
        this.body = new JsonObject();
    }

    private MockPromoCampaigns(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockPromoCampaigns mockPromoCampaigns(String pathToTemplate) {
        return new MockPromoCampaigns(pathToTemplate);
    }

    public static MockPromoCampaigns mockPromoCampaigns(Campaign... campaigns) {
        return new MockPromoCampaigns().setPromoCampaigns(campaigns);
    }

    public MockPromoCampaigns setPromoCampaigns(Campaign... campaigns) {
        JsonArray promoCampaign = new JsonArray();
        stream(campaigns).forEach(campaign -> promoCampaign.add(getJsonObject(campaign)));

        body.add("promo_campaign", promoCampaign);
        return this;
    }

    public static Campaign getBaseCampaign() {
        return campaign()
                .setId(String.valueOf(getRandomShortInt()))
                .setPeriod(
                        period().setFrom(getISO8601Date(-getRandomBetween(2, 10)))
                                .setTo(getISO8601Date(getRandomBetween(5, 10))))
                .setBiddingAlgorithm(
                        biddingAlgorithm().setMaxPositionForPrice(
                                maxPositionForPrice().setMaxBid(100)))
                .setStatus(ACTIVE)
                .setMarketIndicator(marketIndicator())
                .setDescription("")
                .setDaysOnStock(
                        period().setFrom("0")
                                .setTo("0"))
                .setDaysWithoutCalls(
                        period().setFrom("0")
                                .setTo("0"))
                .setChangeAt(getISO8601Date(-1));
    }

    public static Campaign getFilledCampaign() {
        return getBaseCampaign()
                .setChangeAt(getISO8601Date(-1))
                .setDaysWithoutCalls(
                        period().setFrom(String.valueOf(getRandomBetween(1, 50)))
                                .setTo(String.valueOf(getRandomBetween(1, 50))))
                .setDaysOnStock(
                        period().setFrom(String.valueOf(getRandomBetween(1, 50)))
                                .setTo(String.valueOf(getRandomBetween(1, 50))))
                .setFilters(
                        filters().setPriceFrom(String.valueOf(getRandomBetween(1, 1000)))
                                .setPriceTo(String.valueOf(getRandomBetween(5000, 500000)))
                                .setYearFrom(String.valueOf(getRandomBetween(2010, 2015)))
                                .setYearTo(String.valueOf(getRandomBetween(2016, 2021)))
                                .setVinCodes(asList(getRandomVin()))
                                .setInStock("IN_STOCK"))
                .setMaxOfferDailyCalls(String.valueOf(getRandomBetween(1, 50)))
                .setBiddingAlgorithm(biddingAlgorithm().setMaxPositionForPrice(
                        maxPositionForPrice().setMaxBid(getRandomBetween(1, 50) * 100)))
                .setIsPristine(false);
    }

}
