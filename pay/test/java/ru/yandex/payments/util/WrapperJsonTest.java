package ru.yandex.payments.util;

import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.SneakyThrows;
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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class WrapperJsonTest {
    @Inject
    ObjectMapper objectMapper;

    private static <T extends Wrapper> Arguments arg(T value, String expectedJson) {
        return Arguments.of(value.getClass().getSimpleName(), value, expectedJson);
    }

    public static Stream<Arguments> inputArgs() {
        return Stream.of(
                arg(new BooleanValue(true), "true"),
                arg(new ShortValue((short) 10), "10"),
                arg(new IntValue(20), "20"),
                arg(new LongValue(30), "30"),
                arg(new StringValue("abc"), "\"abc\"")
        );
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @MethodSource("inputArgs")
    @DisplayName("Verify that value wrapper serializes and deserializes just like a wrapped value")
    void jsonTest(@SuppressWarnings("unused") String name, Wrapper wrapperValue, String expectedJsonValue) {
        val json = objectMapper.writeValueAsString(wrapperValue);
        assertThatJson(json)
                .isEqualTo(expectedJsonValue);

        val value = objectMapper.readValue(json, wrapperValue.getClass());
        assertThat(value)
                .isEqualTo(wrapperValue);
    }
}
