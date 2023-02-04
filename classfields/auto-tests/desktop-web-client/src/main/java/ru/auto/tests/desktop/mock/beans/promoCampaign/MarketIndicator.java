package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static ru.auto.tests.desktop.mock.beans.promoCampaign.OrderedSegment.orderedSegment;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;

@Getter
@Accessors(chain = true)
public class MarketIndicator {

    @SerializedName("ordered_segments")
    List<OrderedSegment> orderedSegments;

    private MarketIndicator setBaseOrderedSegments() {
        orderedSegments = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            orderedSegments.add(orderedSegment().setOffersCount("0"));
        }

        return this;
    }

    public MarketIndicator setRandomOrderedSegments() {
        orderedSegments = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            orderedSegments.add(orderedSegment().setOffersCount(String.valueOf(getRandomBetween(1, 50))));
        }

        return this;
    }

    public static MarketIndicator marketIndicator() {
        return new MarketIndicator().setBaseOrderedSegments();
    }

}
