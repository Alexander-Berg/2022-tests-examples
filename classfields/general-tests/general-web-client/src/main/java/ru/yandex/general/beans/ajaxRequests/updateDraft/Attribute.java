package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Attribute {

    String id;
    BooleanValue booleanValue;
    DictionaryValue dictionaryValue;
    RepeatedDictionaryValue repeatedDictionaryValue;
    NumberValue numberValue;
    EmptyValue emptyValue;

    public static Attribute attribute() {
        return new Attribute();
    }

}
