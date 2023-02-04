package ru.yandex.solomon.expression.analytics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.math.doubles.AggregateFunctionType;
import ru.yandex.solomon.util.time.Interval;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class ProgramRequestTest {

    @Test
    public void onlyLoad() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("let first = {host=cluster};\n");
        builder.append("let second = {host=man, service=sys};\n");
        Program program = Program.fromSource(builder.toString()).compile();

        Interval interval = interval("2017-05-30T14:09:00Z", "2017-05-30T15:09:00Z");
        PreparedProgram prepared = program.prepare(interval);

        GraphDataLoadRequest first = GraphDataLoadRequest.newBuilder("host=cluster")
            .setInterval(interval)
            .setType(SelTypes.GRAPH_DATA_VECTOR)
            .build();

        GraphDataLoadRequest second = GraphDataLoadRequest.newBuilder("host=man, service=sys")
            .setInterval(interval)
            .setType(SelTypes.GRAPH_DATA_VECTOR)
            .build();

        assertThat(prepared.getLoadRequests(), hasItems(first, second));
    }

    @Test
    public void intervalCorrectWithShift() throws Exception {
        Program program = Program.fromSource("let data = shift({host=cluster}, 1d);").compile();
        PreparedProgram prepared = program.prepare(interval("2017-05-30T14:15:00Z", "2017-05-30T15:09:00Z"));

        GraphDataLoadRequest result = prepared.getLoadRequests()
            .stream()
            .findFirst()
            .orElse(null);

        GraphDataLoadRequest expected =
            GraphDataLoadRequest.newBuilder("host=cluster")
                .setInterval(interval("2017-05-29T14:15:00Z", "2017-05-29T15:09:00Z"))
                .setType(SelTypes.GRAPH_DATA_VECTOR)
                .build();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void propagateGridMillisToRequest() throws Exception {
        Interval interval = interval("2017-05-30T14:15:00Z", "2017-05-30T15:09:00Z");
        Program program = Program.fromSource("let data = group_by_time(42s, 'min', {host='*'});").compile();
        PreparedProgram prepared = program.prepare(interval);

        GraphDataLoadRequest result =
            prepared.getLoadRequests()
                .stream()
                .findFirst()
                .orElse(null);

        GraphDataLoadRequest expected =
            GraphDataLoadRequest.newBuilder("host=*")
                .setInterval(interval)
                .setType(SelTypes.GRAPH_DATA_VECTOR)
                .setGridMillis(TimeUnit.SECONDS.toMillis(42L))
                .setAggregateFunction(AggregateFunctionType.MIN)
                .build();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void skipGridMillisPropagate() throws Exception {
        Interval interval = interval("2017-05-30T15:00:00Z", "2017-05-30T17:00:00Z");
        Program program = Program.fromSource("let data = group_by_time(42s, 'max', derivative({host='*'}));").compile();
        PreparedProgram prepared = program.prepare(interval);

        GraphDataLoadRequest result =
            prepared.getLoadRequests()
                .stream()
                .findFirst()
                .orElse(null);

        GraphDataLoadRequest expected =
            GraphDataLoadRequest.newBuilder("host=*")
                .setInterval(interval)
                .setType(SelTypes.GRAPH_DATA_VECTOR)
                .setGridMillis(0)
                .build();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void downSampleAndShift() throws Exception {
        Program program = Program.fromSource("let data = shift(group_by_time(5m, 'last', {host=cluster}), 1d);").compile();
        PreparedProgram prepared = program.prepare(interval("2017-05-30T14:15:00Z", "2017-05-30T15:09:00Z"));

        GraphDataLoadRequest result = prepared.getLoadRequests()
            .stream()
            .findFirst()
            .orElse(null);

        GraphDataLoadRequest expected =
            GraphDataLoadRequest.newBuilder("host=cluster")
                .setInterval(interval("2017-05-29T14:15:00Z", "2017-05-29T15:09:00Z"))
                .setType(SelTypes.GRAPH_DATA_VECTOR)
                .setGridMillis(TimeUnit.MINUTES.toMillis(5L))
                .setAggregateFunction(AggregateFunctionType.LAST)
                .build();

        assertThat(result, equalTo(expected));
    }

    @Test
    public void granularLoadRequests() throws Exception {
        StringBuilder builder = new StringBuilder()
            .append("let original = {host=cluster};\n")
            .append("let shift2d = shift({host=cluster}, 2d);\n")
            .append("let shift1d = shift({host=cluster}, 1d);\n")
            .append("let shift3d = shift({host=cluster}, 3d);\n")
            .append("let shift360d = shift({host=cluster}, 360d);\n");

        Interval interval = interval("2017-05-30T14:15:00Z", "2017-05-30T15:09:00Z");
        Program program = Program.fromSource(builder.toString()).compile();
        PreparedProgram prepared = program.prepare(interval);

        assertThat(prepared.getLoadRequests(), allOf(
                iterableWithSize(5),
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(interval)
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .build())),
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(shift(interval, 1, ChronoUnit.DAYS))
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .build())),
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(shift(interval, 2, ChronoUnit.DAYS))
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .build())),
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(shift(interval, 3, ChronoUnit.DAYS))
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .build())),
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(shift(interval, 360, ChronoUnit.DAYS))
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .build()))
        ));
    }

    @Test
    public void downsamplingWithDifferentParams() throws Exception {
        StringBuilder builder = new StringBuilder()
                .append("let original = {host=cluster};\n")
                .append("let d5Avg = group_by_time(5m, 'avg', {host=cluster});\n")
                .append("let d10Sum = group_by_time(10m, 'sum', {host=cluster});\n");

        Interval interval = interval("2017-05-30T14:15:00Z", "2017-05-30T15:09:00Z");
        Program program = Program.fromSource(builder.toString()).compile();
        PreparedProgram prepared = program.prepare(interval);

        assertThat(prepared.getLoadRequests(), allOf(
                iterableWithSize(3),

                // original
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(interval)
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .build())),

                // downsampling 5 min avg
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(interval)
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .setGridMillis(TimeUnit.MINUTES.toMillis(5))
                        .setAggregateFunction(AggregateFunctionType.AVG)
                        .build())),

                // downsampling 10 min sum
                hasItem(equalTo(GraphDataLoadRequest.newBuilder("host=cluster")
                        .setInterval(interval)
                        .setType(SelTypes.GRAPH_DATA_VECTOR)
                        .setGridMillis(TimeUnit.MINUTES.toMillis(10))
                        .setAggregateFunction(AggregateFunctionType.SUM)
                        .build()))
        ));
    }

    private static Interval interval(String from, String to) {
        return new Interval(Instant.parse(from), Instant.parse(to));
    }

    private static Interval shift(Interval interval, long value, ChronoUnit unit) {
        Instant begin = interval.getBegin().minus(value, unit);
        Instant end = interval.getEnd().minus(value, unit);
        return new Interval(begin, end);
    }
 }
