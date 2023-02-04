package ru.yandex.general.beans.ajaxRequests.updateDraft;

import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.yandex.general.utils.NullStringTypeAdapter;

import static ru.yandex.general.step.OfferAddSteps.NULL_STRING;

@Setter
@Getter
@Accessors(chain = true)
public class EmptyValue {

    String value;

    public static EmptyValue emptyValue() {
        return new EmptyValue().setValue("");
    }

}
