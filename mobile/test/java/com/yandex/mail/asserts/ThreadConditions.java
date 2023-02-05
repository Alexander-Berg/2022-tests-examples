package com.yandex.mail.asserts;

import org.assertj.core.api.Condition;

import androidx.annotation.NonNull;

public final class ThreadConditions {

    private ThreadConditions() { }

    @NonNull
    public static Condition<Long> fake() {
        return new Condition<Long>() {
            @Override
            public boolean matches(Long threadId) {
                return threadId < 0;
            }
        };
    }
}
