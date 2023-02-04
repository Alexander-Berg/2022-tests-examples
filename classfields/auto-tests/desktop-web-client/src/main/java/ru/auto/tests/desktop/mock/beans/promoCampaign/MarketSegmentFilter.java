package ru.auto.tests.desktop.mock.beans.promoCampaign;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

@Setter
@Getter
@Accessors(chain = true)
public class MarketSegmentFilter {

    @SerializedName("available_segments")
    List<String> availableSegments;

    public static MarketSegmentFilter marketSegmentFilter(String avaliableSegment) {
        return new MarketSegmentFilter().setAvailableSegments(asList(avaliableSegment));
    }

}
