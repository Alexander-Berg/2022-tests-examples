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
public class SelFnAllOfTest {

    @Test
    public void trueWhenEmptyVector() throws Exception {
        SelValue result = ProgramTestSupport.expression("all_of(map(graphData, sensor -> max(sensor) > 20));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.TRUE));
    }

    @Test
    public void falseWhenAllFalse() throws Exception {
        SelValue result = ProgramTestSupport.expression("all_of(as_vector(false, false, false));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.FALSE));
    }

    @Test
    public void falseWhenOneOfFalse() throws Exception {
        SelValue result = ProgramTestSupport.expression("all_of(as_vector(true, false, true));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.FALSE));
    }

    @Test
    public void trueWhenAllTrue() throws Exception {
        SelValue result = ProgramTestSupport.expression("all_of(as_vector(true, true, true));")
                .exec()
                .getAsSelValue();

        assertThat(result, equalTo(SelValueBoolean.TRUE));
    }
}
