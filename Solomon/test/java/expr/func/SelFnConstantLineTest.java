package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoadRequest;
import ru.yandex.solomon.expression.analytics.GraphDataLoader;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleg Baryshnikov
 */
@ParametersAreNonnullByDefault
public class SelFnConstantLineTest {
    @Test
    public void simple() {
        GraphData actual = ProgramTestSupport.expression("constant_line(100);")
            .forTimeInterval(new Interval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(2000)))
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);
        GraphData expected = GraphData.graphData(
            1000_000, 100,
            2000_000, 100
        );
        assertEquals(expected, actual);
    }

    @Test
    public void complex() {
        GraphData source = GraphData.of(
            DataPoint.point("2020-01-21T00:00:00Z", 10),
            DataPoint.point("2020-01-21T00:01:00Z", 20),
            DataPoint.point("2020-01-21T00:02:00Z", 30),
            DataPoint.point("2020-01-21T00:03:00Z", 40)
        );

        Interval interval = new Interval(Instant.parse("2020-01-21T00:00:00Z"), Instant.parse("2020-01-21T00:05:00Z"));

        GraphData actual = ProgramTestSupport.expression("constant_line(avg(graphData));")
            .onSingleLine(source)
            .forTimeInterval(interval)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);
        GraphData expected = GraphData.graphData(
            interval.getBeginMillis(), 25,
            interval.getEndMillis(), 25
        );
        assertEquals(expected, actual);
    }

    @Test
    public void returnType() {
        SelValue scalar = ProgramTestSupport.expression("constant_line(100);")
                .forTimeInterval(new Interval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(2000)))
                .exec(SelVersion.BASIC_1)
                .getAsSelValue();

        SelValue vector = ProgramTestSupport.expression("constant_line(100);")
                .forTimeInterval(new Interval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(2000)))
                .exec(SelVersion.GROUP_LINES_RETURN_VECTOR_2)
                .getAsSelValue();

        Assert.assertEquals(SelTypes.GRAPH_DATA, scalar.type());
        Assert.assertEquals(SelTypes.GRAPH_DATA_VECTOR, vector.type());
        Assert.assertEquals(scalar, vector.castToVector().item(0));
    }

    @Test
    public void gridded() {
        Interval interval = new Interval(Instant.parse("2020-01-21T00:00:17Z"), Instant.parse("2020-01-21T00:05:23Z"));

        GraphData actual = ProgramTestSupport.expression("constant_line(42, 15s);")
                .forTimeInterval(interval)
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        long first = Instant.parse("2020-01-21T00:00:30Z").toEpochMilli();
        long last = Instant.parse("2020-01-21T00:05:30Z").toEpochMilli();

        AggrGraphDataArrayList graphDataArrayList = new AggrGraphDataArrayList();
        for (long ts = first; ts < last; ts += 15_000) {
            graphDataArrayList.addRecordShort(ts, 42d);
        }

        GraphData expected = graphDataArrayList.toGraphDataShort();
        assertEquals(expected, actual);
    }

    @Test
    public void withKnownGridMillis() {
        Interval interval = new Interval(Instant.parse("2020-01-21T00:00:17Z"), Instant.parse("2020-01-21T00:05:23Z"));
        final long gridMillis = 30_000;

        var prepared = Program.fromSource(SelVersion.MAX, "let data = constant_line(42);")
                .compile()
                .prepare(interval);
        var preparedWithData = new ProgramTestSupport.Prepared(prepared, new GraphDataLoader() {
            @Override
            public NamedGraphData[] loadGraphData(GraphDataLoadRequest request) {
                throw new NotImplementedException("stub");
            }

            @Override
            public long getSeriesGridMillis() {
                return gridMillis;
            }
        });

        GraphData actual = preparedWithData
                .exec()
                .getAsSingleLine();

        long first = Instant.parse("2020-01-21T00:00:30Z").toEpochMilli();
        long last = Instant.parse("2020-01-21T00:05:30Z").toEpochMilli();

        AggrGraphDataArrayList graphDataArrayList = new AggrGraphDataArrayList();
        for (long ts = first; ts < last; ts += gridMillis) {
            graphDataArrayList.addRecordShort(ts, 42d);
        }

        GraphData expected = graphDataArrayList.toGraphDataShort();
        assertEquals(expected, actual);
    }
}
