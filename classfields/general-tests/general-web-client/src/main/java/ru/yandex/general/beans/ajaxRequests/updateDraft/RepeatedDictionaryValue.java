package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Arrays.asList;

@Setter
@Getter
@Accessors(chain = true)
public class RepeatedDictionaryValue {

    List<String> keys;

    public static RepeatedDictionaryValue repeatedDictionaryValue(String... values) {
        return new RepeatedDictionaryValue().setKeys(asList(values));
    }

}
