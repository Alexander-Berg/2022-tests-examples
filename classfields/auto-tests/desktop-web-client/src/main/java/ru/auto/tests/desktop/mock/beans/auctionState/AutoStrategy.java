package ru.auto.tests.desktop.mock.beans.auctionState;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class AutoStrategy {

    @SerializedName("max_bid")
    String maxBid;
    @SerializedName("max_position_for_price")
    JsonObject maxPositionForPrice;
    @SerializedName("auto_strategy_settings")
    AutoStrategySettings autoStrategySettings;

    public static AutoStrategy autoStrategy() {
        return new AutoStrategy();
    }

}
