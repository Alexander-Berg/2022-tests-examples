package ru.yandex.realty.beans.developerSearchQuery;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeveloperSearchQueryStatistic {

    private int allHouses;
    private int allSites;
    private DeveloperSearchQueryStatisticAmount finished;
    private DeveloperSearchQueryStatisticAmount unfinished;
    private DeveloperSearchQueryStatisticAmount suspended;

}
