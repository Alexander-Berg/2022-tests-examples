package ru.yandex.intranet.d.utils;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

/**
 * TestUtil.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 14-12-2021
 */
public class TestUtil {
    private TestUtil() {
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> void assertPresent(Optional<T> actual, Consumer<? super T> action) {
        Assertions.assertTrue(actual.isPresent());
        actual.ifPresent(action);
    }
}
