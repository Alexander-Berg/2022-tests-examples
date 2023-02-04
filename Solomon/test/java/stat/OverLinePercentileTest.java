package ru.yandex.solomon.math.stat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.Timeline;


/**
 * @author Vladimir Gordiychuk
 */
public class OverLinePercentileTest {

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    @Test
    public void simplePercentileSameOnSamePoint() throws Exception {
        List<GraphData> source = Arrays.asList(
            GraphData.of(
                point("2017-03-01T00:00:00Z", 8),
                point("2017-03-02T00:00:00Z", 8),
                point("2017-03-03T00:00:00Z", 8),
                point("2017-03-04T00:00:00Z", 8)
            ),
            GraphData.of(
                point("2017-03-01T00:00:00Z", 8),
                point("2017-03-02T00:00:00Z", 8),
                point("2017-03-03T00:00:00Z", 8),
                point("2017-03-04T00:00:00Z", 8)
            ),
            GraphData.of(
                point("2017-03-01T00:00:00Z", 8),
                point("2017-03-02T00:00:00Z", 8),
                point("2017-03-03T00:00:00Z", 8),
                point("2017-03-04T00:00:00Z", 8)
            )
        );

        GraphData expected = GraphData.of(
            point("2017-03-01T00:00:00Z", 8),
            point("2017-03-02T00:00:00Z", 8),
            point("2017-03-03T00:00:00Z", 8),
            point("2017-03-04T00:00:00Z", 8)
        );

        GraphData result = simplePercentile(source, 85);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void simplePercentilePerPointInTime() throws Exception {
        String time1 = "2017-03-01T00:00:00Z";
        String time2 = "2017-03-02T00:00:00Z";
        String time3 = "2017-03-03T00:00:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point(time1, 5), point(time2, 123), point(time3, 0)),
            GraphData.of(point(time1, 5), point(time2, 123), point(time3, 0)),
            GraphData.of(point(time1, 5), point(time2, 123), point(time3, 0))
        );

        GraphData expected = GraphData.of(
            point(time1, 5),
            point(time2, 123),
            point(time3, 0)
        );

        GraphData result = simplePercentile(source, 85);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void simplePercentile() throws Exception {
        String time = "2017-03-21T00:00:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point(time, 15)),
            GraphData.of(point(time, 20)),
            GraphData.of(point(time, 35)),
            GraphData.of(point(time, 40)),
            GraphData.of(point(time, 50))
        );

        GraphData expected = GraphData.of(point(time, 48));

        GraphData result = simplePercentile(source, 80);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void simplePercentileOnMultipleTimePoints() throws Exception {
        String time1 = "2017-03-23T00:00:00Z";
        String time2 = "2017-03-23T00:10:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point(time1, 40), point(time2, -21)),
            GraphData.of(point(time1, 20), point(time2, 10)),
            GraphData.of(point(time1, 50), point(time2, 31)),
            GraphData.of(point(time1, 15), point(time2, 12)),
            GraphData.of(point(time1, 35), point(time2, 95))
        );

        GraphData expected = GraphData.of(point(time1, 50), point(time2, 95));

        GraphData result = simplePercentile(source, 99.9);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void simplePercentileDifferentResultForDifferentPercentile() throws Exception {
        String time = "2017-03-25T00:00:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point(time, 3)),
            GraphData.of(point(time, 10)),
            GraphData.of(point(time, 8)),
            GraphData.of(point(time, 6)),
            GraphData.of(point(time, 7)),
            GraphData.of(point(time, 20)),
            GraphData.of(point(time, 13))
        );

        GraphData percentile80 = simplePercentile(source, 80);
        GraphData percentile40 = simplePercentile(source, 40);

        Assert.assertThat(percentile40, CoreMatchers.not(CoreMatchers.equalTo(percentile80)));
    }

    @Test
    public void simplePercentileIgnoreNans() throws Exception {
        String time = "2017-03-10T00:00:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point(time, Double.NaN)),
            GraphData.of(point(time, 3)),
            GraphData.of(point(time, 7)),
            GraphData.of(point(time, 20)),
            GraphData.of(point(time, Double.NaN)),
            GraphData.of(point(time, 15)),
            GraphData.of(point(time, 9)),
            GraphData.of(point(time, Double.NaN)),
            GraphData.of(point(time, 13)),
            GraphData.of(point(time, 8)),
            GraphData.of(point(time, Double.NaN))
        );

        GraphData expected = GraphData.of(point(time, 19));

        GraphData result = simplePercentile(source, 85);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void simplePercentileWhenAllValueIsNanResultAlsoNan() throws Exception {
        String time1 = "2017-03-23T00:00:00Z";
        String time2 = "2017-03-23T00:10:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point(time1, 40), point(time2, Double.NaN)),
            GraphData.of(point(time1, 20), point(time2, Double.NaN)),
            GraphData.of(point(time1, 50), point(time2, Double.NaN)),
            GraphData.of(point(time1, 15), point(time2, Double.NaN)),
            GraphData.of(point(time1, 35), point(time2, Double.NaN))
        );

        GraphData expected = GraphData.of(point(time1, 50), point(time2, Double.NaN));

        GraphData result = simplePercentile(source, 85);
        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore
    public void simplePercentilePointShouldBeOnTheSameTimeLine() throws Exception {
        String time1 = "2017-03-23T00:00:00Z";
        List<GraphData> source = Arrays.asList(
            GraphData.of(point("2017-03-23T00:00:00Z", 12), point("2017-03-24T00:00:00Z", 0)),
            GraphData.of(point("2017-03-23T00:00:00Z", 92), point("2017-03-24T00:15:00Z", 15)),
            GraphData.of(point("2017-03-23T00:00:00Z", 43), point("2017-03-24T00:30:00Z", 30))
        );

        GraphData result = simplePercentile(source, 85);
        Assert.fail("Over lines percentile available to calculate only for graph data time-aligned, without it we can " +
            "line with merged points because on each particular point will exists only one value, " +
            "that not enough to calculate percentile.  " +
            "Test should fail but calculate: " + result
        );
    }

    @Test
    public void multiThreadEstimation() throws Exception {
        double p80 = IntStream.range(1, 1000)
                .parallel()
                .mapToDouble(a -> {
                    List<GraphData> source = IntStream.range(0, ThreadLocalRandom.current().nextInt(1, 5000))
                            .mapToObj(ignore -> GraphData.of(point("2017-03-23T00:00:00Z", 42)))
                            .collect(Collectors.toList());

                    GraphData result = simplePercentile(source, 80);
                    return result.getValues().first();
                })
                .min()
                .orElse(0);

        Assert.assertThat(p80, CoreMatchers.equalTo(42d));
    }

    private GraphData simplePercentile(Collection<GraphData> source, double percentile) {
        if (source.isEmpty()) {
            return GraphData.empty;
        }

        Timeline timeline = source.iterator().next().getTimeline();
        return OverLinePercentile.percentileOnParticularTime(source, timeline, percentile);
    }
}
