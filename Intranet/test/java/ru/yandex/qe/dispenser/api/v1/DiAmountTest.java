package ru.yandex.qe.dispenser.api.v1;

import java.io.IOException;
import java.text.DecimalFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class DiAmountTest {
    private static final double EPS = 1e-6;

    @Test
    public void bytesCanBeConvertedToHumanReadable() {
        final DiAmount.Humanized humanized = DiAmount.of(296_000_000_000_000L, DiUnit.BYTE).humanize();
        Assertions.assertEquals(DiAmount.of(269, DiUnit.TEBIBYTE), humanized);
        Assertions.assertEquals("TiB", humanized.getAbbreviation());
        Assertions.assertEquals(new DecimalFormat("#.##").format(269.21d) + " TiB", humanized.toString());
    }

    @Test
    public void countsCanBeConvertedToHumanReadable() {
        final DiAmount humanized = DiAmount.of(17_000_000, DiUnit.COUNT).humanize();
        Assertions.assertEquals(DiAmount.of(17, DiUnit.MEGA), humanized);
        Assertions.assertEquals("M units", humanized.getAbbreviation());
        Assertions.assertEquals("17 M units", humanized.toString());
    }

    @Test
    public void bpsCanBeConvertedToHumanReadable() {
        final DiAmount humanized = DiAmount.of(7_000_000, DiUnit.BPS).humanize();
        Assertions.assertEquals(DiAmount.of(7, DiUnit.MBPS), humanized);
        Assertions.assertEquals("MBps", humanized.getAbbreviation());
        Assertions.assertEquals("7 MBps", humanized.toString());
    }

    @Test
    public void binaryBpsCanBeConvertedToHumanReadable() {
        final DiAmount humanized = DiAmount.of(7_340_032, DiUnit.BINARY_BPS).humanize();
        Assertions.assertEquals(DiAmount.of(7, DiUnit.MIBPS), humanized);
        Assertions.assertEquals("MiBps", humanized.getAbbreviation());
        Assertions.assertEquals("7 MiBps", humanized.toString());
    }

    @Test
    public void valueOfZeroIsHumanReadable() {
        valueIsHumanReadable(0);
    }

    @Test
    public void valueOfOneIsHumanReadable() {
        valueIsHumanReadable(1);
    }

    @Test
    public void valueOfThousandIsHumanReadable() {
        valueIsHumanReadable(1000);
    }

    void valueIsHumanReadable(final long value) {
        for (final DiUnit unit : DiUnit.values()) {
            if (unit.canBeUsedForHumanReadable()) {
                final DiAmount one = DiAmount.of(value, unit);
                Assertions.assertEquals(one.humanize(), one);
            }
        }
    }

    @Test
    public void maxHumanReadableBytes() {
        Assertions.assertEquals(DiAmount.of(1024, DiUnit.BYTE).humanize(), DiAmount.of(1024, DiUnit.BYTE));
        Assertions.assertEquals(DiAmount.of(1025, DiUnit.BYTE).humanize(), DiAmount.of(1, DiUnit.KIBIBYTE));
    }

    @Test
    public void humanReadableOfUnitWithoutEnsembleIsHimself() throws Exception {
        final DiAmount currency = DiAmount.of(1_000_000, DiUnit.CURRENCY);
        Assertions.assertEquals(currency, currency.humanize());
    }

    @Test
    public void humanReadablePermilleIsCountOrLarger() {
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(100, DiUnit.PERMILLE).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(1000, DiUnit.PERMILLE).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(10000, DiUnit.PERMILLE).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(100000, DiUnit.PERMILLE).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(1000000, DiUnit.PERMILLE).humanize().getUnit());
        Assertions.assertEquals(DiUnit.KILO, DiAmount.of(10000000, DiUnit.PERMILLE).humanize().getUnit());
    }

    @Test
    public void humanReadablePercentIsCountOrLarger() {
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(10, DiUnit.PERCENT).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(100, DiUnit.PERCENT).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(1000, DiUnit.PERCENT).humanize().getUnit());
        Assertions.assertEquals(DiUnit.COUNT, DiAmount.of(100000, DiUnit.PERCENT).humanize().getUnit());
        Assertions.assertEquals(DiUnit.KILO, DiAmount.of(1000000, DiUnit.PERCENT).humanize().getUnit());
    }

    @Test
    public void humanReadableMustContainDoubleValue() {
        final DiAmount.Humanized humanized = DiAmount.of(5_999_999, DiUnit.BYTE).humanize();
        Assertions.assertEquals(5.722044, humanized.getDoubleValue(), EPS);
        Assertions.assertEquals(new DecimalFormat("#.##").format(5.72d), humanized.getStringValue());
        Assertions.assertEquals(DiUnit.MEBIBYTE, humanized.getUnit());
        Assertions.assertEquals("MiB", humanized.getAbbreviation());
    }

    @Test
    public void amountWithNegativeValueCannotBeCreated() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DiAmount.of(-1, DiUnit.BYTE);
        });
    }

    @Test
    public void humanReadableForProcessorIsCoresOrLarger() {
        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(10, DiUnit.PERMILLE_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(100, DiUnit.PERMILLE_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(1000, DiUnit.PERMILLE_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(100000, DiUnit.PERMILLE_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.KILO_CORES, DiAmount.of(10000000, DiUnit.PERMILLE_CORES).humanize().getUnit());

        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(10, DiUnit.PERCENT_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(100, DiUnit.PERCENT_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.CORES, DiAmount.of(1000, DiUnit.PERCENT_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.KILO_CORES, DiAmount.of(1000000, DiUnit.PERCENT_CORES).humanize().getUnit());
        Assertions.assertEquals(DiUnit.MEGA_CORES, DiAmount.of(1000000000, DiUnit.PERCENT_CORES).humanize().getUnit());
    }

    @Test
    public void testDeserialize() throws IOException {
        final ObjectMapper mapper = new ObjectMapper()
                .registerModules(new KotlinModule.Builder().build(), new Jdk8Module(), new JavaTimeModule());
        final ObjectReader reader = mapper.reader().forType(DiAmount.class);
        final DiAmount diAmountOne = reader.readValue("{\"value\": 1, \"unit\":\"COUNT\"}");
        Assertions.assertEquals(1, diAmountOne.getValue());
        Assertions.assertEquals(DiUnit.COUNT, diAmountOne.getUnit());
        final DiAmount diAmountTwo = reader.readValue("{\"value\": 1.5, \"unit\":\"COUNT\"}");
        Assertions.assertEquals(150, diAmountTwo.getValue());
        Assertions.assertEquals(DiUnit.PERCENT, diAmountTwo.getUnit());
    }

}
