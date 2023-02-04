package ru.yandex.solomon.expression.analytics;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.math.Crop;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.SortedOrCheck;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Vladimir Gordiychuk
 */
public class ProgramLoadTest {
    private GraphDataLoaderStub dataLoaderStub;

    @Before
    public void setUp() throws Exception {
        dataLoaderStub = new GraphDataLoaderStub();
    }

    @Test
    public void loadDataAsIs() {
        Instant now = Instant.parse("2017-04-04T14:25:29Z");
        long timeStep = TimeUnit.SECONDS.toMillis(15L);
        GraphData expectedGraphData = generateGraphData(5, now, timeStep, 1, 10);

        dataLoaderStub.putSelectorValue("name=Jose", expectedGraphData);

        GraphData graphData = executeExpression("{name=Jose}", new Interval(now, now.plus(30, ChronoUnit.DAYS)));

        Assert.assertEquals(expectedGraphData, graphData);
    }

    @Test
    public void loadDataWithStartTimeRestriction() throws Exception {
        Instant now = Instant.parse("2017-04-04T00:00:00Z");
        Instant startTime = now.minus(1, ChronoUnit.DAYS);
        long timeStep = TimeUnit.HOURS.toMillis(1L);
        Interval interval = new Interval(now.minus(12, ChronoUnit.HOURS), now);

        GraphData graphData = generateGraphData(24, startTime, timeStep, 5, 1);
        GraphData expectedGraphData = Crop.crop(graphData, interval);

        dataLoaderStub.putSelectorValue("name=Kim", graphData);

        GraphData result = executeExpression("{name=Kim}", interval);

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void loadDataWithEndTimeRestriction() throws Exception {
        Instant now = Instant.parse("2017-04-04T14:00:00Z");
        Instant startTime = now.minus(1, ChronoUnit.DAYS);
        long timeStep = TimeUnit.HOURS.toMillis(1L);
        Instant halfDay = now.minus(12, ChronoUnit.HOURS);
        Interval interval = new Interval(startTime, halfDay);

        GraphData graphData = generateGraphData(24, startTime, timeStep, 5, 1);
        GraphData expectedGraphData = Crop.crop(graphData, interval);

        dataLoaderStub.putSelectorValue("name=Buford", graphData);

        GraphData result = executeExpression("{name=Buford}", interval);

        Assert.assertEquals(expectedGraphData, result);
    }

    /**
     * Source load window [2017-03-28T00:00:00.000Z; 2017-03-28T12:00:00.000Z]
     * wait load for shift like this
     * shift(crop(prefetch$selectors_0, [2017-03-27T00:00:00Z; 2017-03-27T12:00:00Z]), "1d")
     */
    @Test
    public void loadShifted() throws Exception {
        Instant now = Instant.parse("2017-03-28T00:00:00.000Z");
        Instant startTime = now.minus(Duration.ofDays(1));
        long timeStep = TimeUnit.HOURS.toMillis(1L);
        int countPoints = 24;

        Interval loadInterval = new Interval(now, now.plus(Duration.ofHours(12)));
        GraphData sourceGraphData = generateGraphData(countPoints, startTime, timeStep, 1, 10);
        dataLoaderStub.putSelectorValue("name=ann", sourceGraphData);

        GraphData expectedGraphData =
            Crop.crop(generateGraphData(countPoints, now, timeStep, 1, 10), loadInterval);

        GraphData result = executeExpression("shift({name=ann}, 1d)", loadInterval);

        Assert.assertEquals(expectedGraphData, result);
    }

    /**
     * Expected next execution for interval [2017-03-13T00:00:00Z; 2017-03-13T03:00:00Z]
     *
     * <pre>
     *     let data = crop(prefetch$selectors_0, [2017-03-11T00:00:00Z; 2017-03-12T03:00:00Z]);
     *     let shiftedOneDay = shift(crop(data, [2017-03-12T00:00:00Z; 2017-03-12T03:00:00Z]), 1d);
     *     let shiftedTwoDay = shift(crop(data, [2017-03-11T00:00:00Z; 2017-03-11T03:00:00Z]), 2d);
     * </pre>
     */
    @Test
    public void loadWithDifferentShiftOnSameMetric() throws Exception {
        StringBuilder srcBuilder = new StringBuilder();
        srcBuilder.append("let data = {name=data_to_shift};\n");
        srcBuilder.append("let shiftedOneDay = shift(data, 1d);\n");
        srcBuilder.append("let shiftedTwoDay = shift(data, 2d);\n");

        Instant dataStart = Instant.parse("2017-03-10T00:00:00.000Z");
        Instant loadStart = dataStart.plus(Duration.ofDays(3));
        Instant loadEnd = loadStart.plus(Duration.ofHours(3));

        Interval loadInterval = new Interval(loadStart, loadEnd);

        GraphData sourceGraphData =
            generateGraphData(100, dataStart, Duration.ofHours(1).toMillis(), 1, 1);
        dataLoaderStub.putSelectorValue("name=data_to_shift", sourceGraphData);

        Map<String, SelValue> evaluationResult = executeProgram(srcBuilder.toString(), loadInterval);

        GraphData shiftedOneDay = evaluationResult.get("shiftedOneDay").castToGraphData().getGraphData();
        GraphData expectedShiftedOneDay = GraphData.of(
            point("2017-03-13T00:00:00Z", 49),
            point("2017-03-13T01:00:00Z", 50),
            point("2017-03-13T02:00:00Z", 51),
            point("2017-03-13T03:00:00Z", 52)
        );
        Assert.assertEquals(expectedShiftedOneDay, shiftedOneDay);

        GraphData shiftedTwoDay = evaluationResult.get("shiftedTwoDay").castToGraphData().getGraphData();
        GraphData expectedShiftedTwoDay = GraphData.of(
            point("2017-03-13T00:00:00Z", 25),
            point("2017-03-13T01:00:00Z", 26),
            point("2017-03-13T02:00:00Z", 27),
            point("2017-03-13T03:00:00Z", 28)
        );
        Assert.assertEquals(expectedShiftedTwoDay, shiftedTwoDay);
    }

    /**
     * Source load window [2017-03-29T23:00:00Z; 2017-03-30T00:00:00Z]
     * wait extend left side to something like this
     * moving_avg(crop(prefetch$selectors_0, [2017-03-29T22:00:00Z; 2017-03-30T00:00:00Z]), 1h)
     */
    @Test
    public void loadWithMovingAverage() throws Exception {
        Instant now = Instant.parse("2017-03-29T00:00:00.000Z");
        Instant loadStart = Instant.parse("2017-03-29T23:00:00.000Z");
        long timeStep = TimeUnit.MINUTES.toMillis(10L);
        int countPoints = 6 * 24; //point every 10 minutes whole day

        Interval loadInterval = new Interval(loadStart, loadStart.plus(Duration.ofHours(1)));

        GraphData sourceGraphData = generateGraphData(countPoints, now, timeStep, 1, 1);
        dataLoaderStub.putSelectorValue("analytic=move_average", sourceGraphData);

        GraphData result = executeExpression("moving_avg({analytic=move_average}, 1h)", loadInterval);

        GraphData expectedGraphData = GraphData.of(
            point("2017-03-29T23:00:00Z", 136.0),
            point("2017-03-29T23:10:00Z", 137.0),
            point("2017-03-29T23:20:00Z", 138.0),
            point("2017-03-29T23:30:00Z", 139.0),
            point("2017-03-29T23:40:00Z", 140.0),
            point("2017-03-29T23:50:00Z", 141.0)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    @Test
    public void loadShiftAndMovingAverage() throws Exception {
        StringBuilder srcBuilder = new StringBuilder();
        srcBuilder.append("let data = {name=shift_and_move_average};\n");
        srcBuilder.append("let waitValue = shift(moving_avg(data, 1h), 1d);\n");

        Instant startDataTime = Instant.parse("2017-03-01T00:00:00.000Z");
        int pointCount = 24 * 2 * 3; //3 days every 30 minutes

        GraphData sourceGraphData =
            generateGraphData(pointCount, startDataTime, Duration.ofMinutes(30).toMillis(), 0, 10);
        dataLoaderStub.putSelectorValue("name=shift_and_move_average", sourceGraphData);

        Instant loadStart = Instant.parse("2017-03-02T12:12:12.000Z");
        Instant loadEnd = loadStart.plus(Duration.ofHours(1));

        Interval loadInterval = new Interval(loadStart, loadEnd);

        Map<String, SelValue> evaluationResult = executeProgram(srcBuilder.toString(), loadInterval);

        GraphData result = evaluationResult.get("waitValue").castToGraphData().getGraphData();
        GraphData expectedGraphData = GraphData.of(
            point("2017-03-02T12:30:00Z", 240.0),
            point("2017-03-02T13:00:00Z", 250.0)
        );

        Assert.assertEquals(expectedGraphData, result);
    }

    private GraphData generateGraphData(int size, Instant startTime, long timeStep, double startValue, double valueStep) {
        long[] times = new long[size];
        double[] values = new double[size];

        Instant nextTime = startTime;
        double nextValue = startValue;
        for (int index = 0;index < size; index++) {
            times[index] = nextTime.toEpochMilli();
            values[index] = nextValue;

            nextTime = nextTime.plusMillis(timeStep);
            nextValue = nextValue + valueStep;
        }

        return new GraphData(times, values, SortedOrCheck.CHECK);
    }

    private Program createProgram(List<String> externalExpressions, Map<String, String> selectors) {
        return Program.fromSource("")
                .withExternalExpressions(externalExpressions)
                .withSelectors(selectors)
                .compile();
    }

    private Program createProgram(String program) {
        return Program.fromSource(program).compile();
    }

    private Map<String, SelValue> executeProgram(String src, Interval interval) {
        Program p = createProgram(src);
        PreparedProgram preparedProgram = p.prepare(interval);
        return preparedProgram.evaluate(dataLoaderStub, Collections.emptyMap());
    }

    private GraphData executeExpression(String expression, Interval interval) {
        Program p = createProgram(Collections.singletonList(expression), Collections.emptyMap());
        PreparedProgram preparedProgram = p.prepare(interval);

        Map<String, SelValue> evaluationResult =
            preparedProgram.evaluate(dataLoaderStub, Collections.emptyMap());

        return evaluationResult
            .get(preparedProgram.expressionToVarName(expression))
            .castToGraphData()
            .getGraphData();
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

}
