package ru.yandex.solomon.expression.expr.func.analytical;

import org.junit.Test;

import ru.yandex.solomon.expression.analytics.ProgramTestSupport;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class SelFnBinomialDistributionTest {

    @Test
    public void distribution() {
        var result = ProgramTestSupport.expression("binomial_distribution(20, 0.25, 5);")
            .exec()
            .getAsSelValue()
            .castToScalar()
            .getValue();

        assertEquals(0.20, result, 0.01);
    }
}
