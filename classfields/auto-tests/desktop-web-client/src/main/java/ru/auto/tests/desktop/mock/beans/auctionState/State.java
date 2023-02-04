package ru.auto.tests.desktop.mock.beans.auctionState;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class State {

    Context context;
    @SerializedName("base_price")
    String basePrice;
    @SerializedName("one_step")
    String oneStep;
    @SerializedName("min_bid")
    String minBid;
    @SerializedName("current_bid")
    String currentBid;
    @SerializedName("competitive_bids")
    List<CompetitiveBid> competitiveBids;
    @SerializedName("auto_strategy")
    AutoStrategy autoStrategy;

    public static State state() {
        return new State();
    }

}
