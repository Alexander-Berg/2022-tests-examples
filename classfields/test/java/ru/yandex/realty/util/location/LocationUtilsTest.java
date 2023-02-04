package ru.yandex.realty.util.location;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.location.LocationAccuracy;
import ru.yandex.realty.model.raw.RawLocationExt;


public class LocationUtilsTest {

    @Test
    public void getCombinedAddress() {
        RawLocationExt rawLocation = new RawLocationExt();
        rawLocation.setCountry("Россия");
        rawLocation.setRegion("Ростовская область");
        rawLocation.setLocalityName("Ростов-на-Дону");
        rawLocation.setNonAdminSubLocality("ЖДР");
        rawLocation.setAddress("Краснодарская 2-я, д.72, кв.62");

        Location location = new Location();
        location.setAccuracy(LocationAccuracy.NUMBER);
        location.setGeocoderLocation("Россия, Ростов-на-Дону, 2-я Краснодарская улица, 72", GeoPoint.getPoint(47.204956f, 39.637875f));
        location.setLocalityName("Ростов-на-Дону");
        location.setStreet("2-я Краснодарская улица");
        location.setHouseNum("72");

        assertEquals(
                "Россия, Ростов-на-Дону, Краснодарская 2-я, д.72",
                LocationUtils.getCombinedAddress(rawLocation, location, false)
        );

        rawLocation.setAddress("Краснодарская 2-я, д.72");

        assertEquals(
                "Россия, Ростов-на-Дону, Краснодарская 2-я, д.72",
                LocationUtils.getCombinedAddress(rawLocation, location, false)
        );
    }
}