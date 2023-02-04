package ru.yandex.solomon.expression.expr.func.analytical;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.compile.DeprOpts;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueGraphData;
import ru.yandex.solomon.model.point.AggrPoints;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class SelFnFallbackTest {

    private static final PreparedProgram prepared = Program
            .fromSource("let result = fallback({signal=signal}, {signal=fallback});")
            .withDeprOpts(DeprOpts.ALERTING)
            .compile()
            .prepare(Interval.millis(100_000, 200_000));

    private GraphDataLoaderStub graphDataLoader = new GraphDataLoaderStub();

    @Before
    public void setUp() {
        graphDataLoader = new GraphDataLoaderStub();
    }

    public NamedGraphData[] eval(NamedGraphData[] signal, NamedGraphData[] fallback) {
        graphDataLoader.putSelectorValue("signal=signal", signal);
        graphDataLoader.putSelectorValue("signal=fallback", fallback);

        return Arrays.stream(prepared.evaluate(graphDataLoader, Map.of())
                .get("result")
                .castToVector()
                .valueArray())
                .map(SelValue::castToGraphData)
                .map(SelValueGraphData::getNamedGraphData)
                .collect(Collectors.toList())
                .toArray(NamedGraphData[]::new);
    }

    @Test
    public void emptyBoth() {
        NamedGraphData[] result = eval(new NamedGraphData[] {}, new NamedGraphData[] {});
        assertEquals(0, result.length);
    }

    @Test
    public void emptySignal() {
        NamedGraphData line = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(150_000, 42d))
                .build();
        NamedGraphData empty = NamedGraphData.newBuilder()
                .setGraphData(GraphData.empty)
                .build();
        NamedGraphData[] result = eval(new NamedGraphData[] {}, new NamedGraphData[] {line});
        assertArrayEquals(new NamedGraphData[] {line}, result);

        NamedGraphData[] result2 = eval(new NamedGraphData[] {empty}, new NamedGraphData[] {line});
        assertArrayEquals(new NamedGraphData[] {line}, result2);
    }

    @Test
    public void emptyFallback() {
        NamedGraphData line = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(150_000, 42d))
                .build();
        NamedGraphData empty = NamedGraphData.newBuilder()
                .setGraphData(GraphData.empty)
                .build();
        NamedGraphData[] result = eval(new NamedGraphData[] {line}, new NamedGraphData[] {});
        assertArrayEquals(new NamedGraphData[] {line}, result);

        NamedGraphData[] result2 = eval(new NamedGraphData[] {line}, new NamedGraphData[] {empty});
        assertArrayEquals(new NamedGraphData[] {line}, result2);
    }

    @Test
    public void allPresent() {
        NamedGraphData signal = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 42d,
                        150_000, 43d,
                        180_000, 44d
                ))
                .build();
        NamedGraphData fallback = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 100500d,
                        180_000, 100500d
                ))
                .build();
        NamedGraphData[] result = eval(new NamedGraphData[] {signal}, new NamedGraphData[] {fallback});
        assertArrayEquals(new NamedGraphData[] {signal}, result);
    }

    @Test
    public void holesFilled() {
        NamedGraphData signal = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 42d,
                        180_000, 44d
                ))
                .build();
        NamedGraphData fallback = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 100500d,
                        150_000, 100500d,
                        180_000, 100500d
                ))
                .build();
        NamedGraphData[] result = eval(new NamedGraphData[] {signal}, new NamedGraphData[] {fallback});

        assertArrayEquals(new NamedGraphData[] {NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 42d,
                        150_000, 100500d,
                        180_000, 44d
                ))
                .build()}, result);
    }

    @Test
    public void zerosFilled() {
        NamedGraphData signal = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 42d,
                        150_000, 0d,
                        180_000, 44d
                ))
                .build();
        NamedGraphData fallback = NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 100500d,
                        150_000, 100500d,
                        180_000, 100500d
                ))
                .build();
        NamedGraphData[] result = eval(new NamedGraphData[] {signal}, new NamedGraphData[] {fallback});
        assertArrayEquals(new NamedGraphData[] {NamedGraphData.newBuilder()
                .setGraphData(GraphData.graphData(
                        120_000, 42d,
                        150_000, 100500d,
                        180_000, 44d
                ))
                .build()}, result);
    }

    @Test
    public void holesFilledInSummary() {
        NamedGraphData signal = NamedGraphData.newBuilder()
                .setGraphData(MetricType.DSUMMARY, AggrGraphDataArrayList.of(
                        AggrPoints.point(120_000, AggrPoints.summaryDouble(1, 2, 3)),
                        AggrPoints.point(150_000, AggrPoints.summaryDouble()),
                        AggrPoints.point(180_000, AggrPoints.summaryDouble(4, 5))
                ))
                .build();
        NamedGraphData fallback = NamedGraphData.newBuilder()
                .setGraphData(MetricType.DSUMMARY, AggrGraphDataArrayList.of(
                        AggrPoints.point(120_000, AggrPoints.summaryDouble()),
                        AggrPoints.point(150_000, AggrPoints.summaryDouble(20, 21)),
                        AggrPoints.point(180_000, AggrPoints.summaryDouble())
                ))
                .build();
        NamedGraphData[] result = eval(new NamedGraphData[] {signal}, new NamedGraphData[] {fallback});
        assertArrayEquals(new NamedGraphData[] {NamedGraphData.newBuilder()
                .setGraphData(MetricType.DSUMMARY, AggrGraphDataArrayList.of(
                        AggrPoints.point(120_000, AggrPoints.summaryDouble(1, 2, 3)),
                        AggrPoints.point(150_000, AggrPoints.summaryDouble(20, 21)),
                        AggrPoints.point(180_000, AggrPoints.summaryDouble(4, 5))
                ))
                .build()}, result);
    }

}
