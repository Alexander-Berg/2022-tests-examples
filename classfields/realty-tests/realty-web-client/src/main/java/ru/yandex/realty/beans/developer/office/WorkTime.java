package ru.yandex.realty.beans.developer.office;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class WorkTime {

    private List<DatePatterns> datePatterns;
    private String timeZone;

}
