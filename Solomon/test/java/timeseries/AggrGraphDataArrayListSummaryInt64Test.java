package ru.yandex.solomon.model.timeseries;

import org.junit.Test;

import ru.yandex.monlib.metrics.summary.ImmutableSummaryInt64Snapshot;
import ru.yandex.monlib.metrics.summary.SummaryInt64Snapshot;
import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointData;
import ru.yandex.solomon.model.point.column.StockpileColumn;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class AggrGraphDataArrayListSummaryInt64Test {
    private static AggrPoint point(String time, SummaryInt64Snapshot summary) {
        return AggrPoint.builder()
                .time(time)
                .summary(summary)
                .build();
    }

    private static AggrPoint pointAggregate(String time, SummaryInt64Snapshot summary) {
        return AggrPoint.builder()
                .time(time)
                .summary(summary)
                .count(1)
                .merged()
                .build();
    }

    private static SummaryInt64Snapshot summary(long count, long sum, long min, long max) {
        return new ImmutableSummaryInt64Snapshot(count, sum, min, max);
    }

    @Test
    public void setAndGet() {
        AggrPoint expectedPoint = point(
                "2018-03-10T09:00:00Z",
                summary(10, 100, 5, 15));

        AggrGraphDataArrayList list = AggrGraphDataArrayList.of(expectedPoint);
        AggrPoint result = list.getAnyPoint(0);

        assertThat(result, equalTo(expectedPoint));
        assertThat(list.getTsMillis(0), equalTo(expectedPoint.tsMillis));
        assertThat(list.getSummaryInt64(0), equalTo(expectedPoint.summaryInt64));
    }

    @Test
    public void listEqualWhenAllHistogramsEqual() {
        AggrGraphDataArrayList first = AggrGraphDataArrayList.of(
                point("2017-05-10T09:00:00Z", summary(1, 10, 10, 10)),
                point("2017-05-10T10:00:00Z", summary(3, 30, 0, 15))
        );

        AggrGraphDataArrayList second = AggrGraphDataArrayList.of(
                point("2017-05-10T09:00:00Z", summary(1, 10, 10, 10)),
                point("2017-05-10T10:00:00Z", summary(3, 30, 0, 15))
        );

        assertThat(first, equalTo(second));
    }

    @Test
    public void listDiffHistogramNotEqual() {
        AggrGraphDataArrayList first = AggrGraphDataArrayList.of(
                point("2017-05-10T09:00:00Z", summary(2, 15, 5, 10)),
                point("2017-05-10T10:00:00Z", summary(3, 30, 0, 15))
        );

        AggrGraphDataArrayList second = AggrGraphDataArrayList.of(
                point("2017-05-10T09:00:00Z", summary(10, 1021, 0, 1000)),
                point("2017-05-10T10:00:00Z", summary(15, 1051, 0, 1000))
        );

        assertThat(first, not(equalTo(second)));
    }

    @Test
    public void mask() {
        AggrGraphDataArrayList list = AggrGraphDataArrayList.of(
                point("2017-05-10T09:00:00Z", summary(2, 15, 5, 10))
        );

        int expectMask = StockpileColumn.ISUMMARY.mask() | StockpileColumn.TS.mask();
        assertThat(list.columnSetMask(), equalTo(expectMask));
    }

    @Test
    public void sortAndMergeSkipMerge() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2017-05-10T10:00:00Z", summary(2, 6, 1, 5)),
                point("2017-05-10T03:10:00Z", summary(1, 5, 5, 5)),
                point("2017-05-10T13:00:00Z", summary(3, 10, 1, 5))
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-05-10T03:10:00Z", summary(1, 5, 5, 5)),
                point("2017-05-10T10:00:00Z", summary(2, 6, 1, 5)),
                point("2017-05-10T13:00:00Z", summary(3, 10, 1, 5))
        );

        assertThat(source, equalTo(expected));
    }


    @Test
    public void sortAndMerge() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2017-05-10T03:10:00Z", summary(1, 5, 5, 5)),
                point("2017-05-10T10:10:00Z", summary(4, 20, 0, 10)),
                point("2017-05-10T03:10:00Z", summary(2, 15, 1, 5)),
                point("2017-05-10T03:10:00Z", summary(3, 16, 1, 5))
        );

        source.sortAndMerge();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                point("2017-05-10T03:10:00Z", summary(3, 16, 1, 5)), // latest win
                point("2017-05-10T10:10:00Z", summary(4, 20, 0, 10))
        );

        assertThat(source, equalTo(expected));
    }

    @Test
    public void sortAndMergeSum() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                pointAggregate("2017-05-10T03:10:00Z", summary(1, 5, 5, 5)),
                pointAggregate("2017-05-10T10:10:00Z", summary(10, 41, 2, 11)),
                pointAggregate("2017-05-10T03:10:00Z", summary(3, 10, 2, 5)),
                pointAggregate("2017-05-10T03:10:00Z", summary(12, 40, 4, 10))
        );

        AggrGraphDataArrayList sorted = source.toSortedMerged();

        AggrGraphDataArrayList expected = AggrGraphDataArrayList.of(
                AggrPoint.builder()
                        .time("2017-05-10T03:10:00Z")
                        .count(3)
                        .merged()
                        .summary(summary(16, 55, 2, 10))
                        .build(),
                AggrPoint.builder()
                        .time("2017-05-10T10:10:00Z")
                        .count(1)
                        .merged()
                        .summary(summary(10, 41, 2, 11))
                        .build()
        );

        assertThat(sorted, equalTo(expected));
    }

    @Test
    public void toView() {
        AggrGraphDataArrayList source = AggrGraphDataArrayList.of(
                point("2017-05-10T01:00:00Z", summary(1, 5, 5, 5)),
                point("2017-05-10T02:00:00Z", summary(4, 25, 1, 15)),
                point("2017-05-10T03:10:00Z", summary(8, 50, 1, 20))
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
                point("2017-05-10T01:00:00Z", summary(1, 5, 5, 5)),
                point("2017-05-10T02:00:00Z", summary(4, 25, 1, 15)),
                point("2017-05-10T03:10:00Z", summary(8, 50, 1, 20))
        );

        AggrGraphDataArrayListView view = source.view();

        AggrGraphDataArrayList result = new AggrGraphDataArrayList(view.columnSetMask(), view.length());
        result.addAllFrom(view.iterator());
        assertThat(result, equalTo(source));
    }
}
