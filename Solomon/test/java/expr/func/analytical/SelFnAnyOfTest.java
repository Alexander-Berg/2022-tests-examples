package ru.yandex.solomon.expression.expr.func.analytical;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnAnyOfTest {

    @Test
    public void falseWhenEmptyVector() throws Exception {
        SelValue result = ProgramTestSupport.expression("any_of(map(graphData, sensor -> max(sensor) > 10));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.FALSE));
    }

    @Test
    public void falseWhenAllFalse() throws Exception {
        SelValue result = ProgramTestSupport.expression("any_of(as_vector(false, false, false));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.FALSE));
    }

    @Test
    public void trueWhenOneOfTrue() throws Exception {
        SelValue result = ProgramTestSupport.expression("any_of(as_vector(false, true, false));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.TRUE));
    }

    @Test
    public void trueWhenAllTrue() throws Exception {
        SelValue result = ProgramTestSupport.expression("any_of(as_vector(true, true, true));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.TRUE));
    }
}
