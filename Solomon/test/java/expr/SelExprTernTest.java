package ru.yandex.solomon.expression.expr;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.MultilineProgramTestSupport;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertEquals;


/**
 * @author Vladimir Gordiychuk
 */
public class SelExprTernTest {

    @Test
    public void ternOp() {
        var result = MultilineProgramTestSupport.create()
            .addLine("let is_red = last({sensor=mySensor}) > 30;")
            .addLine("let is_yellow = last({sensor=mySensor}) > 10;")
            .addLine("let trafficColor = is_red ? 'red' : (is_yellow ? 'yellow' : 'green');")
            .onData("sensor=mySensor", GraphData.graphData(System.currentTimeMillis(), 42))
            .forTimeInterval(Interval.millis(System.currentTimeMillis() - 10_000, System.currentTimeMillis() + 10_000))
            .exec()
            .get("trafficColor")
            .getAsSelValue()
            .castToString()
            .getValue();

        assertEquals("red", result);
    }
}
