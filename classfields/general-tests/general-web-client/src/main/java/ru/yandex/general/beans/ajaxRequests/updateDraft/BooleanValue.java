package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class BooleanValue {

    boolean value;

    public static BooleanValue booleanValue(boolean value) {
        return new BooleanValue().setValue(value);
    }

}
