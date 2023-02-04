package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Price {

    PriceInCurrency priceInCurrency;

    public static Price price() {
        return new Price();
    }

    public static Price price(long price) {
        return new Price().setPriceInCurrency(new PriceInCurrency().setPriceRur(price));
    }

}
