package ru.yandex.realty.beans.developerSearchQuery;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.DEVELOPER;

@Getter
@Setter
@Accessors(chain = true)
public class DeveloperSearchQueryResponse {

    private String id;
    private String name;
    private String logo;
    private String born;
    private DeveloperSearchQueryStatistic statistic;

    public static DeveloperSearchQueryResponse developer() {
        return new DeveloperSearchQueryResponse();
    }

    public static DeveloperSearchQueryResponse developerByTemplate() {
        return new GsonBuilder().create().fromJson(
                getResourceAsString(DEVELOPER), DeveloperSearchQueryResponse.class);
    }

    public DeveloperSearchQueryResponse withFinishedCount(int count) {
        getStatistic().getFinished().setHouses(count);
        return this;
    }

    public DeveloperSearchQueryResponse withSuspendedCount(int count) {
        getStatistic().getSuspended().setHouses(count);
        return this;
    }

    public DeveloperSearchQueryResponse withUnfinishedCount(int count) {
        getStatistic().getUnfinished().setHouses(count);
        return this;
    }
}
