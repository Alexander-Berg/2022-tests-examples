package ru.yandex.payments.util.tskv;

import java.nio.CharBuffer;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JustValue {
    @Override
    public String toString() {
        return "v1";
    }
}

class TskvBuilderTest {
    @Test
    @DisplayName("Verify that tskv builder builds correct tskv string")
    void testSingleRecord() {
        val builder = new TskvBuilder();
        val actual = builder.append("k1", new JustValue())
                .append("k2", "v2")
                .append("k3", CharBuffer.wrap("v3"))
                .append("k4", new char[]{'v', '4'})
                .append("k5", '5')
                .append("k6", true)
                .append("k7", (short) 7)
                .append("k8", 8)
                .append("k9", 9L)
                .append("k10", 10.f)
                .append("k11", 11.0d)
                .build();
        val expected = "tskv\tk1=v1\tk2=v2\tk3=v3\tk4=v4\tk5=5\tk6=true\tk7=7\tk8=8\tk9=9\tk10=10.0\tk11=11.0";

        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Verify that tskv builder builds correct multi-record tskv string")
    void testMultiRecord() {
        val builder = new TskvBuilder();
        val actual = builder.append("k1", 1)
                .append("k2", true)
                .startNewRecord()
                .append("k1", 2)
                .append("k2", false)
                .build();
        val expected = "tskv\tk1=1\tk2=true\ntskv\tk1=2\tk2=false";

        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Verify that tskv builder fails on empty record")
    void testEmptyRecord() {
        val builder = new TskvBuilder(5);
        assertThatThrownBy(builder::build)
                .isExactlyInstanceOf(IllegalStateException.class);

        builder.clear();

        assertThatThrownBy(() -> builder.startNewRecord().append("k", "v").build())
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "\t"
    })
    @DisplayName("Verify that tskv builder fails on blank key")
    void testBlankKey(String blankKey) {
        val builder = new TskvBuilder(42);
        assertThatThrownBy(() -> builder.append(blankKey, 1))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
