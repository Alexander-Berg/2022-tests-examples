package ru.yandex.solomon.expression.expr.func.analytical;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.monlib.metrics.MetricType;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.exceptions.EvaluationException;
import ru.yandex.solomon.expression.test.ForEachSelVersionRunner;
import ru.yandex.solomon.expression.test.VersionedSelTestBase;
import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.AggrGraphDataArrayList;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.model.point.AggrPoints.dhistogram;
import static ru.yandex.solomon.model.point.AggrPoints.point;

/**
 * @author Ivan Tsybulin
 */
@RunWith(ForEachSelVersionRunner.class)
public class SelFnSingleTest extends VersionedSelTestBase {

    public SelFnSingleTest(SelVersion version) {
        super(version);
    }

    @Test(expected = EvaluationException.class)
    public void throwOnEmptyVector() {
        ProgramTestSupport.expression("single(graphData);")
                .onMultipleLines(new GraphData[0])
                .exec();
    }

    @Test(expected = EvaluationException.class)
    public void throwOnManyElementVector() {
        ProgramTestSupport.expression("single(graphData);")
                .onMultipleLines(GraphData.empty, GraphData.empty)
                .exec();
    }

    @Test
    public void notForcedLoadSingleGraphData() {
        PreparedProgram p = Program.fromSource(version, "let data = graphData{}; let result = single(data);")
                .compile()
                .prepare(Interval.seconds(0, 1));
        assertThat(p.getLoadRequests().iterator().next().getType(), equalTo(SelTypes.GRAPH_DATA_VECTOR));
    }

    @Test
    public void changePrefetchVarType() {
        GraphDataLoaderStub loader = new GraphDataLoaderStub();
        loader.putSelectorValue("sensor=graphData", GraphData.of(
                DataPoint.point(150000, 42),
                DataPoint.point(160000, 43)
        ));
        PreparedProgram pp = Program.fromSource(version, "" +
                "let foo = single({sensor=graphData});" +
                "let baz = {sensor=graphData};" +
                "let bar = drop_tail(foo, 1h);")
                .compile()
                .prepare(Interval.seconds(100000, 200000));
        var loadRequests = new ArrayList<>(pp.getAllLoadRequests().values());
        assertThat(loadRequests.get(0).getType(), equalTo(SelTypes.GRAPH_DATA));
        assertThat(loadRequests.get(1).getType(), equalTo(SelTypes.GRAPH_DATA_VECTOR));

        Map<String, SelValue> res = pp.evaluate(loader, Map.of());
        assertThat(res.get("prefetch$selector_0").type(), equalTo(SelTypes.GRAPH_DATA));
        assertThat(res.get("prefetch$selector_1").type(), equalTo(SelTypes.GRAPH_DATA_VECTOR));
    }

    @Test
    public void sameAsLoad1() {
        PreparedProgram p = Program.fromSource(version, "" +
                "let result = max(single({sensor=graphData})) by 5m;" +
                "let result2 = max(load1('sensor=graphData')) by 5m;" +
                "let foo = shift(result, 1h);" +
                "let bar = shift(result2, 1h);")
                .compile()
                .prepare(Interval.seconds(100000, 200000));
        var loadRequests = new ArrayList<>(p.getAllLoadRequests().values());
        assertThat(loadRequests.get(0).getType(), equalTo(SelTypes.GRAPH_DATA));
        assertThat(loadRequests.get(0), equalTo(loadRequests.get(1)));
    }

    @Test
    public void notWorkingWithout() {
        NamedGraphData namedGraphData = new NamedGraphData(
                "someAlias", MetricType.HIST_RATE, "responseTime",
                Labels.of("host", "solomon-pre-00", "status", "2xx"),
                AggrGraphDataArrayList.of(
                        point(15_000, dhistogram(new double[] {10d, 20d}, new long[] {42, 14})),
                        point(30_000, dhistogram(new double[] {10d, 20d}, new long[] {80, 5}))
                )
        );
        SelValue result = ProgramTestSupport.expression("graphData;")
                .onMultipleLines(namedGraphData)
                .exec()
                .getAsSelValue();

        assertFalse(result.type().isGraphData());
    }

    @Test
    public void preserveAttributes() {
        NamedGraphData namedGraphData = new NamedGraphData(
                "someAlias", MetricType.HIST_RATE, "responseTime",
                Labels.of("host", "solomon-pre-00", "status", "2xx"),
                AggrGraphDataArrayList.of(
                        point(15_000, dhistogram(new double[] {10d, 20d}, new long[] {42, 14})),
                        point(30_000, dhistogram(new double[] {10d, 20d}, new long[] {80, 5}))
                )
        );
        SelValue result = ProgramTestSupport.expression("single(graphData);")
                .onMultipleLines(namedGraphData)
                .exec()
                .getAsSelValue();

        assertTrue(result.type().isGraphData());
        assertThat(result.castToGraphData().getNamedGraphData(), equalTo(namedGraphData));
    }
}
