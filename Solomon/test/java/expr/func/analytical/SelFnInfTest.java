package ru.yandex.solomon.expression.expr.func.analytical;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.compile.SelAssignment;
import ru.yandex.solomon.expression.expr.SelExprValue;
import ru.yandex.solomon.expression.value.SelValueBoolean;
import ru.yandex.solomon.expression.version.SelVersion;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class SelFnInfTest {
    @Test
    public void value() {
        double size = ProgramTestSupport.expression("inf();")
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(size, equalTo(Double.POSITIVE_INFINITY));
    }

    @Test
    public void neg() {
        double size = ProgramTestSupport.expression("-inf();")
                .exec()
                .getAsSelValue()
                .castToScalar()
                .getValue();

        assertThat(size, equalTo(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void inlined() {
        for (SelVersion version : SelVersion.values()) {
            PreparedProgram pp = ProgramTestSupport.expression("-inf();")
                    .prepare(version)
                    .getPrepared();

            SelAssignment result = (SelAssignment) pp.getCode().get(1);
            assertThat(result.getIdent(), equalTo("data"));
            assertThat(result.getExpr(), instanceOf(SelExprValue.class));
            SelExprValue value = (SelExprValue) result.getExpr();
            assertThat(value.getValue().castToScalar().getValue(), equalTo(Double.NEGATIVE_INFINITY));
        }
    }

    @Test
    public void inlined2() {
        for (SelVersion version : SelVersion.values()) {
            PreparedProgram pp = ProgramTestSupport.expression("inf() == 1 / 0;")
                    .prepare(version)
                    .getPrepared();

            SelAssignment result = (SelAssignment) pp.getCode().get(1);
            assertThat(result.getIdent(), equalTo("data"));
            assertThat(result.getExpr(), instanceOf(SelExprValue.class));
            SelExprValue value = (SelExprValue) result.getExpr();
            assertThat(value.getValue().castToBoolean(), equalTo(SelValueBoolean.TRUE));
        }
    }
}
