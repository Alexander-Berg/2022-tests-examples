package ru.yandex.solomon.expression.expr.func;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.version.SelVersion;

import static org.junit.Assert.assertNotEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFn0Random01Test {

    @Test
    public void differentResult() {
        for (SelVersion version : SelVersion.values()) {
            double v1 = ProgramTestSupport.expression("random01();")
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();

            double v2 = ProgramTestSupport.expression("random01();")
                    .exec(version)
                    .getAsSelValue()
                    .castToScalar()
                    .getValue();

            assertNotEquals(v1, v2, 0d);
        }
    }

    @Test
    public void randomIsNotAPureFunction() {
        for (SelVersion version : SelVersion.values()) {
            ProgramTestSupport.Prepared prepared = ProgramTestSupport.expression("random01();")
                    .prepare(version);

            double v1 = prepared.exec().getAsSelValue().castToScalar().getValue();
            double v2 = prepared.exec().getAsSelValue().castToScalar().getValue();

            assertNotEquals(v1, v2, 0d);
        }
    }
}
