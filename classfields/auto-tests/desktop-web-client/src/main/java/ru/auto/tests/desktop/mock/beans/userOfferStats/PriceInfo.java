package ru.auto.tests.desktop.mock.beans.userOfferStats;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PriceInfo {

    @SerializedName("create_timestamp")
    String createTimestamp;
    String currency;
    long dprice;
    long price;

    public static PriceInfo priceInfo() {
        return new PriceInfo();
    }

}
