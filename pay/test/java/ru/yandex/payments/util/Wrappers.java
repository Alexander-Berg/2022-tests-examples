package ru.yandex.payments.util;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

import io.micronaut.core.annotation.Introspected;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import ru.yandex.payments.util.wrapper.BooleanWrapper;
import ru.yandex.payments.util.wrapper.IntWrapper;
import ru.yandex.payments.util.wrapper.LongWrapper;
import ru.yandex.payments.util.wrapper.ShortWrapper;
import ru.yandex.payments.util.wrapper.ValueWrapper;

@UtilityClass
class Wrappers {
    @Introspected
    @AllArgsConstructor
    public static class BooleanValue extends BooleanWrapper {
        @AssertTrue
        private final boolean value;

        @Override
        public boolean getValue() {
            return value;
        }
    }

    @Getter
    @Introspected
    @AllArgsConstructor
    public static class ShortValue extends ShortWrapper {
        @Positive
        private final short value;
    }

    @Getter
    @Introspected
    @AllArgsConstructor
    public static class IntValue extends IntWrapper {
        @Positive
        private final int value;
    }

    @Getter
    @Introspected
    @AllArgsConstructor
    public static class LongValue extends LongWrapper {
        @Positive
        private final long value;
    }

    @Getter
    @Introspected
    @AllArgsConstructor
    public static class StringValue extends ValueWrapper<String> {
        @NotBlank
        private final String value;
    }
}
