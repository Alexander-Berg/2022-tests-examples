package ru.yandex.qe.dispenser.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class TextUtilsTest {

    @Test
    public void emptyStringIsntUpperCase() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            TextUtils.requireUpperCase("", "");
        });
    }

}