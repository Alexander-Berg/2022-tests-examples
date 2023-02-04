package ru.yandex.general.beans.feed;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class FeedStatistics {

    int totalOfferCount;
    int activeOfferCount;
    int errorCount;
    int criticalErrorCount;

    public static FeedStatistics feedStatistics() {
        return new FeedStatistics();
    }

}
