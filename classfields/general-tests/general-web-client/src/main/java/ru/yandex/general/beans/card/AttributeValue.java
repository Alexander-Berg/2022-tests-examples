package ru.yandex.general.beans.card;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class AttributeValue {

    @SerializedName("__typename")
    String typename;
    boolean booleanValue;
    AttributeDictionaryValue dictionaryValue;
    List<AttributeDictionaryValue> repeatedDictionaryValue;
    double numberValue;

    public static AttributeValue value() {
        return new AttributeValue();
    }

}
