package ru.yandex.general.beans.ldJson;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class AggregateOffer {

    @SerializedName("@type")
    String type;
    int lowPrice;
    int highPrice;
    int offerCount;
    String priceCurrency;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static AggregateOffer aggregateOffer() {
        return new AggregateOffer().setType("AggregateOffer").setPriceCurrency("RUB");
    }

}
