package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnPercentileGroupLinesTest {

    @Test
    public void emptySingleLine() throws Exception {
        GraphData result = ProgramTestSupport.expression("percentile_group_lines(99.9, graphData);")
            .onMultipleLines(GraphData.empty)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void emptyMultipleLines() throws Exception {
        GraphData result = ProgramTestSupport.expression("percentile_group_lines(99.9, graphData);")
            .onMultipleLines(GraphData.empty, GraphData.empty, GraphData.empty, GraphData.empty)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        Assert.assertThat(result, CoreMatchers.equalTo(GraphData.empty));
    }

    @Test
    public void percentileForSingleLine() throws Exception {
        NamedGraphData source = NamedGraphData.newBuilder()
            .setGraphData(GraphData.of(
                point("2017-06-08T15:05:00Z", 2),
                point("2017-06-08T15:06:00Z", 4),
                point("2017-06-08T15:07:00Z", 5),
                point("2017-06-08T15:08:00Z", 0),
                point("2017-06-08T15:09:00Z", Double.NaN),
                point("2017-06-08T15:10:00Z", 10)
            ))
            .setLabels(Labels.of("label1", "value1"))
            .build();

        NamedGraphData result = ProgramTestSupport.expression("percentile_group_lines(80, graphData);")
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsNamedSingleLine);

        Assert.assertThat(result, CoreMatchers.equalTo(source.toBuilder().setAlias("p80.0").build()));
    }

    @Test
    public void percentileForSamePointData() throws Exception {
        NamedGraphData result = ProgramTestSupport.expression("percentile_group_lines(90, graphData);")
            .onMultipleLines(
                NamedGraphData.newBuilder()
                    .setLabels(Labels.of("label1", "value1", "label2", "value1"))
                    .setGraphData(
                        GraphData.of(
                            point("2017-06-08T15:05:00Z", 5),
                            point("2017-06-08T15:06:00Z", 1),
                            point("2017-06-08T15:07:00Z", 6)
                        )
                    ).build(),

                NamedGraphData.newBuilder()
                    .setLabels(Labels.of("label1", "value1", "label2", "value2"))
                    .setGraphData(GraphData.of(
                        point("2017-06-08T15:05:00Z", 5),
                        point("2017-06-08T15:06:00Z", 1),
                        point("2017-06-08T15:07:00Z", 6)
                    )).build(),

                NamedGraphData.newBuilder()
                    .setLabels(Labels.of("label1", "value1", "label2", "value3"))
                    .setGraphData(GraphData.of(
                        point("2017-06-08T15:05:00Z", 5),
                        point("2017-06-08T15:06:00Z", 1),
                        point("2017-06-08T15:07:00Z", 6)
                    )).build()
            )
            .exec(ProgramTestSupport.ExecResult::getAsNamedSingleLine);

        NamedGraphData expected = NamedGraphData.newBuilder()
            .setAlias("p90.0")
            .setLabels(Labels.of("label1", "value1"))
            .setGraphData(
                GraphData.of(
                point("2017-06-08T15:05:00Z", 5),
                point("2017-06-08T15:06:00Z", 1),
                point("2017-06-08T15:07:00Z", 6)
            ))
            .build();

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void percentileForMultipleLines() throws Exception {
        GraphData result = ProgramTestSupport.expression("percentile_group_lines(80, graphData);")
            .onMultipleLines(
                GraphData.of(point("2017-06-08T15:05:00Z", 15), point("2017-06-08T15:06:00Z", 1)),
                GraphData.of(point("2017-06-08T15:05:00Z", 20), point("2017-06-08T15:06:00Z", 6)),
                GraphData.of(point("2017-06-08T15:05:00Z", 35), point("2017-06-08T15:06:00Z", 2)),
                GraphData.of(point("2017-06-08T15:05:00Z", 40), point("2017-06-08T15:06:00Z", 8)),
                GraphData.of(point("2017-06-08T15:05:00Z", 50), point("2017-06-08T15:06:00Z", 3))
            )
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
            point("2017-06-08T15:05:00Z", 48),
            point("2017-06-08T15:06:00Z", 7.6)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void percentileForMultipleLinesAlias() throws Exception {
        GraphData result = ProgramTestSupport.expression("series_percentile(80, graphData);")
                .onMultipleLines(
                        GraphData.of(point("2017-06-08T15:05:00Z", 15), point("2017-06-08T15:06:00Z", 1)),
                        GraphData.of(point("2017-06-08T15:05:00Z", 20), point("2017-06-08T15:06:00Z", 6)),
                        GraphData.of(point("2017-06-08T15:05:00Z", 35), point("2017-06-08T15:06:00Z", 2)),
                        GraphData.of(point("2017-06-08T15:05:00Z", 40), point("2017-06-08T15:06:00Z", 8)),
                        GraphData.of(point("2017-06-08T15:05:00Z", 50), point("2017-06-08T15:06:00Z", 3))
                )
                .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
                point("2017-06-08T15:05:00Z", 48),
                point("2017-06-08T15:06:00Z", 7.6)
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void differentPercentileValueLeadToDifferentResults() throws Exception {
        GraphData[] source = new GraphData[] {
            GraphData.of(point("2017-06-08T15:05:00Z", 15), point("2017-06-08T15:06:00Z", 1)),
            GraphData.of(point("2017-06-08T15:05:00Z", 20), point("2017-06-08T15:06:00Z", 6)),
            GraphData.of(point("2017-06-08T15:05:00Z", 35), point("2017-06-08T15:06:00Z", 2)),
            GraphData.of(point("2017-06-08T15:05:00Z", 40), point("2017-06-08T15:06:00Z", 8)),
            GraphData.of(point("2017-06-08T15:05:00Z", 50), point("2017-06-08T15:06:00Z", 3))
        };

        GraphData percentile_80 = ProgramTestSupport.expression("percentile_group_lines(80, graphData);")
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData percentile_20 = ProgramTestSupport.expression("percentile_group_lines(20, graphData);")
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        Assert.assertThat(percentile_20, CoreMatchers.not(CoreMatchers.equalTo(percentile_80)));
    }

    @Test
    public void percentileOnNotTimeAlightedData() throws Exception {
        GraphData base = GraphData.of(
            point("2017-06-08T15:05:00Z", 15),
            point("2017-06-08T15:06:00Z", 1)
        );

        GraphData shilftLeft = GraphData.of(
            point("2017-06-08T15:00:00Z", 20),
            point("2017-06-08T15:05:00Z", 30)
        );

        GraphData shilftRight = GraphData.of(
            point("2017-06-08T15:06:00Z", 20),
            point("2017-06-08T15:07:00Z", Double.NaN),
            point("2017-06-08T15:10:00Z", 40)
        );

        GraphData empty = GraphData.empty;

        GraphData smallEnough = GraphData.of(point("2017-06-08T15:05:50Z", 15));


        GraphData result = ProgramTestSupport.expression("percentile_group_lines(50, graphData);")
            .onMultipleLines(base, shilftLeft, shilftRight, empty, smallEnough)
            .exec(ProgramTestSupport.ExecResult::getAsSingleLine);

        GraphData expected = GraphData.of(
            point("2017-06-08T15:00:00Z", 20),   // 20
            point("2017-06-08T15:05:00Z", 22.5), // 15, 30
            point("2017-06-08T15:05:50Z", 15),   // 15
            point("2017-06-08T15:06:00Z", 10.5), // 1, 20
            point("2017-06-08T15:07:00Z", Double.NaN), // NaN
            point("2017-06-08T15:10:00Z", 40)    // 40
        );

        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void severalPercentilesForLines() throws Exception {
        final String ts = "2019-05-01T00:00:00Z";
        GraphData[] source = new GraphData[] {
          GraphData.of(point(ts, 0)),
          GraphData.of(point(ts, 1)),
          GraphData.of(point(ts, 2)),
          GraphData.of(point(ts, 3)),
          GraphData.of(point(ts, 4)),
          GraphData.of(point(ts, 5)),
          GraphData.of(point(ts, 6)),
          GraphData.of(point(ts, 7)),
          GraphData.of(point(ts, 8)),
          GraphData.of(point(ts, 9)),
        };

        NamedGraphData[] result = ProgramTestSupport.expression("percentile_group_lines(as_vector(50, 90), graphData);")
            .onMultipleLines(source)
            .exec(ProgramTestSupport.ExecResult::getAsNamedMultipleLines);

        NamedGraphData[] expected = new NamedGraphData[] {
            NamedGraphData.of(GraphData.of(point(ts, 4.5)), "", Labels.empty(), "p50.0"),
            NamedGraphData.of(GraphData.of(point(ts, 8.9)), "", Labels.empty(), "p90.0"),
        };


        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void severalPercentilesForLinesAlias() throws Exception {
        final String ts = "2019-05-01T00:00:00Z";
        GraphData[] source = new GraphData[] {
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 1)),
                GraphData.of(point(ts, 2)),
                GraphData.of(point(ts, 3)),
                GraphData.of(point(ts, 4)),
                GraphData.of(point(ts, 5)),
                GraphData.of(point(ts, 6)),
                GraphData.of(point(ts, 7)),
                GraphData.of(point(ts, 8)),
                GraphData.of(point(ts, 9)),
        };

        NamedGraphData[] result = ProgramTestSupport.expression("series_percentile([50, 90], graphData);")
                .onMultipleLines(source)
                .exec(ProgramTestSupport.ExecResult::getAsNamedMultipleLines);

        NamedGraphData[] expected = new NamedGraphData[] {
                NamedGraphData.of(GraphData.of(point(ts, 4.5)), "", Labels.empty(), "p50.0"),
                NamedGraphData.of(GraphData.of(point(ts, 8.9)), "", Labels.empty(), "p90.0"),
        };


        Assert.assertThat(result, CoreMatchers.equalTo(expected));
    }

    @Test
    public void returnTypeByVersion() {
        final String ts = "2019-05-01T00:00:00Z";
        GraphData[] source = new GraphData[] {
                GraphData.of(point(ts, 0)),
                GraphData.of(point(ts, 1)),
                GraphData.of(point(ts, 2)),
                GraphData.of(point(ts, 3)),
                GraphData.of(point(ts, 4)),
                GraphData.of(point(ts, 5)),
                GraphData.of(point(ts, 6)),
                GraphData.of(point(ts, 7)),
                GraphData.of(point(ts, 8)),
                GraphData.of(point(ts, 9)),
        };

        SelValue scalar = ProgramTestSupport.expression("percentile_group_lines(58, graphData);")
                .onMultipleLines(source)
                .exec(SelVersion.BASIC_1)
                .getAsSelValue();
        SelValue vector = ProgramTestSupport.expression("percentile_group_lines(58, graphData);")
                .onMultipleLines(source)
                .exec(SelVersion.GROUP_LINES_RETURN_VECTOR_2)
                .getAsSelValue();

        Assert.assertEquals(SelTypes.GRAPH_DATA, scalar.type());
        Assert.assertEquals(SelTypes.GRAPH_DATA_VECTOR, vector.type());
        Assert.assertEquals(scalar, vector.castToVector().item(0));
    }

    private static DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
