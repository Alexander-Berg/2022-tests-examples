package ru.yandex.realty.beans.developer.office;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class DatePatterns {

    private int from;
    private int to;
    private List<TimeIntervals> timeIntervals;

}
