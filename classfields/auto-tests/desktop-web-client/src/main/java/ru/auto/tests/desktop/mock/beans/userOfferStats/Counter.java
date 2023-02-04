package ru.auto.tests.desktop.mock.beans.userOfferStats;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Counter {

    int views;
    @SerializedName("price_info")
    PriceInfo priceInfo;
    String date;
    @SerializedName("phone_calls")
    int phoneCalls;
    @SerializedName("phone_views")
    int phoneViews;
    @SerializedName("favorite_total")
    int favoriteTotal;
    int favorite;
    @SerializedName("favorite_remove")
    int favoriteRemove;

    public static Counter counter() {
        return new Counter();
    }

}
