package ru.yandex.solomon.model.type.ugram;

import org.junit.Test;

import ru.yandex.solomon.model.type.Histogram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.bucket;
import static ru.yandex.solomon.model.type.ugram.UgramHelper.ugram;

/**
 * @author Vladimir Gordiychuk
 */
public class UgramTest {

    private static double[] bounds(double... bounds) {
        return bounds;
    }

    private static long[] buckets(long... buckets) {
        return buckets;
    }

    @Test
    public void ugramToSolomonHistogram() {
        {
            var ugram = ugram(bucket(0, 5, 42));
            assertEquals(dhistogram(bounds(5), buckets(42)), ugram);
        }
        {
            var ugram = ugram(
                bucket(0, 5, 42),
                bucket(5, 10, 4),
                bucket(10, 15, 0));
            assertEquals(dhistogram(bounds(5, 10, 15), buckets(42, 4, 0)), ugram);
        }
        {
            var ugram = ugram(bucket(1, 5, 2));
            assertEquals(dhistogram(bounds(1, 5), buckets(0, 2)), ugram);
        }
        {
            var ugram = ugram(
                bucket(10, 15, 42),
                bucket(30, 40, 4),
                bucket(40, 45, 2)
            );
            assertEquals(dhistogram(bounds(10, 15, 30, 40, 45), buckets(0, 42, 0, 4, 2)), ugram);
        }
    }

    @Test
    public void compatibility_Accumulate1() {
        assertMerge(
            ugram(
                bucket(10, 20, 2),
                bucket(20, 50, 6),
                bucket(50, 60, 1)),
            ugram(
                bucket(5, 10, 1),
                bucket(10, 15, 3),
                bucket(15, 30, 3),
                bucket(30, 60, 3)),
            ugram(
                bucket(5, 10, 0.0 + 1.0),
                bucket(10, 15, 1.0 + 3.0),
                bucket(15, 20, 1.0 + 1.0),
                bucket(20, 30, 2.0 + 2.0),
                bucket(30, 50, 4.0 + 2.0),
                bucket(50, 60, 1.0 + 1.0))
        );
    }

    @Test
    public void compatibility_Accumulate2() {
        // Buckets nesting
        assertMerge(
            ugram(
                bucket(10.0, 20.0, 2.0),
                bucket(20.0, 30.0, 6.0),
                bucket(30.0, 50.0, 12.0),
                bucket(50.0, 50.0, 1.0)),
            ugram(
                bucket(10.0, 30.0, 8.0),
                bucket(30.0, 40.0, 3.0),
                bucket(40.0, 45.0, 5.0),
                bucket(45.0, 50.0, 10.0)
            ),
            ugram(
                bucket(10.0, 20.0, 2.0 + 8.0 / 2.0),
                bucket(20.0, 30.0, 6.0 + 8.0 / 2.0),
                bucket(30.0, 40.0, 12.0 / 2.0 + 3.0),
                bucket(40.0, 45.0, 12.0 / 4.0 + 5.0),
                bucket(45.0, 50.0, 12.0 / 4.0 + 10.0),
                bucket(50.0, 50.0, 1.0 + 0.0)
            )
        );
    }

    @Test
    public void compatibility_Accumulate3() {
        // Nonintersecting borders
        assertMerge(
            ugram(
                bucket(10.0, 20.0, 2.0),
                bucket(20.0, 50.0, 6.0),
                bucket(50.0, 50.0, 1.0)),
            ugram(
                bucket(5.0, 10.0, 1.0),
                bucket(10.0, 15.0, 3.0),
                bucket(15.0, 30.0, 3.0),
                bucket(30.0, 60.0, 3.0)),
            ugram(
                bucket(5.0, 10.0, 0.0 + 1.0),
                bucket(10.0, 15.0, 1.0 + 3.0),
                bucket(15.0, 20.0, 1.0 + 1.0),
                bucket(20.0, 30.0, 2.0 + 2.0),
                bucket(30.0, 50.0, 4.0 + 2.0),
                bucket(50.0, 50.0, 1.0 + 0.0),
                bucket(50.0, 60.0, 0.0 + 1.0))
        );
    }

    @Test
    public void compatibility_MulUgramWithSameBuckets() {
        assertMerge(
            ugram(
                bucket(1.0, 2.0, 1.0),
                bucket(3.0, 4.0, 3.0)
            ),
            ugram(
                bucket(3.0, 4.0, 2.0),
                bucket(5.0, 6.0, 1.0)
            ),
            ugram(
                bucket(1.0, 2.0, 1.0), // 1.0 + 0.0
                bucket(3.0, 4.0, 5.0), // 3.0 + 2.0
                bucket(5.0, 6.0, 1.0) // 0.0 + 1.0
            )
        );
    }

    @Test
    public void compatibility_IntersectedBucketsUgramMul() {
        assertMerge(
            ugram(
                bucket(1.0, 3.0, 2.0),
                bucket(5.0, 7.0, 2.0)),
            ugram(
                bucket(2.0, 6.0, 4.0)),
            ugram(
                bucket(1.0, 2.0, 1.0), // 1.0 + 0.0
                bucket(2.0, 3.0, 2.0), // 1.0 + 1.0
                bucket(3.0, 5.0, 2.0), // 0.0 + 2.0
                bucket(5.0, 6.0, 2.0), // 1.0 + 1.0
                bucket(6.0, 7.0, 1.0) // 1.0 + 0.0
            ));
    }

    @Test
    public void sameBounds() {
        assertMerge(
            dhistogram(bounds(10, 15), buckets(0, 2)),
            dhistogram(bounds(10, 15), buckets(3, 5)),
            dhistogram(bounds(10, 15), buckets(3, 7)));
    }

    @Test
    public void sameBoundsLess() {
        assertMerge(
            dhistogram(bounds(10, 15, 20), buckets(0, 2, 1)),
            dhistogram(bounds(10, 15), buckets(3, 5)),
            dhistogram(bounds(10, 15, 20), buckets(3, 7, 1)));
    }

    @Test
    public void sameBoundsMore() {
        assertMerge(
            dhistogram(bounds(10, 15), buckets(3, 5)),
            dhistogram(bounds(10, 15, 20), buckets(0, 2, 1)),
            dhistogram(bounds(10, 15, 20), buckets(3, 7, 1)));
    }

    @Test
    public void growRight() {
        assertMerge(
            dhistogram(bounds(10, 15, 20), buckets(3, 5, 1)),
            dhistogram(bounds(25, 30, 35), buckets(0, 2, 4)),
            dhistogram(bounds(10, 15, 20, 25, 30, 35), buckets(3, 5, 1, 0, 2, 4)));
    }


    @Test
    public void growLeft() {
        assertMerge(
            ugram(
                bucket(25, 30, 2),
                bucket(30, 35, 4)),
            ugram(
                bucket(10, 15, 5),
                bucket(15, 20, 1)),
            ugram(
                bucket(10, 15, 5),
                bucket(15, 20, 1),
                bucket(25, 30, 2),
                bucket(30, 35, 4)
            ));
    }

    @Test
    public void splitGrow() {
        assertMerge(
            ugram(
                bucket(10, 90, 42)),
            ugram(
                bucket(10, 15, 5),
                bucket(15, 20, 1)),
            ugram(
                bucket(10, 15, 7.625),
                bucket(15, 20, 3.625),
                bucket(20, 90, 36.75)
            ));
    }

    @Test
    public void upperSameDiffLower() {
        assertMerge(
            ugram(
                bucket(30, 50, 20)),
            ugram(
                bucket(10, 50, 40)),
            ugram(
                bucket(10, 30, 20),
                bucket(30, 50, 40)
            ));
    }

    @Test
    public void splitBucket() {
        assertMerge(
            ugram(
                bucket(0, 100, 100),
                bucket(100, 200, 100),
                bucket(200, 300, 100)
            ),
            ugram(
                bucket(0, 50, 10),
                bucket(50, 100, 10),
                bucket(100, 150, 10),
                bucket(150, 200, 10)
            ),
            ugram(
                bucket(0, 50, 60),
                bucket(50, 100, 60),
                bucket(100, 150, 60),
                bucket(150, 200, 60),
                bucket(200, 300, 100)
            ));
    }

    @Test
    public void gapFromMidle() {
        assertMerge(
            ugram(
                bucket(0, 10, 10),
                bucket(10, 100, 100),
                bucket(100, 200, 100),
                bucket(200, 300, 100)
            ),
            ugram(
                bucket(0, 10, 10),
                bucket(50, 100, 10),
                bucket(150, 250, 10)
            ),
            ugram(
                bucket(0, 10, 20),
                bucket(10, 50, 44),
                bucket(50, 100, 65.5),
                bucket(100, 150, 50),
                bucket(150, 200, 55),
                bucket(200, 250, 55),
                bucket(250, 300, 50)
            ));
    }

    @Test
    public void diffLower() {
        assertMerge(
            ugram(
                bucket(10, 30, 20),
                bucket(41, 45, 20),
                bucket(50, 100, 20)
            ),
            ugram(
                bucket(5, 20, 20),
                bucket(25, 30, 20),
                bucket(30, 60, 20),
                bucket(60, 100, 20)
            ),
            ugram(
                bucket(5, 10, 6.6),
                bucket(10, 20, 23.3),
                bucket(20, 25, 5),
                bucket(25, 30, 25),
                bucket(30, 41, 7.3),
                bucket(41, 45, 22.6),
                bucket(45, 50, 3.3),
                bucket(50, 60, 10.6),
                bucket(60, 100, 36)
            ));
    }

    @Test
    public void lowerSameDiffUpper() {
        assertMerge(
            ugram(
                bucket(10, 30, 20)),
            ugram(
                bucket(10, 50, 40)),
            ugram(
                bucket(10, 30, 40),
                bucket(30, 50, 20)
            ));
    }

    @Test
    public void compatibilityMerge() {
        // 10 .. 15 & 10 .. 20 => 10 .. 15 & 15 .. 20
        assertMerge(
            dhistogram(bounds(10, 15), buckets(0, 2)),
            dhistogram(bounds(10, 20), buckets(0, 5)),
            dhistogram(bounds(10, 15, 20), buckets(0, 5, 3)));

        // 10 .. 20 & 10 .. 30 => 10 .. 20 & 20 .. 30
        assertMerge(
            dhistogram(bounds(10, 20), buckets(0, 2)),
            dhistogram(bounds(10, 30), buckets(0, 5)),
            dhistogram(bounds(10, 20, 30), buckets(0, 5, 3)));

        // 10 .. 20 & 15 .. 20 => 10 .. 15 & 15 .. 20
        assertMerge(
            dhistogram(bounds(10, 20), buckets(0, 2)),
            dhistogram(bounds(15, 20), buckets(0, 5)),
            dhistogram(bounds(10, 15, 20), buckets(0, 1, 6)));

        // 10 .. 20 & 15 .. 18 => 10 .. 15 & 15 .. 18 & 18 .. 20
        assertMerge(
            dhistogram(bounds(10, 20), buckets(0, 2)),
            dhistogram(bounds(15, 18), buckets(0, 5)),
            dhistogram(bounds(10, 15, 18, 20), buckets(0, 1, 6, 0)));

        // 10 .. 20 & 15 .. 25 => 10 .. 15 & 15 .. 20 & 20 .. 25
        assertMerge(
            dhistogram(bounds(10, 20), buckets(0, 2)),
            dhistogram(bounds(15, 25), buckets(0, 5)),
            dhistogram(bounds(10, 15, 20, 25), buckets(0, 1, 4, 3)));

        // 10 .. 10 & 10 .. 20 => 10 .. 10 & 10 .. 20
        assertMerge(
            dhistogram(bounds(10), buckets(2)),
            dhistogram(bounds(10, 20), buckets(0, 5)),
            dhistogram(bounds(10, 20), buckets(2, 5)));

        // 10 .. 20 & 15 .. 15 => 10 .. 15 & 15 .. 15 & 15 .. 20
        assertMerge(
            dhistogram(bounds(10, 20), buckets(1, 2)),
            dhistogram(bounds(15), buckets(5)),
            dhistogram(bounds(10, 15, 20), buckets(4, 3, 1)));
    }

    @Test
    public void reset() {
        var snapshot = ugram(
            bucket(0, 5, 42),
            bucket(5, 10, 4),
            bucket(10, 15, 0));

        var ugram = Ugram.create();
        ugram.merge(snapshot);

        assertEquals(snapshot, ugram.snapshot());
        ugram.reset();
        assertEquals(Histogram.newInstance(), ugram.snapshot());
        ugram.merge(snapshot);
        assertEquals(snapshot, ugram.snapshot());
    }

    @Test
    public void recycle() {
        var one = Ugram.create();
        var two = Ugram.create();
        assertNotSame(one, two);
        one.recycle();
        var three = Ugram.create();
        assertNotSame(two, three);
    }

    public void assertMerge(Histogram one, Histogram two, Histogram expected) {
        assertMergeInternal(one, two, expected);
        assertMergeInternal(two, one, expected);
    }

    private void assertMergeInternal(Histogram one, Histogram two, Histogram expected) {
        System.out.println("\nassert " + one + " + " + two + " => " + expected);
        var ugram = Ugram.create();
        var stepOne = ugram.snapshot();
        ugram.merge(one);
        var stepTwo = ugram.snapshot();
        System.out.println(stepOne + " + " + one + " = " + stepTwo);
        ugram.merge(two);
        var stepThree = ugram.snapshot();
        System.out.println(stepTwo + " + " + two + " = " + stepThree);
        assertEquals(one + " + " + two, expected, stepThree);
    }
}
