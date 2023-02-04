package ru.yandex.payments.util;

import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.core.convert.ConversionService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
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
class ConversionTest {
    @Inject
    ConversionService<?> converter;

    private static Arguments arg(Object source, Wrapper expectedResult) {
        // used to avoid parameter resolution failure
        // junit is not able to handle `Object` type parameters while another parameter resolver (micronaut) is present
        final Supplier<Object> sourceSupplier = () -> source;
        return Arguments.of(source.getClass(), expectedResult.getClass(), sourceSupplier, expectedResult);
    }

    public static Stream<Arguments> fromValueConversionArgs() {
        return Stream.of(
                arg(true, new BooleanValue(true)),
                arg("false", new BooleanValue(false)),

                arg((short) 10, new ShortValue((short) 10)),
                arg("20", new ShortValue((short) 20)),

                arg(30, new IntValue(30)),
                arg((short) 40, new IntValue(40)),
                arg("50", new IntValue(50)),

                arg(60L, new LongValue(60)),
                arg((short) 70, new LongValue(70)),
                arg(80, new LongValue(80)),
                arg("90", new LongValue(90)),

                arg("abcd", new StringValue("abcd"))
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("fromValueConversionArgs")
    @DisplayName("Verify that user-defined wrapper-type could be converted from a value of underlying type or string")
    void fromValueConversionTest(@SuppressWarnings("unused") Class<?> sourceType,
                                 Class<?> wrapperType,
                                 Supplier<Object> sourceSupplier,
                                 Wrapper expectedResult) {
        val result = converter.convertRequired(sourceSupplier.get(), wrapperType);
        assertThat(result)
                .isEqualTo(expectedResult);
    }

    private static Arguments arg(Wrapper source, Object expectedValue) {
        // used to avoid parameter resolution failure
        // junit is not able to handle `Object` type parameters while another parameter resolver (micronaut) is present
        final Supplier<Object> expectedResultSupplier = () -> expectedValue;
        return Arguments.of(source.getClass().getSimpleName(), expectedValue.getClass(), source,
                expectedResultSupplier);
    }

    public static Stream<Arguments> toValueConversionArgs() {
        return Stream.of(
                arg(new BooleanValue(true), true),

                arg(new ShortValue((short) 10), (short) 10),
                arg(new ShortValue((short) 20), 20),
                arg(new ShortValue((short) 30), 30L),

                arg(new IntValue(40), 40),
                arg(new IntValue(50), 50L),

                arg(new LongValue(60), 60L),

                arg(new StringValue("abcd"), "abcd")
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("toValueConversionArgs")
    @DisplayName("Verify that user-defined wrapper-type could be converted to underlying type")
    void toValueConversionTest(@SuppressWarnings("unused") String wrapperType,
                               Class<?> dstType,
                               Wrapper wrapper,
                               Supplier<Object> expectedResultSupplier) {
        val result = converter.convertRequired(wrapper, dstType);
        assertThat(result)
                .isEqualTo(expectedResultSupplier.get());
    }
}
