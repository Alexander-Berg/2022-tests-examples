package ru.auto.tests.desktop.mock.beans.offer.auction;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CurrentState {

    @SerializedName("current_bid")
    Integer currentBid;
    @SerializedName("base_price")
    Integer basePrice;
    @SerializedName("min_bid")
    Integer minBid;
    @SerializedName("max_bid")
    Integer maxBid;
    @SerializedName("one_step")
    Integer oneStep;

    public static CurrentState currentState() {
        return new CurrentState();
    }

}
