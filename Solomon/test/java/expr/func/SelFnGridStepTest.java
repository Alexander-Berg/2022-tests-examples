package ru.yandex.solomon.expression.expr.func;

import java.time.Instant;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.NamedGraphData;
import ru.yandex.solomon.expression.analytics.GraphDataLoadRequest;
import ru.yandex.solomon.expression.analytics.GraphDataLoader;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.util.time.Interval;

public class SelFnGridStepTest {
    @Test
    public void unknown() {
        double scalar = ProgramTestSupport.expression("grid_step();")
                .forTimeInterval(new Interval(Instant.ofEpochSecond(1000), Instant.ofEpochSecond(2000)))
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        Assert.assertEquals(0d, scalar, 1e-14);
    }

    @Test
    public void withKnownGridMillis() {
        Interval interval = new Interval(Instant.parse("2020-01-21T00:00:17Z"), Instant.parse("2020-01-21T00:05:23Z"));
        final long gridMillis = 30_000;

        var prepared = Program.fromSource(SelVersion.MAX, "let data = grid_step();")
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

        double actual = preparedWithData
                .exec()
                .getAsSelValue().castToScalar().getValue();

        Assert.assertEquals(30d, actual, 1e-14);
    }
}
