package ru.yandex.payments.util;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyTest {
    @Test
    @DisplayName("Verify that Lazy compute value only once")
    void testInitOnce() {
        val lazy = new Lazy<String>();

        assertThat(lazy.getOrCompute(() -> "a"))
            .isEqualTo("a");
        assertThat(lazy.getOrCompute(() -> "b"))
            .isEqualTo("a");
    }
}
