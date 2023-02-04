package ru.auto.tests.desktop.mock.beans.stub;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Query {

    @SerializedName("page_size")
    String pageSize;
    String page;
    String domain;

    @SerializedName("with_person_profiles")
    String withPersonProfiles;
    @SerializedName("with_offers")
    String withOffers;

    String mark;
    String model;
    String rid;
    @SerializedName("body_type")
    String bodyType;
    @SerializedName("gear_type")
    String gearType;
    @SerializedName("super_gen_id")
    String superGenId;
    String transmission;
    String year;

    String category;
    String offerId;
    @SerializedName("offer_id")
    String underscoreOfferId;
    String product;
    @SerializedName("schedule_type")
    String scheduleType;
    String time;
    String timezone;
    String weekdays;

    @SerializedName("mark_id")
    String markId;
    @SerializedName("model_id")
    String modelId;
    @SerializedName("complectation_id")
    String complectationId;
    @SerializedName("configuration_id")
    String configurationId;
    @SerializedName("tech_param_id")
    String techParamId;

    @SerializedName("catalog_filter")
    String catalogFilter;
    String context;
    @SerializedName("engine_group")
    String engineGroup;
    @SerializedName("engine_type")
    String engineType;
    String sort;
    @SerializedName("in_stock")
    String inStock;
    @SerializedName("group_by")
    String groupBy;
    @SerializedName("offer_grouping")
    String offerGrouping;
    @SerializedName("state_group")
    String stateGroup;
    Boolean photo;
    @SerializedName("geo_radius")
    String geoRadius;
    @SerializedName("geo_id")
    String geoId;
    @SerializedName("with_delivery")
    String withDelivery;

    String fingerprint;
    Boolean postTradeIn;

    Integer dayLimit;
    Boolean enabled;
    Boolean recalculateCostPerCall;

    String status;
    String section;
    @SerializedName("with_daily_counters")
    String withDailyCounters;
    @SerializedName("daily_counters_days")
    String dailyCountersDays;

    @SerializedName("user_id")
    String userId;
    @SerializedName("content_amount")
    String contentAmount;
    @SerializedName("content_id")
    String contentId;
    String source;

    @SerializedName("vin_or_license_plate")
    String vinOrLicensePlate;
    @SerializedName("decrement_quota")
    Boolean decrementQuota;

    String competitor;

    public static Query query() {
        return new Query();
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
