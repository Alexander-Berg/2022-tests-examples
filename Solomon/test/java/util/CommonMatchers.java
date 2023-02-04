package ru.yandex.solomon.alert.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * @author Vladimir Gordiychuk
 */
public final class CommonMatchers {
    private CommonMatchers() {
    }

    public static <T> Matcher<T> reflectionEqualTo(T expect) {
        return new TypeSafeDiagnosingMatcher<T>() {
            @Override
            protected boolean matchesSafely(T item, Description mismatchDescription) {
                mismatchDescription.appendValue(item);
                return EqualsBuilder.reflectionEquals(expect, item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("refEq(" + expect + ")");
            }
        };
    }
}
