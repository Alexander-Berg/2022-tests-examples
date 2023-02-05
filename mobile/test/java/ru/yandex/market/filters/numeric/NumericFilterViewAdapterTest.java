package ru.yandex.market.filters.numeric;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;


@RunWith(Enclosed.class)
public class NumericFilterViewAdapterTest {

    @RunWith(Parameterized.class)
    public static class SuccessTest {

        private static final BigDecimal MIN_VALUE = new BigDecimal(Long.MIN_VALUE);
        private static final BigDecimal MAX_VALUE = new BigDecimal(Long.MAX_VALUE);

        private static final BigDecimal sampleMinValue = BigDecimal.valueOf(1000);
        private static final BigDecimal sampleMaxValue = BigDecimal.valueOf(5000);

        @Parameterized.Parameter
        public BigDecimal resultExpected;

        @Parameterized.Parameter(1)
        public String parseStr;

        @Parameterized.Parameter(2)
        public BigDecimal defaultValue;

        @Parameterized.Parameter(3)
        public boolean isMin;

        @Parameterized.Parameters(name = "{index}: [\"{1}\", \"{2}\", \"{3}\"] → \"{0}\"")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {MIN_VALUE, "", MIN_VALUE, true},
                    {MAX_VALUE, "", MAX_VALUE, false},
                    {MAX_VALUE, null, MAX_VALUE, false},
                    {MAX_VALUE, "aaa", MAX_VALUE, false},
                    {BigDecimal.valueOf(1), "1", MIN_VALUE, true},
                    {BigDecimal.valueOf(-1), "-1", MAX_VALUE, false},
                    {BigDecimal.valueOf(2), "1.055", MAX_VALUE, false},
                    {BigDecimal.valueOf(1.55), "1.55", MAX_VALUE, false},

                    {BigDecimal.valueOf(900), "900", sampleMinValue, true},
                    {BigDecimal.valueOf(900), "900", sampleMaxValue, false},
                    {BigDecimal.valueOf(8000), "8000", sampleMaxValue, false}
            });
        }

        @Test
        public void testParseResultMatchExpected() {
            final BigDecimal result = NumericFilterViewAdapter.parse(parseStr, defaultValue, isMin);
            assertThat(result, equalTo(resultExpected));
        }

    }
}