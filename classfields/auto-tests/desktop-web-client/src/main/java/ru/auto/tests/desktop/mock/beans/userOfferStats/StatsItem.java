package ru.auto.tests.desktop.mock.beans.userOfferStats;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class StatsItem {

    @SerializedName("offer_id")
    String offerId;
    List<Counter> counters;

    public static StatsItem stats() {
        return new StatsItem();
    }

}
