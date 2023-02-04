package ru.yandex.payments.util.tskv;

import java.text.ParseException;
import java.util.Map;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.payments.util.tskv.TskvParser.parseLine;
import static ru.yandex.payments.util.tskv.TskvParser.parseLines;

class TskvParserTest {
    @Test
    @DisplayName("Verify that tskv parser could parse single key value tskv record")
    void testLegalSingleKv() {
        assertThat(parseLine("tskv\tkey=value"))
                .containsOnly(
                        entry("key", "value")
                );
    }

    @Test
    @DisplayName("Verify that tskv parser could parse multiple key value tskv record")
    void testLegalMultiKv() {
        assertThat(parseLine("tskv\tkey=value\tk2=v2\tk=v"))
                .containsOnly(
                        entry("key", "value"),
                        entry("k2", "v2"),
                        entry("k", "v")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tskv\t=value",
            "tskv\t   =value"
    })
    @DisplayName("Verify that tskv parser fails on records containing blank keys")
    void testEmptyKey(String illegalInput) {
        assertThatThrownBy(() -> parseLine(illegalInput))
                .isInstanceOf(ParseException.class);
    }

    @Test
    @DisplayName("Verify that tskv parser could parse records containing empty values")
    void testEmptyValue() {
        assertThat(parseLine("tskv\tkey=\tk2=v2\tk="))
                .containsOnly(
                        entry("key", ""),
                        entry("k2", "v2"),
                        entry("k", "")
                );
    }

    @Test
    @DisplayName("Verify that tskv parser fails on empty input string")
    void testEmptyInput() {
        assertThatThrownBy(() -> parseLine(""))
                .isExactlyInstanceOf(ParseException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "k=v\tk2=v2",
            "tskvk=v\tk2=v2",
            "\tk=v\tk2=v2"
    })
    @DisplayName("Verify that tskv parser fails on input string with no tskv header")
    void testHeaderLessInput(String illegalInput) {
        assertThatThrownBy(() -> parseLine(illegalInput))
                .isExactlyInstanceOf(ParseException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tskv\t",
            "tskv\tkey",
            "tskv\tkey=value\t"
    })
    @DisplayName("Verify that tskv parser fails on illegal input")
    void testIllegalInput(String illegalInput) {
        assertThatThrownBy(() -> parseLine(illegalInput))
                .isExactlyInstanceOf(ParseException.class);
    }

    @Test
    @DisplayName("Verify that tskv parser could parse multiline tskv string")
    void testMultipleRecords() {
        val input = """
                tskv\tk=v

                tskv\tkey=value\tk1=22

                tskv\tflag=true\tk=null
                """;
        assertThat(parseLines(input))
                .containsExactly(
                        Map.of(
                                "k", "v"
                        ),
                        Map.of(
                                "key", "value",
                                "k1", "22"
                        ),
                        Map.of(
                                "flag", "true",
                                "k", "null"
                        )
                );
    }
}
