package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.monlib.metrics.histogram.Histograms;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointData;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.type.Histogram;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class AggrGraphDataArrayListHistogramTest {
    private static AggrPoint point(String time, double[] bounds, long[] buckets) {
        return AggrPoint.builder()
            .time(time)
            .histogram(histogram(bounds, buckets))
            .build();
    }

    private static Histogram histogram(double[] bounds, long[] buckets) {
        return Histogram.newInstance(bounds, buckets);
    }

    @Test
    public void setAndGet() {
        AggrPoint expectedPoint = point(
            "2018-03-10T09:00:00Z",
            new double[]{10, 20, 30, Histograms.INF_BOUND },
            new long[]{4, 2, 0, 0});

        AggrGraphDataArrayList list = AggrGraphDataArrayList.of(expectedPoint);
        AggrPoint result = list.getAnyPoint(0);

        assertThat(result, equalTo(expectedPoint));
        assertThat(list.getTsMillis(0), equalTo(expectedPoint.tsMillis));
        assertThat(list.getHistogram(0), equalTo(expectedPoint.histogram));
    }

    @Test
    public void listEqualWhenAllHistogramsEqual() {
        AggrGraphDataArrayList first = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T10:00:00Z", new double[]{10, 20}, new long[]{5, 9})
        );

        AggrGraphDataArrayList second = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T10:00:00Z", new double[]{10, 20}, new long[]{5, 9})
        );

        assertThat(first, equalTo(second));
    }

    @Test
    public void listDiffHistogramNotEqual() {
        AggrGraphDataArrayList first = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T10:00:00Z", new double[]{10, 20}, new long[]{5, 9})
        );

        AggrGraphDataArrayList second = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", new double[]{10, 20}, new long[]{34, 44}),
            point("2017-05-10T10:00:00Z", new double[]{10, 20}, new long[]{93, 44})
        );

        assertThat(first, not(equalTo(second)));
    }

    @Test
    public void mask() {
        AggrGraphDataArrayList list = AggrGraphDataArrayList.of(
            point("2017-05-10T09:00:00Z", new double[]{10, 20}, new long[]{4, 2})
        );

        int expectMask = StockpileColumn.HISTOGRAM.mask() | StockpileColumn.TS.mask();
        assertThat(list.columnSetMask(), equalTo(expectMask));
    }

    @Test
    public void sortAndMergeSkipMerge() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T10:00:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{3, 4}),
            point("2017-05-10T13:00:00Z", new double[]{10, 20}, new long[]{5, 6})
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{3, 4}),
            point("2017-05-10T10:00:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T13:00:00Z", new double[]{10, 20}, new long[]{5, 6})
        );

        assertThat(source, equalTo(expected));
    }


    @Test
    public void sortAndMerge() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T10:10:00Z", new double[]{10, 20}, new long[]{3, 4}),
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{5, 6}),
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{7, 8})
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{7, 8}), // latest win
            point("2017-05-10T10:10:00Z", new double[]{10, 20}, new long[]{3, 4})
        );

        assertThat(source, equalTo(expected));
    }

    @Test
    public void sortAndMergeToRightBounds() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{1, 2}),
            point("2017-05-10T10:10:00Z", new double[]{10, 20}, new long[]{3, 4}),
            point("2017-05-10T03:10:00Z", new double[]{10, 20}, new long[]{5, 6}),
            point("2017-05-10T03:10:00Z", new double[]{10, 15, 20}, new long[]{7, 9, 8})
        );

        AggrGraphDataArrayList sorted = source.toSortedMerged();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
            point("2017-05-10T03:10:00Z", new double[]{10, 15, 20}, new long[]{7, 9, 8}), // latest win
            point("2017-05-10T10:10:00Z", new double[]{10, 20}, new long[]{3, 4})
        );

        assertThat(sorted, equalTo(expected));
    }

    @Test
    public void toView() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T01:00:00Z", new double[]{10, 15, 20}, new long[]{1, 2, 3}),
            point("2017-05-10T02:00:00Z", new double[]{10, 15, 20}, new long[]{4, 5, 6}),
            point("2017-05-10T03:10:00Z", new double[]{10, 15, 20}, new long[]{7, 8, 9})
        );

        AggrGraphDataArrayListView view = source.view();
        assertThat(view.columnSetMask(), equalTo(source.columnSetMask()));
        for (int index = 0; index < source.length(); index++) {
            AggrPointData sPoint = new AggrPointData();
            source.getDataTo(index, sPoint);

            AggrPointData vPoint = new AggrPointData();
            view.getDataTo(index, vPoint);

            assertThat(vPoint, equalTo(sPoint));
        }
    }

    @Test
    public void toViewAndBack() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
            point("2017-05-10T01:00:00Z", new double[]{10, 15, 20}, new long[]{1, 2, 3}),
            point("2017-05-10T02:00:00Z", new double[]{10, 15, 20}, new long[]{4, 5, 6}),
            point("2017-05-10T03:10:00Z", new double[]{10, 15, 20}, new long[]{7, 8, 9})
        );

        AggrGraphDataArrayListView view = source.view();

        AggrGraphDataArrayList result = new AggrGraphDataArrayList(view.columnSetMask(), view.length());
        result.addAllFrom(view.iterator());
        assertThat(result, equalTo(source));
    }
}
