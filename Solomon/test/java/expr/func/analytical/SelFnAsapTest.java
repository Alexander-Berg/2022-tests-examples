package ru.yandex.solomon.expression.expr.func.analytical;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.GraphDataArrayList;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnAsapTest {

    @Test
    public void empty() {
        GraphData result = ProgramTestSupport.expression("asap(graphData);")
                .onSingleLine(GraphData.empty)
                .fromTime("2018-06-02T06:50:00Z")
                .toTime("2018-06-02T06:59:00Z")
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(GraphData.empty));
    }

    @Test
    public void constant() {
        long ts0 = System.currentTimeMillis();

        GraphDataArrayList list = new GraphDataArrayList(100);
        for (int index = 0; index < 100; index++) {
            list.add(ts0 + (index * 15_000), 42.0);
        }

        GraphData source = list.buildGraphData();

        GraphData result = ProgramTestSupport.expression("asap(graphData);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        assertThat(result, equalTo(source));
    }

    @Test
    public void smooth() {
        long ts0 = System.currentTimeMillis();

        GraphDataArrayList list = new GraphDataArrayList(100);
        for (int index = 0; index < 100; index++) {
            double value = ThreadLocalRandom.current().nextDouble(40.0, 50.0);
            list.add(ts0 + (index * 15_000), value);
        }

        GraphData source = list.buildGraphData();
        GraphData result = ProgramTestSupport.expression("asap(graphData);")
                .onSingleLine(source)
                .exec()
                .getAsSingleLine();

        DoubleArrayView values = result.getValues();
        for (int index = 50; index < values.length(); index++) {
            Assert.assertEquals(45, values.at(index), 5.0);
        }
    }
}
