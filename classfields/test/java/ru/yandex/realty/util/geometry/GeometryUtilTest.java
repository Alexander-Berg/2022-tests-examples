package ru.yandex.realty.util.geometry;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.model.geometry.HolePolygon;
import ru.yandex.realty.model.geometry.MultiPolygon;
import ru.yandex.realty.model.geometry.Polygon;

import static ru.yandex.realty.util.geometry.GeometryUtil.pointInGeometry;

/**
 * author: rmuzhikov
 */
public class GeometryUtilTest {
    @Test
    public void testHolePolygon() throws Exception {
        Polygon a = new Polygon(new float[]{1, 7, 7, 1}, new float[]{1, 1, 6, 6});
        Polygon b = new Polygon(new float[]{2, 4, 2}, new float[]{2, 2, 5});
        Polygon c = new Polygon(new float[]{4, 6, 6, 4}, new float[]{3, 3, 5, 5});

        float lat0 = 3f;
        float lon0 = 3f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat0, lon0, a));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat0, lon0, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, c));

        float lat1 = 3f;
        float lon1 = 4f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat1, lon1, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, c));

        float lat2 = 5f;
        float lon2 = 4f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat2, lon2, b));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, c));

        HolePolygon a_bc = new HolePolygon(a, b, c);
        Assert.assertFalse(pointInGeometry(lat0, lon0, a_bc));
        Assert.assertTrue(pointInGeometry(lat1, lon1, a_bc));
        Assert.assertFalse(pointInGeometry(lat2, lon2, a_bc));
    }

    @Test
    public void testMultiPolygon() throws Exception {
        Polygon a = new Polygon(new float[]{1, 4, 1}, new float[]{1, 1, 4});
        Polygon b = new Polygon(new float[]{3, 6, 6, 3}, new float[]{3, 3, 7, 7});
        Polygon c = new Polygon(new float[]{4, 5, 5, 4}, new float[]{4, 4, 6, 6});

        float lat0 = 2f;
        float lon0 = 4f;
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, c));

        float lat1 = 2f;
        float lon1 = 2f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat1, lon1, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, c));

        float lat2 = 3.5f;
        float lon2 = 5f;
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat2, lon2, a));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat2, lon2, c));

        float lat3 = 4.5f;
        float lon3 = 5f;
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat3, lon3, a));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat3, lon3, b));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat3, lon3, c));

        MultiPolygon abc = new MultiPolygon(new HolePolygon(a), new HolePolygon(b, c));
        Assert.assertFalse(pointInGeometry(lat0, lon0, abc));
        Assert.assertTrue(pointInGeometry(lat1, lon1, abc));
        Assert.assertTrue(pointInGeometry(lat2, lon2, abc));
        Assert.assertFalse(pointInGeometry(lat3, lon3, abc));
    }

}
