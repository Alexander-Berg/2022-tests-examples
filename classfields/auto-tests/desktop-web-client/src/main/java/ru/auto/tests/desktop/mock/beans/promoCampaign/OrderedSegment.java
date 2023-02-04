package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class OrderedSegment {

    @SerializedName("offers_count")
    String offersCount;

    public static OrderedSegment orderedSegment() {
        return new OrderedSegment();
    }

}
