package ru.auto.tests.desktop.mock.beans.promoCampaign;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Period {

    String from;
    String to;

    public static Period period() {
        return new Period();
    }

}
