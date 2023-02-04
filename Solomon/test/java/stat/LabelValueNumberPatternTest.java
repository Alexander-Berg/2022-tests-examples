package ru.yandex.solomon.math.stat;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.histogram.Histograms;


/**
 * @author Maksim Leonov
 */
public class LabelValueNumberPatternTest {
    @Test
    public void basicTest() {
        double a = LabelValueNumberPattern.parse("<500ms");
        Assert.assertEquals(500, a, 0);
    }

    @Test
    public void testMultipleNumbers() {
        double a = LabelValueNumberPattern.parse("host12_56apples");
        double b = LabelValueNumberPattern.parse("host34_78apples");
        Assert.assertEquals(56L, a, 0);
        Assert.assertEquals(78L, b, 0);
        // a and b have similar patterns
    }

    @Test
    public void testDoubleNumbers() {
        double a = LabelValueNumberPattern.parse("500.0");
        Assert.assertEquals(500L, a, 0);
    }

    @Test(expected = NumberFormatException.class)
    public void testNoNumbers() {
        LabelValueNumberPattern.parse("host_no_apples");
    }

    @Test
    public void testInf() {
        for (String inf : new String[]{"inf", "Inf", "iNf", "inF", "INf", "iNF", "INF"}) {
            double a = LabelValueNumberPattern.parse(inf);
            Assert.assertEquals(Histograms.INF_BOUND, a, 0);
        }
    }

    @Test
    public void testExponential() {
        double a = LabelValueNumberPattern.parse("-2.0E-4");
        Assert.assertEquals(-2e-4, a, 0);
    }
}
