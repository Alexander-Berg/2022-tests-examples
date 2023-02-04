package ru.yandex.general.beans.ajaxRequests.offerListingService;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class GreaterThan {

    int value;
    boolean orEquals;

    public static GreaterThan greaterThan() {
        return new GreaterThan();
    }

}
