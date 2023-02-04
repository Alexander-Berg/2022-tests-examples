package ru.yandex.realty.util.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.graph.util.GeometryUtil;
import ru.yandex.realty.model.geometry.BoundingBox;
import ru.yandex.realty.model.geometry.Polygon;

/**
 * @author: berkut
 */
public class PolygonUtilTest {

    private static final WKTReader WKT_READER = new WKTReader();


    @Test
    public void testConvertingGeometry() throws Exception {
        Geometry originalGeom = WKT_READER.read(STRING_GEOMETRY);
        ru.yandex.realty.model.geometry.Geometry geometry = Converter.convert(originalGeom);
        Assert.assertTrue(geometry != null);

        Geometry newGeom = GeometryUtil.toGeometry(geometry);
        Assert.assertNotNull(newGeom);

        Coordinate[] originalCoord = originalGeom.getCoordinates();
        Coordinate[] newCoord = newGeom.getCoordinates();

        Assert.assertEquals("Not equal amount of points in geometries", originalCoord.length, newCoord.length);

        for (int i = 0; i < newCoord.length; ++i) {
            Assert.assertEquals("X coord not equals", originalCoord[i].x, newCoord[i].x, 1f);
            Assert.assertEquals("Y coord not equals", originalCoord[i].y, newCoord[i].y, 1f);
            Assert.assertEquals("Z coord not equals", originalCoord[i].z, newCoord[i].z, 1f);
        }
    }

    @Test
    public void testJoin() throws Exception {
        Polygon a = new Polygon(new float[]{1, 1, 6, 6}, new float[]{2, 7, 7, 2});
        Polygon b = new Polygon(new float[]{2, 2, 3, 3}, new float[]{3, 4, 4, 3});
        Polygon c = new Polygon(new float[]{4, 4, 5, 5}, new float[]{5, 6, 6, 5});
        Polygon d = new Polygon(new float[]{-2, -2, -5}, new float[]{2, 5, 3});

        float lat0 = 4.5f;
        float lon0 = 5.5f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat0, lon0, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, b));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat0, lon0, c));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, d));

        float lat1 = 2.5f;
        float lon1 = 3.5f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat1, lon1, a));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat1, lon1, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, c));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, d));

        float lat2 = 1.5f;
        float lon2 = 2.5f;
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat2, lon2, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat2, lon2, c));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat2, lon2, d));

        float lat3 = -3f;
        float lon3 = 3f;
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat3, lon3, a));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat3, lon3, b));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat3, lon3, c));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat3, lon3, d));

        Polygon ab = PolygonUtil.join(a, b);
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat0, lon0, ab));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, ab));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, ab));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat3, lon3, ab));

        Polygon abc = PolygonUtil.join(ab, c);
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, abc));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, abc));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, abc));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat3, lon3, abc));

        Polygon abcd = PolygonUtil.join(abc, d);
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat0, lon0, abcd));
        Assert.assertFalse(PolygonUtil.pointInPolygon(lat1, lon1, abcd));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat2, lon2, abcd));
        Assert.assertTrue(PolygonUtil.pointInPolygon(lat3, lon3, abcd));
    }

    @Test
    public void testBoxIntersection() throws Exception {
        BoundingBox box1 = new BoundingBox(0, -5, 5, 5);
        BoundingBox box2 = new BoundingBox(-30, -20, -10, -6);
        Assert.assertFalse(BoundingBoxUtil.intersects(box1, box2));

        box2 = new BoundingBox(3, -10, 10, -3);
        Assert.assertTrue(BoundingBoxUtil.intersects(box1, box2));

        box2 = BoundingBoxUtil.fixCrossMeridian(new BoundingBox(0, -170, 5, 170));
        Assert.assertFalse(BoundingBoxUtil.intersects(box2, box1));
        Assert.assertFalse(BoundingBoxUtil.intersects(box1, box2));

        box1 = BoundingBoxUtil.fixCrossMeridian(new BoundingBox(6, -170, 10, 170));
        Assert.assertFalse(BoundingBoxUtil.intersects(box1, box2));
        Assert.assertFalse(BoundingBoxUtil.intersects(box2, box1));

        box2 = BoundingBoxUtil.fixCrossMeridian(new BoundingBox(0, -169, 7, 160));
        Assert.assertTrue(BoundingBoxUtil.intersects(box1, box2));
        Assert.assertTrue(BoundingBoxUtil.intersects(box2, box1));

        box2 = new BoundingBox(6, -169, 7, -100);
        Assert.assertFalse(BoundingBoxUtil.intersects(box1, box2));
        Assert.assertFalse(BoundingBoxUtil.intersects(box2, box1));

    }

    String STRING_GEOMETRY = "POLYGON ((26.6826800 50.3893080, 26.6901930 50.3895050, 26.6815550 50.3658760, 26.6805520 50.3621720, 26.6760080 50.3525470, 26.6810680 50.3489000, 26.6778370 50.3472560, 26.6784010 50.3393900, 26.6823460 50.3381970, 26.6938150 50.3266490, \n" +
            "    26.6897950 50.3239310, 26.6717630 50.3244090, 26.6692300 50.3206900, 26.6657300 50.3209530, 26.6628400 50.3186720, 26.6635690 50.3179050, 26.6638130 50.3172870, 26.6626130 50.3166690, 26.6633400 50.3160550, 26.6647920 50.3149790, \n" +
            "    26.6657550 50.3149830, 26.6662320 50.3157530, 26.6671890 50.3165280, 26.6679100 50.3166840, 26.6691160 50.3165320, 26.6698420 50.3159170, 26.6693640 50.3156080, 26.6684020 50.3152990, 26.6676830 50.3146780, 26.6686500 50.3140630, \n" +
            "    26.6700940 50.3142240, 26.6691500 50.3098670, 26.6665790 50.3092260, 26.6682190 50.3065140, 26.6718370 50.3055990, 26.6725720 50.3037520, 26.6654860 50.3040160, 26.6656790 50.2962910, 26.6658020 50.2931510, 26.6534170 50.2892150, \n" +
            "    26.6510100 50.2893630, 26.6365360 50.2891640, 26.6359380 50.2776050, 26.6379110 50.2584440, 26.6310250 50.2591000, 26.6295770 50.2597110, 26.6274030 50.2612440, 26.6254250 50.2626100, 26.6247420 50.2630870, 26.6220830 50.2649290, \n" +
            "    26.6187010 50.2667680, 26.6162830 50.2684540, 26.6133820 50.2704490, 26.6090290 50.2732070, 26.6063990 50.2749310, 26.6058570 50.2766020, 26.5963120 50.2857270, 26.5969870 50.2888780, 26.6082120 50.2889120, 26.6095510 50.2970110, \n" +
            "    26.5919570 50.3027940, 26.5855900 50.3086150, 26.5814190 50.3041020, 26.5758030 50.3040830, 26.5706780 50.3123730, 26.5630110 50.3213730, 26.5595130 50.3236460, 26.5579450 50.3248710, 26.5559290 50.3266750, 26.5526670 50.3297420, \n" +
            "    26.5555420 50.3305770, 26.5642750 50.3331520, 26.5684600 50.3342890, 26.5718720 50.3351280, 26.5762920 50.3365440, 26.5794460 50.3378330, 26.5820420 50.3392250, 26.5825570 50.3394850, 26.5827520 50.3395040, 26.5844450 50.3396330, \n" +
            "    26.5853760 50.3406680, 26.5863070 50.3418120, 26.5897100 50.3453170, 26.5897350 50.3458630, 26.5881970 50.3459430, 26.5891720 50.3477930, 26.6006880 50.3661570, 26.6011820 50.3670150, 26.6016350 50.3676370, 26.6018060 50.3679350, \n" +
            "    26.6022120 50.3681710, 26.6030920 50.3683890, 26.6220390 50.3715210, 26.6480040 50.3647610, 26.6485190 50.3657680, 26.6489040 50.3665610, 26.6516720 50.3726800, 26.6530450 50.3764340, 26.6541840 50.3790970, 26.6555570 50.3824650, \n" +
            "    26.6570580 50.3859170, 26.6581110 50.3884080, 26.6594410 50.3914750, 26.6599560 50.3922270, 26.6601710 50.3924630, 26.6633030 50.3919060, 26.6649340 50.3916240, 26.6689260 50.3910250, 26.6703200 50.3908310, 26.6721640 50.3908310, \n" +
            "    26.6747390 50.3906170, 26.6814570 50.3893960, 26.6826800 50.3893080))";
}
