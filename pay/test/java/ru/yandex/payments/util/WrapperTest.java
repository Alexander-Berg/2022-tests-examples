package ru.yandex.payments.util;

import java.util.stream.Stream;

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

class WrapperTest {
    @FunctionalInterface
    private interface ValueFactory<W, V> {
        W create(V value);
    }

    @FunctionalInterface
    private interface ValueComparator<W, V> {
        boolean isEqual(W wrapper, V value);
    }

    @FunctionalInterface
    private interface ValueExtractor<W, V> {
        V extract(W wrapper);
    }

    private static <W extends Wrapper, V> Arguments args(ValueFactory<W, V> factory,
                                                         ValueComparator<W, V> comparator,
                                                         ValueExtractor<W, V> extractor, V one, V two) {
        val tmp = factory.create(one);
        return Arguments.of(tmp.getClass().getSimpleName(), factory, comparator, extractor, one, two);
    }

    public static Stream<Arguments> inputArgs() {
        return Stream.of(
                args(BooleanValue::new, BooleanValue::isEqual, BooleanValue::getValue, true, false),
                args(ShortValue::new, ShortValue::isEqual, ShortValue::getValue, (short) 0, (short) 1),
                args(IntValue::new, IntValue::isEqual, IntValue::getValue, 0, 1),
                args(LongValue::new, LongValue::isEqual, LongValue::getValue, 0L, 1L),
                args(StringValue::new, StringValue::isEqual, StringValue::getValue, "0", "1")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputArgs")
    @DisplayName("Verify that wrapper types follow reference equality rules")
    void referenceEqualityTest(@SuppressWarnings("unused") String name,
                               ValueFactory<Wrapper, Object> factory,
                               @SuppressWarnings("unused") ValueComparator<Wrapper, Object> comparator,
                               @SuppressWarnings("unused") ValueExtractor<Wrapper, Object> extractor,
                               Object one,
                               Object two) {
        val oneValue = factory.create(one);
        val twoValue = factory.create(two);
        val likeOneValue = factory.create(one);

        assertThat(oneValue)
                .isNotSameAs(twoValue)
                .isNotSameAs(likeOneValue)
                .isNotSameAs(one)
                .isNotSameAs(two)
                .isSameAs(oneValue);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputArgs")
    @DisplayName("Verify that wrapper types follow value equivalence rules")
    void equivalenceTest(@SuppressWarnings("unused") String name,
                         ValueFactory<Wrapper, Object> factory,
                         ValueComparator<Wrapper, Object> comparator,
                         @SuppressWarnings("unused") ValueExtractor<Wrapper, Object> extractor,
                         Object one,
                         Object two) {
        val oneValue = factory.create(one);
        val twoValue = factory.create(two);
        val likeOneValue = factory.create(one);

        assertThat(oneValue)
                .isNotEqualTo(twoValue)
                .isEqualTo(likeOneValue)
                .isNotEqualTo(one)
                .isNotEqualTo(two)
                .isEqualTo(oneValue);

        assertThat(comparator.isEqual(oneValue, one))
                .isTrue();
        assertThat(comparator.isEqual(oneValue, two))
                .isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputArgs")
    @DisplayName("Verify that wrapper types follow hashCode rules")
    void hashCodeTest(@SuppressWarnings("unused") String name,
                      ValueFactory<Wrapper, Object> factory,
                      @SuppressWarnings("unused") ValueComparator<Wrapper, Object> comparator,
                      @SuppressWarnings("unused") ValueExtractor<Wrapper, Object> extractor,
                      Object one,
                      Object two) {
        val oneValue = factory.create(one);
        val twoValue = factory.create(two);
        val likeOneValue = factory.create(one);

        assertThat(oneValue.hashCode())
                .isNotEqualTo(twoValue.hashCode())
                .isEqualTo(likeOneValue.hashCode())
                .isEqualTo(one.hashCode())
                .isNotEqualTo(two.hashCode())
                .isEqualTo(oneValue.hashCode());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputArgs")
    @DisplayName("Verify that wrapper types follow toString rules")
    void toStringTest(@SuppressWarnings("unused") String name,
                      ValueFactory<Wrapper, Object> factory,
                      @SuppressWarnings("unused") ValueComparator<Wrapper, Object> comparator,
                      @SuppressWarnings("unused") ValueExtractor<Wrapper, Object> extractor,
                      Object one,
                      Object two) {
        val oneValue = factory.create(one);
        val twoValue = factory.create(two);
        val likeOneValue = factory.create(one);

        assertThat(oneValue.toString())
                .isNotEqualTo(twoValue.toString())
                .isEqualTo(likeOneValue.toString())
                .isEqualTo(one.toString())
                .isNotEqualTo(two.toString())
                .isEqualTo(oneValue.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputArgs")
    @DisplayName("Verify that wrapper types getValue returns expected value")
    void getValueTest(@SuppressWarnings("unused") String name,
                      ValueFactory<Wrapper, Object> factory,
                      @SuppressWarnings("unused") ValueComparator<Wrapper, Object> comparator,
                      ValueExtractor<Wrapper, Object> extractor,
                      Object one,
                      Object two) {
        val oneValue = factory.create(one);
        assertThat(extractor.extract(oneValue))
                .isEqualTo(one)
                .isNotEqualTo(two);
    }
}
