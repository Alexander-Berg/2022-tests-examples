package ru.yandex.payments.util;

import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.payments.util.Wrappers.BooleanValue;
import ru.yandex.payments.util.Wrappers.IntValue;
import ru.yandex.payments.util.Wrappers.LongValue;
import ru.yandex.payments.util.Wrappers.ShortValue;
import ru.yandex.payments.util.Wrappers.StringValue;
import ru.yandex.payments.util.wrapper.Wrapper;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class ValidationTest {
    @Inject
    Validator validator;

    private static Arguments arg(Wrapper value) {
        return Arguments.of(value.getClass().getSimpleName(), value);
    }

    public static Stream<Arguments> successArgs() {
        return Stream.of(
                arg(new BooleanValue(true)),
                arg(new ShortValue((short) 10)),
                arg(new IntValue(20)),
                arg(new LongValue(30L)),
                arg(new StringValue("abc"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("successArgs")
    @DisplayName("Verify that valid value of wrapper-type passes validation")
    void successValidationTest(@SuppressWarnings("unused") String typeName, Wrapper value) {
        val violations = validator.validate(value);
        assertThat(violations)
                .isEmpty();
    }

    public static Stream<Arguments> failArgs() {
        return Stream.of(
                arg(new BooleanValue(false)),
                arg(new ShortValue((short) -10)),
                arg(new IntValue(-20)),
                arg(new LongValue(-30L)),
                arg(new StringValue(""))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failArgs")
    @DisplayName("Verify that invalid value of wrapper-type doesn't pass validation")
    void failValidationTest(@SuppressWarnings("unused") String typeName, Wrapper value) {
        val violations = validator.validate(value);
        assertThat(violations)
                .isNotEmpty();
    }
}
