package ru.yandex.general.beans.ajaxRequests.offerListingService;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Area {

    Coordinates coordinates;
    Toponyms toponyms;

    public static Area area() {
        return new Area();
    }

}
