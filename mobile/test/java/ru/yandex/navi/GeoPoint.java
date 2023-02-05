package ru.yandex.navi;

import org.openqa.selenium.html5.Location;

public final class GeoPoint extends Location {
    public final String country;
    public final String name;

    public static final GeoPoint YANDEX = new GeoPoint("Яндекс", 55.733969, 37.587093);

    public GeoPoint(String name, double latitude, double longitude) {
        this(null, name, latitude, longitude);
    }

    public GeoPoint(String country, String name, double latitude, double longitude) {
        super(latitude, longitude, 0.0);
        this.country = country;
        this.name = name;
    }
}
