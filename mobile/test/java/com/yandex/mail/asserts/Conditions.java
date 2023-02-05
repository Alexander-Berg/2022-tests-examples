package com.yandex.mail.asserts;

import com.yandex.mail.util.Mapper;

import org.assertj.core.api.Condition;

import androidx.annotation.NonNull;

public final class Conditions {

    private Conditions() {
        throw new IllegalStateException("No instances please");
    }

    /**
     * Converts predicate to {@link Condition} to assert on. <br>
     *
     * Example of usage: <br>
     *
     * assertThat(whatever).is(matching(predicate)) <br>
     * assertThat(whatevers).are(matching(predicate))
     */
    @NonNull
    public static <T> Condition<T> matching(@NonNull Mapper<? super T, Boolean> predicate) {
        return new Condition<T>() {
            @Override
            public boolean matches(T value) {
                return predicate.map(value);
            }
        };
    }
}
