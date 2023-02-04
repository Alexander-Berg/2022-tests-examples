package ru.yandex.general.beans;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class GoalRequestBody {

    @SerializedName("offer_id")
    String offerId;

    @SerializedName("inactive_recall_reason")
    String inactiveRecallReason;

    @SerializedName("filter_id")
    String filterId;

    @SerializedName("category_id")
    String categoryId;

    @SerializedName("service_id")
    String serviceId;

    @SerializedName("event_place")
    String eventPlace;

    @SerializedName("region_id")
    String regionId;

    @SerializedName("offer_origin")
    String offerOrigin;

    String hasGeneralSearchResults;
    String block;
    String page;

    public static GoalRequestBody goalRequestBody() {
        return new GoalRequestBody();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
