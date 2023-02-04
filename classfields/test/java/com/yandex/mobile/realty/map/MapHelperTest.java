package com.yandex.mobile.realty.map;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mobile.realty.RobolectricTest;
import com.yandex.mobile.realty.domain.model.geo.GeoPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.yandex.mobile.realty.utils.GeoPointExtensionsKt.getBoundingBox;
import static com.yandex.mobile.realty.utils.BoundingBoxKt.toMapBoundingBox;
import static junit.framework.Assert.assertEquals;

/**
 * @author agapitov on 20.01.2016.
 */
@RunWith(RobolectricTestRunner.class)
public class MapHelperTest extends RobolectricTest {
    public static final double MIN_LAT = 59.906089;
    public static final double MAX_LAT = 59.959072;
    public static final double MIN_LON = 30.317413;
    public static final double MAX_LON = 30.40599;

    private final Point southWest = new Point(MIN_LAT, MIN_LON);
    private final Point northEast = new Point(MAX_LAT, MAX_LON);
    private final BoundingBox boundingBox = new BoundingBox(southWest, northEast);

    @Test
    public void shouldReturnMaxLat() {
        assertEquals(MAX_LAT, MapHelper.getMaxLat(boundingBox));
    }

    @Test
    public void shouldReturnMaxLon() {
        assertEquals(MAX_LON, MapHelper.getMaxLon(boundingBox));
    }

    @Test
    public void shouldReturnMinLat() {
        assertEquals(MIN_LAT, MapHelper.getMinLat(boundingBox));
    }

    @Test
    public void shouldReturnMinLon() {
        assertEquals(MIN_LON, MapHelper.getMinLon(boundingBox));
    }

    @Test
    public void shouldReturnCorrectBoundingBox() {
        List<GeoPoint> points = new ArrayList<>(2);
        points.add(new GeoPoint(MAX_LAT, MIN_LON));
        points.add(new GeoPoint(MIN_LAT, MAX_LON));

        BoundingBox actual = toMapBoundingBox(getBoundingBox(points));
        
        assertEquals(MapHelper.getMaxLat(actual), MAX_LAT);
        assertEquals(MapHelper.getMinLon(actual), MIN_LON);
        assertEquals(MapHelper.getMinLat(actual), MIN_LAT);
        assertEquals(MapHelper.getMaxLon(actual), MAX_LON);
    }
}
