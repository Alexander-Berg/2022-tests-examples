package ru.yandex.general.beans.ajaxRequests.offerListingService;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Parameter {

    String key;
    Constraint constraint;

    public static Parameter parameter() {
        return new Parameter();
    }

}
