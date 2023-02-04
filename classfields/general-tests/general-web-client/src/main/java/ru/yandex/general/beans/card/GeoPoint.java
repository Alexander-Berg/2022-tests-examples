package ru.yandex.general.beans.card;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class GeoPoint {

    String latitude;
    String longitude;

    public static GeoPoint geoPoint() {
        return new GeoPoint();
    }

}
