package ru.yandex.general.beans.ajaxRequests.offerListingService;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Coordinates {

    double latitude;
    double longitude;
    int radiusMeters;

    public static Coordinates coordinates() {
        return new Coordinates();
    }

}
