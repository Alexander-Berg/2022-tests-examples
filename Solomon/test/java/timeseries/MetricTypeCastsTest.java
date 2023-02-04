package ru.yandex.solomon.model.timeseries;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.monlib.metrics.MetricType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.solomon.model.timeseries.MetricTypeCasts.commonType;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MetricTypeCastsTest {
    private static List<MetricType> getTypes() {
        return Arrays.stream(MetricType.values())
            .collect(Collectors.toList());
    }

    @Test
    public void idempotence() {
        var types = getTypes();

        for (var type : types) {
            assertThat(String.format("%1$s o %1$s == %1$s", type),
                commonType(type, type), equalTo(type));
        }
    }

    @Test
    public void commutativity() {
        var types = getTypes();

        for (var left : types) {
            for (var right : types) {
                var lr = commonType(left, right);
                var rl = commonType(right, left);
                assertThat(String.format("%1$s o %2$s == %2$s o %1$s", left, right),
                    lr, equalTo(rl));
            }
        }
    }

    @Test
    public void associativity() {
        var types = getTypes();

        for (var left : types) {
            for (var mid : types) {
                for (var right : types) {
                    var lm = commonType(left, mid);
                    var mr = commonType(mid, right);
                    if (lm == null || mr == null) {
                        continue;
                    }
                    var lm_r = commonType(lm, right);
                    var l_mr = commonType(left, mr);
                    assertThat(String.format("(%1$s o %2$s) o %3$s == %1$s o (%2$s o %3$s)", left, mid, right),
                        lm_r, equalTo(l_mr));
                }
            }
        }
    }
}
