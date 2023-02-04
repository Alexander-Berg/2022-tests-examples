package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class NumberValue {

    int value;

    public static NumberValue numberValue(int value) {
        return new NumberValue().setValue(value);
    }

}
