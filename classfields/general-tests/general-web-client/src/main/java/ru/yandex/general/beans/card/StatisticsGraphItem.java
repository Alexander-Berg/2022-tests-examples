package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class StatisticsGraphItem {

    String date;
    Statistics statistics;
    boolean isHighlighted;

    public static StatisticsGraphItem statisticsGraphItem() {
        return new StatisticsGraphItem();
    }

}
