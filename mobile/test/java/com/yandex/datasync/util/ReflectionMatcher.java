/*
 * Copyright © 2016 YANDEX. All rights reserved.
 */

package com.yandex.datasync.util;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class ReflectionMatcher<T> extends BaseMatcher {

    private final T expected;

    public ReflectionMatcher(@NonNull final T expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(final Object item) {
        return EqualsBuilder.reflectionEquals(expected, item);
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(expected.toString());
    }

    @Factory
    public static <T> Matcher<T> reflectionEqualTo(final T actual) {
        return new ReflectionMatcher(actual);
    }
}
