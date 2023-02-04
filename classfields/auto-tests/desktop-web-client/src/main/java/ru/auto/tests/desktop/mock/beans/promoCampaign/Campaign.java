package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Campaign {

    String id;
    String name;
    Period period;
    Filters filters;
    @SerializedName("bidding_algorithm")
    BiddingAlgorithm biddingAlgorithm;
    String status;
    @SerializedName("market_indicator")
    MarketIndicator marketIndicator;
    String description;
    @SerializedName("change_at")
    String changeAt;
    @SerializedName("days_on_stock")
    Period daysOnStock;
    @SerializedName("days_without_calls")
    Period daysWithoutCalls;
    Boolean isPristine;
    @SerializedName("max_offer_daily_calls")
    String maxOfferDailyCalls;
    Pagination pagination;
    @SerializedName("market_segment_filter")
    MarketSegmentFilter marketSegmentFilter;

    public static Campaign campaign() {
        return new Campaign();
    }

}
