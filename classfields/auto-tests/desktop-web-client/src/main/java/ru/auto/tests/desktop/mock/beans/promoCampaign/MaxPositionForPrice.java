package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class MaxPositionForPrice {

    @SerializedName("max_bid")
    Integer maxBid;

    public static MaxPositionForPrice maxPositionForPrice() {
        return new MaxPositionForPrice();
    }

}
