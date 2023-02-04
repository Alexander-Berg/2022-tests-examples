package ru.yandex.solomon.expression.analytics;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import ru.yandex.solomon.expression.type.SelTypes;
import ru.yandex.solomon.labels.query.Selectors;
import ru.yandex.solomon.math.doubles.AggregateFunctionType;
import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.util.time.Interval;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class GraphDataLoadRequestTest {

    @Test
    public void toBuilderAndBack() {
        var random = ThreadLocalRandom.current();
        var source = GraphDataLoadRequest.newBuilder(Selectors.parse("a == b"))
                .setType(random.nextBoolean() ? SelTypes.GRAPH_DATA : SelTypes.GRAPH_DATA_VECTOR)
                .setInterval(Interval.millis(random.nextLong(1, 10_000), random.nextLong(10_000, 10_000_000)))
                .setAggregateFunction(AggregateFunctionType.AVG)
                .setRankFilter(random.nextBoolean(), random.nextInt(1, 100), Aggregation.SUM)
                .setGridMillis(random.nextLong(300_000))
                .build();
        var result = source.toBuilder().build();
        assertEquals(source, result);
    }
}
