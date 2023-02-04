package ru.yandex.solomon.expression.expr.func;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;
import ru.yandex.solomon.expression.value.SelValue;
import ru.yandex.solomon.expression.value.SelValueDouble;

/**
 * @author Vladimir Gordiychuk
 */
public class DoubleSelFnSqrTest {

    @Test
    public void testSqr() {
        SelValue selValue = ProgramTestSupport.expression("sqr(5);")
                .exec()
                .getAsSelValue();

        SelValue expected = new SelValueDouble(25);
        Assert.assertThat(selValue, CoreMatchers.equalTo(expected));
    }

    @Test
    public void testSqrMoreSymbols() {
        SelValue selValue = ProgramTestSupport.expression("sqr((5));")
                .exec()
                .getAsSelValue();

        SelValue expected = new SelValueDouble(25);
        Assert.assertThat(selValue, CoreMatchers.equalTo(expected));
    }

    @Test
    public void testSqrByPow() {
        SelValue selValue = ProgramTestSupport.expression("pow(5, 2);")
                .exec()
                .getAsSelValue();

        SelValue expected = new SelValueDouble(25);
        Assert.assertThat(selValue, CoreMatchers.equalTo(expected));
    }
}
