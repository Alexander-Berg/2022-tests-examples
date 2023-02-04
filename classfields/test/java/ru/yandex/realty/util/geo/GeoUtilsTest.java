package ru.yandex.realty.util.geo;

import org.junit.Test;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.realty.model.geometry.BoundingBox;
import ru.yandex.realty.model.location.GeoPoint;

import static java.lang.Math.PI;
import static java.lang.Math.toRadians;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
public class GeoUtilsTest {

    @Test
    public void testGeoDistance() {
        GeoPoint spb = GeoPoint.getPoint(59.938531f, 30.313497f);
        GeoPoint vsevolozhsk = GeoPoint.getPoint(60.014657f, 30.651748f);
//        double spbLat = toRadians(59.938531);
//        double spbLong = toRadians(30.313497);
//        double vsevolozhskLat = toRadians(60.014657);
//        double vsevolozhskLong = toRadians(30.651748);

        double distance = GeoUtils.geoDistance(spb, vsevolozhsk);
        assertTrue(20000 < distance && distance < 21000);
    }

    @Test
    public void testFastGeoDistance() {
        GeoPoint spb = GeoPoint.getPoint(59.938531f, 30.313497f);
        GeoPoint vsevolozhsk = GeoPoint.getPoint(60.014657f, 30.651748f);
//        double spbLat = toRadians(59.938531);
//        double spbLong = toRadians(30.313497);
//        double vsevolozhskLat = toRadians(60.014657);
//        double vsevolozhskLong = toRadians(30.651748);

        double distance = GeoUtils.fastGeoDistance(spb, vsevolozhsk);
        assertTrue(20000 < distance && distance < 21000);
    }

//    @Test
//    public void testSpeedDistance() {
//        GeoPoint spb = GeoPoint.getPoint(59.938531f, 30.313497f);
//        GeoPoint vsevolozhsk = GeoPoint.getPoint(60.014657f, 30.651748f);
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 10000000; ++ i) {
//            double distance = GeoUtils.geoDistance(spb, vsevolozhsk);
//        }
//        System.out.println(System.currentTimeMillis() - start);
//
//        start = System.currentTimeMillis();
//        for (int i = 0; i < 10000000; ++ i) {
//            double distance = GeoUtils.fastGeoDistance(spb, vsevolozhsk);
//        }
//        System.out.println(System.currentTimeMillis() - start);
//
//    }
//
    @Test
    public void testTheSame() {
        double latitude = toRadians(60.0);
        double longitude = toRadians(30.0);

        GeoPoint point = GeoPoint.getPoint(60.0f, 30.0f);

        double distance = GeoUtils.geoDistance(point, point);
        assertEquals(0, distance, 1e-7);
    }

    @Test
    public void testTheSameFast() {
        double latitude = toRadians(60.0);
        double longitude = toRadians(30.0);

        GeoPoint point = GeoPoint.getPoint(60.0f, 30.0f);

        double distance = GeoUtils.fastGeoDistance(point, point);
        assertEquals(0, distance, 1e-7);
    }

    @Test
    public void testCalculatingSqare() {
        GeoPoint spb = GeoPoint.getPoint(59.938531f, 30.313497f);
        double distance = 1000 * 50; //50 km
        Pair<Double, Double> squareDiffs = GeoUtils.getFastLatitudeLongitudeForSquare(spb.getLatitude(), spb.getLongitude(), distance);
        double latDif = squareDiffs.first;
        double lonDif = squareDiffs.second;
        GeoPoint v1 = GeoPoint.getPoint((float) (spb.getLatitude() + latDif), (float) (spb.getLongitude() + lonDif));
        GeoPoint v2 = GeoPoint.getPoint((float) (spb.getLatitude() + latDif), (float) (spb.getLongitude() - lonDif));
        GeoPoint v3 = GeoPoint.getPoint((float)(spb.getLatitude() - latDif), (float) (spb.getLongitude() - lonDif));
        GeoPoint v4 = GeoPoint.getPoint((float) (spb.getLatitude() - latDif), (float) (spb.getLongitude() + lonDif));

        double distance12 = GeoUtils.geoDistance(v1, v2);
        double distance23 = GeoUtils.geoDistance(v2, v3);
        double distance34 = GeoUtils.geoDistance(v3, v4);
        double distance41 = GeoUtils.geoDistance(v4, v1);

        assertTrue(95000 < distance12 && distance12 < 105000);
        assertTrue(95000 < distance23 && distance23 < 105000);
        assertTrue(95000 < distance34 && distance34 < 105000);
        assertTrue(95000 < distance41 && distance41 < 105000);

        GeoPoint midPoint12 = GeoPoint.getPoint((v1.getLatitude() + v2.getLatitude()) / 2, (v1.getLongitude() + v2.getLongitude()) / 2);
        GeoPoint midPoint23 = GeoPoint.getPoint((v2.getLatitude() + v3.getLatitude()) / 2, (v2.getLongitude() + v3.getLongitude()) / 2);
        GeoPoint midPoint34 = GeoPoint.getPoint((v3.getLatitude() + v4.getLatitude()) / 2, (v3.getLongitude() + v4.getLongitude()) / 2);
        GeoPoint midPoint41 = GeoPoint.getPoint((v4.getLatitude() + v1.getLatitude()) / 2, (v4.getLongitude() + v1.getLongitude()) / 2);

        double distanceMid12 = GeoUtils.geoDistance(midPoint12, spb);
        double distanceMid23 = GeoUtils.geoDistance(midPoint23, spb);
        double distanceMid34 = GeoUtils.geoDistance(midPoint34, spb);
        double distanceMid41 = GeoUtils.geoDistance(midPoint41, spb);


        assertTrue(45000 < distanceMid12 && distanceMid12 < 55000);
        assertTrue(45000 < distanceMid23 && distanceMid23 < 55000);
        assertTrue(45000 < distanceMid34 && distanceMid34 < 55000);
        assertTrue(45000 < distanceMid41 && distanceMid41 < 55000);
    }

    @Test
    public void testExtendBoundingBox() {
        GeoPoint point1 = GeoPoint.getPoint(59.996609f, 30.377182f);
        GeoPoint point2 = GeoPoint.getPoint(60.010629f, 30.388696f);
        double distance = 500; //500 meters
        double distanceTotal = Math.sqrt(2*distance*distance);

        BoundingBox box1 = new BoundingBox(
                Math.min(point1.getLatitude(), point2.getLatitude()),
                Math.min(point1.getLongitude(), point2.getLongitude()),
                Math.max(point1.getLatitude(), point2.getLatitude()),
                Math.max(point1.getLongitude(), point2.getLongitude())
        );

        BoundingBox box2 = GeoUtils.extendFastBoundingBox(box1, distance);

        GeoPoint maxPoint1 = GeoPoint.getPoint(box1.getMaxLatitude(), box1.getMaxLongitude());
        GeoPoint minPoint1 = GeoPoint.getPoint(box1.getMinLatitude(), box1.getMinLongitude());

        GeoPoint maxPoint2 = GeoPoint.getPoint(box2.getMaxLatitude(), box2.getMaxLongitude());
        GeoPoint minPoint2 = GeoPoint.getPoint(box2.getMinLatitude(), box2.getMinLongitude());

        double distanceMax = GeoUtils.fastGeoDistance(maxPoint1, maxPoint2);
        double distanceMin = GeoUtils.fastGeoDistance(minPoint1, minPoint2);

        System.out.println("maxPoint2 = " + maxPoint2);
        System.out.println("minPoint2 = " + minPoint2);
        System.out.println("distanceTotal = " + distanceTotal);
        System.out.println("distanceMax = " + distanceMax);
        System.out.println("distanceMin = " + distanceMin);

        assertTrue(distanceTotal - 50 < distanceMax && distanceMax < distanceTotal + 50);
        assertTrue(distanceTotal - 50 < distanceMin && distanceMin < distanceTotal + 50);

    }


    @Test
    public void testMoveByDistance1() {
        GeoPoint point1 = GeoPoint.getPoint(59f, 30f);
        double distance = 50; //50 meters
        double tc = PI;
        GeoPoint moved = GeoUtils.moveByDistance(point1, distance, tc);
        System.out.println("move = " + moved + " dist=" + GeoUtils.geoDistance(point1, moved));
        assertTrue(GeoUtils.geoDistance(point1, moved) < distance + 1);
    }

    @Test
    public void testMoveByDistance2() {
        GeoPoint point1 = GeoPoint.getPoint(59f, 30f);
        double distance = 200; //200 meters
        double tc = 0.1;
        GeoPoint moved = GeoUtils.moveByDistance(point1, distance, tc);
        System.out.println("move = " + moved + " dist=" + GeoUtils.geoDistance(point1, moved));
        assertTrue(GeoUtils.geoDistance(point1, moved) < distance + 1);
    }
}
