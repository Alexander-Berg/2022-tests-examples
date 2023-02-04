package ru.yandex.general.beans.ajaxRequests.updateDraft;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class GeoPoint {

    double latitude;
    double longitude;

    public static GeoPoint geoPoint() {
        return new GeoPoint();
    }

}
