package ru.auto.tests.desktop.mock.beans.offer.auction;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Segment {

    Integer percent;
    @SerializedName("min_bid")
    Integer minBid;
    @SerializedName("max_bid")
    Integer maxBid;
    Boolean current;

    public static Segment segment(Integer percent) {
        return new Segment().setPercent(percent);
    }

}
