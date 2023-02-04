package ru.yandex.general.beans.ajaxRequests.offerListingService;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class Constraint {

    List<String> oneOf;
    GreaterThan greaterThan;
    Boolean equalTo;

    public static Constraint constraint() {
        return new Constraint();
    }

}
