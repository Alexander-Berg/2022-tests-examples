package ru.yandex.general.beans.wizard;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Price {

    String offerPrice;
    String currency;

    public static Price price() {
        return new Price();
    }

}
