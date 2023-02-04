package ru.yandex.solomon.math;

import java.time.Duration;

import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.model.point.DataPoint.point;

/**
 * @author Vladimir Gordiychuk
 */
public class GraphDataMathTest {

    @Test
    public void emptyHeadPoints() {
        GraphData result = GraphDataMath.head(GraphData.empty, 3);
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyTailPoints() {
        GraphData result = GraphDataMath.tail(GraphData.empty, 3);
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyDropHeadPoints() {
        GraphData result = GraphDataMath.dropHead(GraphData.empty, 3);
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyDropTailPoints() {
        GraphData result = GraphDataMath.dropTail(GraphData.empty, 3);
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyHeadDuration() {
        GraphData result = GraphDataMath.head(GraphData.empty, Duration.ofMinutes(10));
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyTailDuration() {
        GraphData result = GraphDataMath.tail(GraphData.empty, Duration.ofMinutes(10));
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyDropHeadDuration() {
        GraphData result = GraphDataMath.dropHead(GraphData.empty, Duration.ofMinutes(10));
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void emptyDropTailDuration() {
        GraphData result = GraphDataMath.dropTail(GraphData.empty, Duration.ofMinutes(10));
        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void headPointsSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.head(source, 10), equalTo(source));
    }

    @Test
    public void tailPointsSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.tail(source, 10), equalTo(source));
    }

    @Test
    public void dropHeadPointsSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.dropHead(source, 10), equalTo(GraphData.empty));
    }

    @Test
    public void dropTailPointsSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.dropTail(source, 10), equalTo(GraphData.empty));
    }

    @Test
    public void dropEmptyHeadPointsSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.dropHead(source, 0), equalTo(source));
    }

    @Test
    public void dropEmptyTailPointsSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.dropTail(source, 0), equalTo(source));
    }

    @Test
    public void headDurationSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.head(source, Duration.ofMinutes(50)), equalTo(source));
    }

    @Test
    public void tailDurationSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.tail(source, Duration.ofMinutes(50)), equalTo(source));
    }

    @Test
    public void dropHeadDurationSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.dropHead(source, Duration.ofMinutes(50)), equalTo(GraphData.empty));
    }

    @Test
    public void dropTailDurationSmall() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:37Z", 10),
                point("2018-06-02T07:42:37Z", 42)
        );

        assertThat(GraphDataMath.dropTail(source, Duration.ofMinutes(50)), equalTo(GraphData.empty));
    }

    @Test
    public void tailNanAlsoPoints() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:00Z", 10),
                point("2018-06-02T07:33:00Z", 20),
                point("2018-06-02T07:33:10Z", Double.NaN),
                point("2018-06-02T07:43:00Z", 30)
        );

        GraphData expected = GraphData.of(
                point("2018-06-02T07:33:10Z", Double.NaN),
                point("2018-06-02T07:43:00Z", 30));

        assertThat(GraphDataMath.tail(source, 2), equalTo(expected));
    }

    @Test
    public void headNanAlsoPoints() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:00Z", 10),
                point("2018-06-02T07:33:00Z", Double.NaN),
                point("2018-06-02T07:33:30Z", Double.NaN),
                point("2018-06-02T07:43:00Z", 30)
        );

        GraphData expected = GraphData.of(
                point("2018-06-02T07:32:00Z", 10),
                point("2018-06-02T07:33:00Z", Double.NaN)
        );

        assertThat(GraphDataMath.head(source, 2), equalTo(expected));
    }

    @Test
    public void dropTailNanAlsoPoints() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:00Z", 10),
                point("2018-06-02T07:33:00Z", 20),
                point("2018-06-02T07:33:10Z", Double.NaN),
                point("2018-06-02T07:43:00Z", 30)
        );

        GraphData expected = GraphData.of(
                point("2018-06-02T07:32:00Z", 10),
                point("2018-06-02T07:33:00Z", 20));

        assertThat(GraphDataMath.dropTail(source, 2), equalTo(expected));
    }

    @Test
    public void dropHeadNanAlsoPoints() {
        GraphData source = GraphData.of(
                point("2018-06-02T07:32:00Z", 10),
                point("2018-06-02T07:33:00Z", Double.NaN),
                point("2018-06-02T07:33:30Z", Double.NaN),
                point("2018-06-02T07:43:00Z", 30)
        );

        GraphData expected = GraphData.of(
                point("2018-06-02T07:33:30Z", Double.NaN),
                point("2018-06-02T07:43:00Z", 30)
        );

        assertThat(GraphDataMath.dropHead(source, 2), equalTo(expected));
    }

    @Test
    public void ensureHeadSplitDuration() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50),
                point("2018-06-13T07:05:00Z", 60),
                point("2018-06-13T07:06:00Z", 70),
                point("2018-06-13T07:07:00Z", 80),
                point("2018-06-13T07:08:00Z", 90),
                point("2018-06-13T07:09:00Z", 100),
                point("2018-06-13T07:10:00Z", 110),
                point("2018-06-13T07:11:00Z", 120)
        );

        GraphData headExpect = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40)
        );

        GraphData dropHeadExpect = GraphData.of(
                point("2018-06-13T07:04:00Z", 50),
                point("2018-06-13T07:05:00Z", 60),
                point("2018-06-13T07:06:00Z", 70),
                point("2018-06-13T07:07:00Z", 80),
                point("2018-06-13T07:08:00Z", 90),
                point("2018-06-13T07:09:00Z", 100),
                point("2018-06-13T07:10:00Z", 110),
                point("2018-06-13T07:11:00Z", 120)
        );

        assertThat(GraphDataMath.head(source, Duration.ofMinutes(3)), equalTo(headExpect));
        assertThat(GraphDataMath.dropHead(source, Duration.ofMinutes(3)), equalTo(dropHeadExpect));

        assertThat(GraphDataMath.head(source, Duration.ofSeconds(200)), equalTo(headExpect));
        assertThat(GraphDataMath.dropHead(source, Duration.ofSeconds(200)), equalTo(dropHeadExpect));
    }

    @Test
    public void ensureTailSplitDuration() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50),
                point("2018-06-13T07:05:00Z", 60),
                point("2018-06-13T07:06:00Z", 70),
                point("2018-06-13T07:07:00Z", 80),
                point("2018-06-13T07:08:00Z", 90),
                point("2018-06-13T07:09:00Z", 100),
                point("2018-06-13T07:10:00Z", 110),
                point("2018-06-13T07:11:00Z", 120)
        );

        GraphData dropTailExpect = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50),
                point("2018-06-13T07:05:00Z", 60),
                point("2018-06-13T07:06:00Z", 70),
                point("2018-06-13T07:07:00Z", 80)
        );

        GraphData tailExpect = GraphData.of(
                point("2018-06-13T07:08:00Z", 90),
                point("2018-06-13T07:09:00Z", 100),
                point("2018-06-13T07:10:00Z", 110),
                point("2018-06-13T07:11:00Z", 120)
        );

        assertThat(GraphDataMath.tail(source, Duration.ofMinutes(3)), equalTo(tailExpect));
        assertThat(GraphDataMath.dropTail(source, Duration.ofMinutes(3)), equalTo(dropTailExpect));

        assertThat(GraphDataMath.tail(source, Duration.ofSeconds(200)), equalTo(tailExpect));
        assertThat(GraphDataMath.dropTail(source, Duration.ofSeconds(200)), equalTo(dropTailExpect));
    }

    @Test
    public void emptyDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.empty;

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(source));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(GraphData.empty));
    }

    @Test
    public void effectivelyEmptyDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.of(
                point("2018-06-13T07:01:00Z", 0),
                point("2018-06-13T07:02:00Z", 0),
                point("2018-06-13T07:03:00Z", 0)
        );

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(source));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(GraphData.empty));
    }

    @Test
    public void coincidenceDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.of(
                point("2018-06-13T07:01:00Z", 0),
                point("2018-06-13T07:02:00Z", 1),
                point("2018-06-13T07:03:00Z", 0)
        );
        GraphData expectedDrop = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData expectedTake = GraphData.of(
                point("2018-06-13T07:02:00Z", 30)
        );

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(expectedDrop));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(expectedTake));
    }

    @Test
    public void afterDropDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.of(
                point("2018-06-13T07:02:30Z", 1)
        );
        GraphData expectedDrop = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30)
        );
        GraphData expectedTake = GraphData.of(
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(expectedDrop));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(expectedTake));
    }

    @Test
    public void everythingDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.of(
                point("2018-06-13T06:00:00Z", 1),
                point("2018-06-13T07:04:01Z", 0)
        );

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(GraphData.empty));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(source));
    }

    @Test
    public void frequentDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.of(
                point("2018-06-13T06:59:35Z", 1),
                point("2018-06-13T07:00:05Z", 0),
                point("2018-06-13T07:00:35Z", 1),
                point("2018-06-13T07:01:05Z", 0),
                point("2018-06-13T07:01:35Z", 0),
                point("2018-06-13T07:02:05Z", 1),
                point("2018-06-13T07:02:35Z", 0),
                point("2018-06-13T07:03:05Z", 1),
                point("2018-06-13T07:03:35Z", 1),
                point("2018-06-13T07:04:05Z", 0)
        );
        GraphData expectedDrop = GraphData.of(
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40)
        );
        GraphData expectedTake = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:04:00Z", 50)
        );

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(expectedDrop));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(expectedTake));
    }

    @Test
    public void nanDropTakeIf() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );
        GraphData condition = GraphData.of(
                point("2018-06-13T06:59:35Z", 1),
                point("2018-06-13T06:59:55Z", Double.NaN),
                point("2018-06-13T07:00:05Z", 0),
                point("2018-06-13T07:00:35Z", 1),
                point("2018-06-13T07:01:05Z", 0),
                point("2018-06-13T07:01:35Z", 0),
                point("2018-06-13T07:01:55Z", Double.NaN),
                point("2018-06-13T07:02:05Z", 1),
                point("2018-06-13T07:02:35Z", 0),
                point("2018-06-13T07:03:05Z", 1),
                point("2018-06-13T07:03:35Z", 1),
                point("2018-06-13T07:04:05Z", 0)
        );
        GraphData expectedDrop = GraphData.of(
                point("2018-06-13T07:02:00Z", 30),
                point("2018-06-13T07:03:00Z", 40)
        );
        GraphData expectedTake = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:04:00Z", 50)
        );

        assertThat(GraphDataMath.dropIf(condition, source), equalTo(expectedDrop));
        assertThat(GraphDataMath.takeIf(condition, source), equalTo(expectedTake));
    }

    @Test
    public void getTimestampsTestEmpty() {
        DoubleArrayView result = GraphDataMath.getTimestampsAsEpochSeconds(GraphData.empty);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getTimestampsTestWithNaNs() {
        GraphData source = GraphData.of(
                point("2018-06-13T07:00:00Z", 10),
                point("2018-06-13T07:01:00Z", 20),
                point("2018-06-13T07:02:00Z", Double.NaN),
                point("2018-06-13T07:03:00Z", 40),
                point("2018-06-13T07:04:00Z", 50)
        );

        double first = 1528873200.0;
        DoubleArrayView expectedResult = new DoubleArrayView(new double [] {
                first,
                first+60.0,
                first+180.0,
                first+240.0
        });

        DoubleArrayView result = GraphDataMath.getTimestampsAsEpochSeconds(source);
        assertThat(result, equalTo(expectedResult));
    }
}
