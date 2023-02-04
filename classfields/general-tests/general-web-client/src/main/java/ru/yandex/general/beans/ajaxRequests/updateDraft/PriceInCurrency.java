package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class PriceInCurrency {

    String priceRur;

    public static PriceInCurrency priceInCurrency(String value) {
        return new PriceInCurrency().setPriceRur(value);
    }

}
