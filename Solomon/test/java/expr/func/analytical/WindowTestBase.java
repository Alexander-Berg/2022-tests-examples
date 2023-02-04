package ru.yandex.solomon.expression.expr.func.analytical;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import ru.yandex.solomon.expression.analytics.GraphDataLoaderStub;
import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.model.point.DataPoint;
import ru.yandex.solomon.model.timeseries.GraphData;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class WindowTestBase {
    protected GraphData executeProgram(GraphData graphData, String program) {
        String fullProgram = "let data = " + program.replace("graphData", "{name=testData}");
        Program p = Program.fromSource(fullProgram).compile();
        PreparedProgram preparedProgram = p.prepare(graphData.getTimeline().interval());

        GraphDataLoaderStub dataLoaderStub = new GraphDataLoaderStub();
        dataLoaderStub.putSelectorValue("name=testData", graphData);

        Map<String, SelValue> evalutionResult =
            preparedProgram.evaluate(dataLoaderStub, Collections.emptyMap());

        return evalutionResult.get("data").castToGraphData().getGraphData();
    }

    protected DataPoint point(String time, double value) {
        return new DataPoint(Instant.parse(time), value);
    }
}
