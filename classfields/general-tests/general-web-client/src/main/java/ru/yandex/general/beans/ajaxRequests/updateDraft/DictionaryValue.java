package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class DictionaryValue {

    String key;

    public static DictionaryValue dictionaryValue(String value) {
        return new DictionaryValue().setKey(value);
    }

}
