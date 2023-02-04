package ru.yandex.solomon.math;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Vladimir Gordiychuk
 */
public class CropTest {

    @Test
    public void crop() {
        AggrGraphDataArrayList orig =
            AggrGraphDataArrayList.of(
                AggrPoint.shortPoint(2000, 2),
                AggrPoint.shortPoint(3000, 3),
                AggrPoint.shortPoint(4000, 4),
                AggrPoint.shortPoint(5000, 5)
            );

        Assert.assertEquals(orig, Crop.crop(orig, Interval.millis(1000, 6000)));
        Assert.assertEquals(orig, Crop.crop(orig, Interval.millis(2000, 5000)));

        Assert.assertEquals(
            AggrGraphDataArrayList.of(
                AggrPoint.shortPoint(2000, 2),
                AggrPoint.shortPoint(3000, 3),
                AggrPoint.shortPoint(4000, 4)
            ),
            Crop.crop(orig, Interval.millis(2000, 4500)));
        Assert.assertEquals(
            AggrGraphDataArrayList.of(
                AggrPoint.shortPoint(2000, 2),
                AggrPoint.shortPoint(3000, 3),
                AggrPoint.shortPoint(4000, 4)
            ),
            Crop.crop(orig, Interval.millis(2000, 4000)));
        Assert.assertEquals(
            AggrGraphDataArrayList.of(
                AggrPoint.shortPoint(3000, 3),
                AggrPoint.shortPoint(4000, 4)),
            Crop.crop(orig, Interval.millis(2500, 4000)));
        Assert.assertEquals(
            AggrGraphDataArrayList.of(AggrPoint.shortPoint(3000, 3)),
            Crop.crop(orig, Interval.millis(3000, 3000)));

        Assert.assertEquals(
            AggrGraphDataArrayList.of(AggrPoint.shortPoint(3000, 3)),
            Crop.crop(orig,  Interval.millis(2500, 3500)));

        Assert.assertEquals(
            AggrGraphDataArrayList.empty(),
            Crop.crop(orig,  Interval.millis(6000, 7000)));

        Assert.assertEquals(
            AggrGraphDataArrayList.empty(),
            Crop.crop(orig,  Interval.millis(0, 1000)));

        Assert.assertEquals(
            AggrGraphDataArrayList.empty(),
            Crop.crop(AggrGraphDataArrayList.empty(), Interval.millis(0, 1000)));
    }
}
