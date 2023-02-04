package ru.yandex.general.beans.ajaxRequests.updateDraft;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.utils.NullStringTypeAdapter;

@Setter
@Getter
@Accessors(chain = true)
public class Price {

    boolean isFree;
    PriceInCurrency priceInCurrency;
    Salary salary;
    @JsonAdapter(NullStringTypeAdapter.class)
    String salaryReplace;

    public static Price price() {
        return new Price();
    }

}
