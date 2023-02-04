package ru.yandex.general.beans.card;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.general.beans.card.AttributeDictionaryValue.dictionaryValue;

@Setter
@Getter
@Accessors(chain = true)
public class Attribute {

    String id;
    String name;
    String description;
    String metric;
    AttributeValue value;

    public static Attribute attribute() {
        return new Attribute();
    }

    public Attribute withInputValue(String value) {
        this.value.setNumberValue(Double.parseDouble(value));
        return this;
    }

    public Attribute withBooleanValue(boolean value) {
        this.value.setBooleanValue(value);
        return this;
    }

    public Attribute withSelectValue(String value) {
        this.value.setDictionaryValue(dictionaryValue().setName(value));
        return this;
    }

    public Attribute withMultiselectValue(String... values) {
        List<AttributeDictionaryValue> dictionaryValues = new ArrayList<>();
        for (String value:values) {
            dictionaryValues.add(dictionaryValue().setName(value));
        }
        this.value.setRepeatedDictionaryValue(dictionaryValues);
        return this;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}