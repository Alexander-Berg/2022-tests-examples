package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Label;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnGroupByLabelsTest {
    @Test
    public void empty() throws Exception {
        GraphData[] result = ProgramTestSupport.expression("group_by_labels(graphData, 'host', v -> group_lines('max', v));")
                .onMultipleLines(new NamedGraphData[0])
                .exec()
                .getAsMultipleLines();

        assertThat(result, emptyArray());
    }

    @Test
    public void groupBySingleLabel() throws Exception {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        NamedGraphData[] source = {
                graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
                graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
                graph(Labels.of("host", "test-2", "sensor", "freeSpace", "disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
                graph(Labels.of("host", "test-1", "sensor", "freeSpace", "disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("group_by_labels(graphData, 'host', v -> group_lines('sum', v));")
                .onMultipleLines(source)
                .exec()
                .getAsMultipleLines();

        assertThat(result, allOf(
                arrayWithSize(2),
                hasItemInArray(GraphData.of(point(ts1, 100 + 1015), point(ts2, 142 + 2411))),
                hasItemInArray(GraphData.of(point(ts1, 108 + 123), point(ts2, 2000 + 321)))
        ));
    }

    @Test
    public void groupByMultipleLabel() throws Exception {
        String ts1 = "2017-12-01T00:00:00Z";

        Labels host1 = Labels.of("sensor", "requestCount", "cluster", "pre", "host", "solomon-1");
        Labels host2 = Labels.of("sensor", "requestCount", "cluster", "pre", "host", "solomon-2");

        NamedGraphData[] source = {
                graph(host1.add("endpoint", "/ok"), point(ts1, 5)),
                graph(host1.add("endpoint", "/metric"), point(ts1, 2)),

                graph(host2.add("endpoint", "/ok"), point(ts1, 42)),
                graph(host2.add("endpoint", "/metric"), point(ts1, 10)),

                graph(host1.add("cluster", "prod").add("endpoint", "/ok"), point(ts1, 1000)),
                graph(host1.add("cluster", "prod").add("endpoint", "/metric"), point(ts1, 3000))
        };

        GraphData[] result = ProgramTestSupport.expression("group_by_labels(graphData, as_vector('cluster', 'host'), v -> group_lines('sum', v));")
                .onMultipleLines(source)
                .exec()
                .getAsMultipleLines();

        assertThat(result, allOf(
                arrayWithSize(3),
                hasItemInArray(GraphData.of(point(ts1, 5 + 2))),
                hasItemInArray(GraphData.of(point(ts1, 42 + 10))),
                hasItemInArray(GraphData.of(point(ts1, 1000 + 3000)))
        ));
    }

    @Test
    public void groupByLabelsViAlias() throws Exception {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        Labels host1 = Labels.of("host", "test-1", "sensor", "freeSpace");
        Labels host2 = Labels.of("host", "test-2", "sensor", "freeSpace");

        NamedGraphData[] source = {
                graph(host1.add("disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
                graph(host2.add("disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
                graph(host2.add("disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
                graph(host1.add("disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("sum(graphData) by host;")
                .onMultipleLines(source)
                .exec()
                .getAsMultipleLines();

        assertThat(result, allOf(
                arrayWithSize(2),
                hasItemInArray(GraphData.of(point(ts1, 100 + 1015), point(ts2, 142 + 2411))),
                hasItemInArray(GraphData.of(point(ts1, 108 + 123), point(ts2, 2000 + 321)))
        ));
    }

    @Test
    public void groupByLabelsBiAliasSaveCommonLabels() throws Exception {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        Labels host1 = Labels.of("host", "test-1", "sensor", "freeSpace");
        Labels host2 = Labels.of("host", "test-2", "sensor", "freeSpace");

        NamedGraphData[] source = {
                graph(host1.add("disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
                graph(host2.add("disk", "/dev/sda1"), point(ts1, 108), point(ts2, 2000)),
                graph(host2.add("disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
                graph(host1.add("disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        NamedGraphData[] lines = ProgramTestSupport.expression("sum(graphData) by host;")
                .onMultipleLines(source)
                .exec()
                .getAsNamedMultipleLines();

        List<Labels> labels = Stream.of(lines)
                .map(NamedGraphData::getLabels)
                .collect(Collectors.toList());

        assertThat(labels,
                allOf(
                        hasItem(Labels.of("host", "test-1", "sensor", "freeSpace")),
                        hasItem(Labels.of("host", "test-2", "sensor", "freeSpace"))
                ));
    }

    @Test
    public void groupByLabelsAndDivide() throws Exception {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:00:15Z";

        Labels dcMan = Labels.of("DC", "MAN", "name", "container_latency", "category", "runtime", "host", "cluster");
        Labels dcSas = Labels.of("DC", "SAS", "name", "container_latency", "category", "runtime", "host", "cluster");

        NamedGraphData[] source = {
                // rps 26; 17
                graph(dcMan.add("latency", "100"), point(ts1, 321), point(ts2, 240)),
                graph(dcMan.add("latency", "200"), point(ts1, 65), point(ts2, 9)),
                graph(dcMan.add("latency", "300"), point(ts1, 4), point(ts2, 6)),

                // rps 12
                graph(dcSas.add("latency", "100"), point(ts2, 123)),
                graph(dcSas.add("latency", "200"), point(ts2, 42)),
                graph(dcSas.add("latency", "300"), point(ts2, 13)),
                graph(dcSas.add("latency", "500"), point(ts2, 2)),
        };

        NamedGraphData[] lines = ProgramTestSupport.expression("sum(graphData) by 'DC' / 15;")
                .onMultipleLines(source)
                .exec()
                .getAsNamedMultipleLines();

        NamedGraphData man = Stream.of(lines)
                .filter(gd -> {
                    Label dc = gd.getLabels().findByKey("DC");
                    return dc != null && dc.getValue().equals("MAN");
                })
                .findFirst()
                .orElseThrow(NullPointerException::new);

        NamedGraphData sas = Stream.of(lines)
                .filter(gd -> {
                    Label dc = gd.getLabels().findByKey("DC");
                    return dc != null && dc.getValue().equals("SAS");
                })
                .findFirst()
                .orElseThrow(NullPointerException::new);

        assertThat(man.getLabels(), equalTo(Labels.of("DC", "MAN", "name", "container_latency", "category", "runtime", "host", "cluster")));
        assertThat(man.getGraphData(), equalTo(GraphData.of(point(ts1, 26), point(ts2, 17))));

        assertThat(sas.getLabels(), equalTo(Labels.of("DC", "SAS", "name", "container_latency", "category", "runtime", "host", "cluster")));
        assertThat(sas.getGraphData(), equalTo(GraphData.of(point(ts2, 12))));
    }

    @Test
    public void groupByLabelsCount() {
        String ts1 = "2017-12-01T00:00:00Z";
        String ts2 = "2017-12-01T00:01:00Z";

        Labels host1 = Labels.of("host", "test-1", "sensor", "freeSpace");
        Labels host2 = Labels.of("host", "test-2", "sensor", "freeSpace");

        NamedGraphData[] source = {
            graph(host1.add("disk", "/dev/sda1"), point(ts1, 100), point(ts2, 142)),
            graph(host2.add("disk", "/dev/sda2"), point(ts1, 123), point(ts2, 321)),
            graph(host1.add("disk", "/dev/sdb"), point(ts1, 1015), point(ts2, 2411))
        };

        GraphData[] result = ProgramTestSupport.expression("count(graphData) by host;")
            .onMultipleLines(source)
            .exec()
            .getAsMultipleLines();

        GraphData[] expected = {
            GraphData.of(point(ts1, 1), point(ts2, 1)),
            GraphData.of(point(ts1, 2), point(ts2, 2))
        };

        assertArrayEquals(expected, result);
    }

    private DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }

    private NamedGraphData graph(Labels labels, DataPoint... points) {
        return NamedGraphData.of(labels, points);
    }
}
