package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class BaseSalary {

    @SerializedName("@type")
    String type;
    String currency;
    Value value;

    public static BaseSalary baseSalary(Value value) {
        return new BaseSalary().setType("MonetaryAmount").setCurrency("RUB").setValue(value);
    }
}
