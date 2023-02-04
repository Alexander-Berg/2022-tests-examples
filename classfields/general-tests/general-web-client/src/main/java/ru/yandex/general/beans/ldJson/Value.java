package ru.yandex.general.beans.ldJson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Value {

    @SerializedName("@type")
    String type;
    String value;
    String unitText;

    public static Value value(String value) {
        return new Value().setType("QuantitativeValue").setUnitText("MONTH").setValue(value);
    }
}
