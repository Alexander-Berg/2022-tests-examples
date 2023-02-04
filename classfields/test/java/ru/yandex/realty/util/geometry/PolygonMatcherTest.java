package ru.yandex.realty.util.geometry;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.model.geometry.Polygon;

/**
 * author: rmuzhikov
 */
public class PolygonMatcherTest {

    @Test
    public void testMatch() throws Exception {
        Polygon a = new Polygon(new float[]{1, 1, 6, 6}, new float[]{2, 7, 7, 2});
        Polygon b = new Polygon(new float[]{2, 2, 3, 3}, new float[]{3, 4, 4, 3});
        Polygon c = new Polygon(new float[]{4, 4, 5, 5}, new float[]{5, 6, 6, 5});
        Polygon d = new Polygon(new float[]{-2, -2, -5}, new float[]{2, 5, 3});

        float lat0 = 4.5f;
        float lon0 = 5.5f;
        Assert.assertTrue(new PolygonMatcher(a).matches(lat0, lon0));
        Assert.assertFalse(new PolygonMatcher(b).matches(lat0, lon0));
        Assert.assertTrue(new PolygonMatcher(c).matches(lat0, lon0));
        Assert.assertFalse(new PolygonMatcher(d).matches(lat0, lon0));

        float lat1 = 2.5f;
        float lon1 = 3.5f;
        Assert.assertTrue(new PolygonMatcher(a).matches(lat1, lon1));
        Assert.assertTrue(new PolygonMatcher(b).matches(lat1, lon1));
        Assert.assertFalse(new PolygonMatcher(c).matches(lat1, lon1));
        Assert.assertFalse(new PolygonMatcher(d).matches(lat1, lon1));

        float lat2 = 1.5f;
        float lon2 = 2.5f;
        Assert.assertTrue(new PolygonMatcher(a).matches(lat2, lon2));
        Assert.assertFalse(new PolygonMatcher(b).matches(lat2, lon2));
        Assert.assertFalse(new PolygonMatcher(c).matches(lat2, lon2));
        Assert.assertFalse(new PolygonMatcher(d).matches(lat2, lon2));

        float lat3 = -3f;
        float lon3 = 3f;
        Assert.assertFalse(new PolygonMatcher(a).matches(lat3, lon3));
        Assert.assertFalse(new PolygonMatcher(b).matches(lat3, lon3));
        Assert.assertFalse(new PolygonMatcher(c).matches(lat3, lon3));
        Assert.assertTrue(new PolygonMatcher(d).matches(lat3, lon3));

        Polygon ab = PolygonUtil.join(a, b);
        Assert.assertTrue(new PolygonMatcher(ab).matches(lat0, lon0));
        Assert.assertFalse(new PolygonMatcher(ab).matches(lat1, lon1));
        Assert.assertTrue(new PolygonMatcher(ab).matches(lat2, lon2));
        Assert.assertFalse(new PolygonMatcher(ab).matches(lat3, lon3));

        Polygon abc = PolygonUtil.join(ab, c);
        Assert.assertFalse(new PolygonMatcher(abc).matches(lat0, lon0));
        Assert.assertFalse(new PolygonMatcher(abc).matches(lat1, lon1));
        Assert.assertTrue(new PolygonMatcher(abc).matches(lat2, lon2));
        Assert.assertFalse(new PolygonMatcher(abc).matches(lat3, lon3));

        Polygon abcd = PolygonUtil.join(abc, d);
        Assert.assertFalse(new PolygonMatcher(abcd).matches(lat0, lon0));
        Assert.assertFalse(new PolygonMatcher(abcd).matches(lat1, lon1));
        Assert.assertTrue(new PolygonMatcher(abcd).matches(lat2, lon2));
        Assert.assertTrue(new PolygonMatcher(abcd).matches(lat3, lon3));
    }

}
