package ru.yandex.realty.searcher.serp;

import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.location.Location;
import ru.yandex.realty.model.location.LocationAccuracy;
import ru.yandex.realty.model.location.LocationType;
import ru.yandex.realty.model.offer.Offer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;
import static ru.yandex.common.util.collections.CollectionFactory.newArrayList;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
public class PointScattererV15Test {

    @Test
    public void testZeroPoints() {
        List<Offer> offers = Collections.emptyList();
        Map<Long, GeoPoint> mapping = PointScattererV15.scatterEqualPoints(offers);
        Assert.assertEquals(0, mapping.size());
    }

    @Test
    public void testThreePoints() {
        float latitude = 59f;
        float longitude = 30f;
        List<Offer> offers = newArrayList();
        for (int i = 1; i <= 3; i++) {
            offers.add(getOffer(i, getLocation(latitude, longitude)));
        }

        Map<Long, GeoPoint> mapping = PointScattererV15.scatterEqualPoints(offers);
        boolean upperPoint = false;
        boolean lowerRight = false;
        boolean lowerLeft = false;
        for (long i = 1; i <= 3; i++) {
            GeoPoint point = mapping.get(i);
            if (point.getLatitude() > latitude && equalDegrees(point.getLongitude(),longitude)) upperPoint = true;
            if (point.getLatitude() < latitude && point.getLongitude() > longitude) lowerRight = true;
            if (point.getLatitude() < latitude && point.getLongitude() < longitude) lowerLeft = true;
        }
        Assert.assertTrue(upperPoint && lowerRight && lowerLeft);
    }

    // helper methods
    private static Offer getOffer(int id, Location location) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setLocation(location);
        return offer;
    }

    private static Location getLocation(float latitude, float longitude) {
        Location location = new Location();
        location.setAccuracy(LocationAccuracy.EXACT);
        location.setType(LocationType.EXACT_ADDRESS);
        location.setGeocoderLocation("geocoder address", GeoPoint.getPoint(latitude, longitude));
        return location;
    }

    private static boolean equalDegrees(float degree1, float degree2) {
        return abs(degree1 - degree2) < 1E-5;
    }
}
