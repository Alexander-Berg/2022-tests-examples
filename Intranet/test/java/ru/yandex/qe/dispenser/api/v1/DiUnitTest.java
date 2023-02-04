package ru.yandex.qe.dispenser.api.v1;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class DiUnitTest {
    @Test
    public void conversionOfCompatibleUnitsMustBeCorrect() {
        Assertions.assertEquals(2000, DiUnit.PERMILLE.convert(2, DiUnit.COUNT));
        Assertions.assertEquals(2, DiUnit.COUNT.convert(2123, DiUnit.PERMILLE));
        Assertions.assertEquals(29, DiUnit.COUNT.convert(2999, DiUnit.PERCENT));
        Assertions.assertEquals(2_097_152, DiUnit.KIBIBYTE.convert(2, DiUnit.GIBIBYTE));
        Assertions.assertEquals(2, DiUnit.KBPS.convert(2100, DiUnit.BPS));
        Assertions.assertEquals(5, DiUnit.MBPS.convert(5900000, DiUnit.BPS));
        Assertions.assertEquals(2, DiUnit.KIBPS.convert(2048, DiUnit.BINARY_BPS));
        Assertions.assertEquals(4, DiUnit.MIBPS.convert(5_242_879, DiUnit.BINARY_BPS));
    }

    @Test
    public void conversionMustBePossibleForLongValues() {
        Assertions.assertEquals(1_073_741_824_000_000_000L, DiUnit.BYTE.convert(1_000_000_000L, DiUnit.GIBIBYTE));
    }

    @Test
    public void canNotConvertIncompatibleUnits() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DiUnit.PERMILLE.convert(1, DiUnit.BYTE);
        });
    }

    @Test
    public void conversionToAmountMustBeCorrect() {
        Assertions.assertEquals(DiUnit.COUNT.convertToAmount(DiAmount.of(3000, DiUnit.PERMILLE)), DiAmount.of(3, DiUnit.COUNT));
    }

    @Test
    public void normalize() {
        Assertions.assertEquals(DiAmount.of(500, DiUnit.COUNT), DiUnit.KILO.normalize(0.5).get());
        Assertions.assertEquals(DiAmount.of(50, DiUnit.COUNT), DiUnit.KILO.normalize(0.05).get());
        Assertions.assertEquals(DiAmount.of(5, DiUnit.COUNT), DiUnit.KILO.normalize(0.005).get());
        Assertions.assertEquals(DiAmount.of(50, DiUnit.PERCENT), DiUnit.KILO.normalize(0.0005).get());
        Assertions.assertEquals(DiAmount.of(5, DiUnit.PERCENT), DiUnit.KILO.normalize(0.00005).get());
        Assertions.assertEquals(DiAmount.of(5, DiUnit.PERMILLE), DiUnit.KILO.normalize(0.000005).get());
        Assertions.assertEquals(DiAmount.of(0, DiUnit.PERMILLE), DiUnit.KILO.normalize(0.0000005).get());
        Assertions.assertEquals(DiAmount.of(1, DiUnit.PERMILLE), DiUnit.KILO.normalize(0.0000015).get());
        Assertions.assertFalse(DiUnit.COUNT.normalize(1E19).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(1E22).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(1E25).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(1E28).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(Double.POSITIVE_INFINITY).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(Double.NEGATIVE_INFINITY).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(Double.NaN).isPresent());
        Assertions.assertEquals(DiAmount.of(1, DiUnit.KILO), DiUnit.KILO.normalize(1.0).get());
        Assertions.assertEquals(DiAmount.of(1500, DiUnit.COUNT), DiUnit.KILO.normalize(1.5).get());
        Assertions.assertEquals(DiAmount.of(500, DiUnit.COUNT), DiUnit.KILO.normalize(BigDecimal.valueOf(0.5)).get());
        Assertions.assertEquals(DiAmount.of(50, DiUnit.COUNT), DiUnit.KILO.normalize(BigDecimal.valueOf(0.05)).get());
        Assertions.assertEquals(DiAmount.of(5, DiUnit.COUNT), DiUnit.KILO.normalize(BigDecimal.valueOf(0.005)).get());
        Assertions.assertEquals(DiAmount.of(50, DiUnit.PERCENT), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0005)).get());
        Assertions.assertEquals(DiAmount.of(5, DiUnit.PERCENT), DiUnit.KILO.normalize(BigDecimal.valueOf(0.00005)).get());
        Assertions.assertEquals(DiAmount.of(5, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.000005)).get());
        Assertions.assertEquals(DiAmount.of(0, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0000005)).get());
        Assertions.assertEquals(DiAmount.of(0, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0000001)).get());
        Assertions.assertEquals(DiAmount.of(0, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0000009)).get());
        Assertions.assertEquals(DiAmount.of(1, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0000015)).get());
        Assertions.assertEquals(DiAmount.of(1, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0000011)).get());
        Assertions.assertEquals(DiAmount.of(1, DiUnit.PERMILLE), DiUnit.KILO.normalize(BigDecimal.valueOf(0.0000019)).get());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E19)).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E22)).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E25)).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E28)).isPresent());
        Assertions.assertEquals(DiAmount.of(1, DiUnit.KILO), DiUnit.KILO.normalize(BigDecimal.valueOf(1.0)).get());
        Assertions.assertEquals(DiAmount.of(1500, DiUnit.COUNT), DiUnit.KILO.normalize(BigDecimal.valueOf(1.5)).get());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E19).toBigInteger()).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E22).toBigInteger()).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E25).toBigInteger()).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(new BigDecimal("10000000000000000000000000.5")).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(new BigDecimal("10000000000000000.123456789123456789")).isPresent());
        Assertions.assertFalse(DiUnit.COUNT.normalize(BigDecimal.valueOf(1E28).toBigInteger()).isPresent());
        Assertions.assertEquals(DiAmount.of(1L, DiUnit.COUNT), DiUnit.COUNT.normalize(BigInteger.valueOf(1)).get());
        Assertions.assertEquals(DiAmount.of(337, DiUnit.BYTE), DiUnit.KIBIBYTE.normalize(0.33).get());
        Assertions.assertEquals(DiAmount.of(1361, DiUnit.BYTE), DiUnit.KIBIBYTE.normalize(1.33).get());
        Assertions.assertEquals(DiAmount.of(1L, DiUnit.COUNT), DiUnit.COUNT.normalize(1L).get());
        Assertions.assertFalse(DiUnit.COUNT.normalize(Long.MAX_VALUE).isPresent());
    }

    @Test
    public void testLargestIntegerAmount() {
        Assertions.assertEquals(DiAmount.of(1L, DiUnit.GIBIBYTE),
                DiUnit.largestIntegerAmount(1024L * 1024L * 1024L, DiUnit.BYTE));
        Assertions.assertEquals(DiAmount.of(1L, DiUnit.BYTE),
                DiUnit.largestIntegerAmount(1L, DiUnit.BYTE));
        Assertions.assertEquals(DiAmount.of(1L, DiUnit.GIBIBYTE_BASE),
                DiUnit.largestIntegerAmount(1L, DiUnit.GIBIBYTE_BASE));
        Assertions.assertEquals(DiAmount.of(1L, DiUnit.GIBIBYTE),
                DiUnit.largestIntegerAmount(1L, DiUnit.GIBIBYTE));
        Assertions.assertEquals(DiAmount.of(1023L * 1023L * 1023L, DiUnit.BYTE),
                DiUnit.largestIntegerAmount(1023L * 1023L * 1023L, DiUnit.BYTE));
        Assertions.assertEquals(DiAmount.of(0L, DiUnit.GIBIBYTE),
                DiUnit.largestIntegerAmount(0L, DiUnit.BYTE));
        Assertions.assertEquals(DiAmount.of(0L, DiUnit.CORES),
                DiUnit.largestIntegerAmount(0L, DiUnit.PERMILLE_CORES));
    }

}
