package ru.yandex.realty.beans;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class Goal {

    @SerializedName("favorites")
    private Params favorites;

    @SerializedName("favorite")
    private Params favorite;

    @SerializedName("card")
    private Params card;

    @SerializedName("phone")
    private Params phone;

    @SerializedName("service")
    private String service;

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public class Params {
        @SerializedName("offer_type")
        private String offerType;

        @SerializedName("offer_category")
        private String offerCategory;

        @SerializedName("has_good_price")
        private Integer hasGoodPrice;

        @SerializedName("has_plan")
        private Integer hasPlan;

        @SerializedName("vas")
        private List<String> vas;

        @SerializedName("flat_type")
        private String flatType;

        @SerializedName("page")
        private String page;

        @SerializedName("placement")
        private String placement;

        @SerializedName("page_type")
        private String pageType;

        @SerializedName("source")
        private String source;

        @SerializedName("payed")
        private Integer payed;

        @SerializedName("has_egrn_report")
        private Integer hasEgrnReport;

        @SerializedName("has_video_review")
        private Integer hasVideoReview;

        @SerializedName("has_virtual_tour")
        private Integer hasVirtualTour;

        @SerializedName("offer_id")
        private String offerId;

        @SerializedName("online_show_available")
        private Integer onlineShowAvailable;

        @SerializedName("plan_type")
        private String planType;

        @SerializedName("primary_sale")
        private Boolean primarySale;

        @SerializedName("tuz")
        private Integer tuz;

        @SerializedName("pricing_period")
        private String pricingPeriod;

        @SerializedName("exact_match")
        private Integer exactMatch;
    }

    public static Goal goal() {
        return new Goal();
    }

    public static Params params() {
        return new Goal().new Params();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
