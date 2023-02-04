package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class BiddingAlgorithm {

    @SerializedName("max_position_for_price")
    MaxPositionForPrice maxPositionForPrice;

    public static BiddingAlgorithm biddingAlgorithm() {
        return new BiddingAlgorithm();
    }

}
