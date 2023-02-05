package ru.yandex.market.test.matchers;

import com.annimon.stream.Exceptional;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import androidx.annotation.NonNull;

import static org.hamcrest.Matchers.nullValue;

public class ExceptionalMatchers {

    @NonNull
    public static <T> Matcher<Exceptional<T>> hasValueThat(
            @NonNull final Matcher<? super T> matcher) {

        return new HasValueMatcher<>(matcher);
    }

    @NonNull
    public static <T> Matcher<Exceptional<T>> containsNull() {
        return new HasValueMatcher<>(nullValue());
    }

    @NonNull
    public static Matcher<Exceptional<?>> containsError() {
        return new IsError();
    }

    @NonNull
    public static <T> Matcher<Exceptional<T>> containsErrorThat(@NonNull final Matcher<Throwable> errorMatcher) {
        return new HasErrorThat<>(errorMatcher);
    }

    private ExceptionalMatchers() {
        throw new AssertionError("Instances are not allowed!");
    }

    static class IsError extends TypeSafeDiagnosingMatcher<Exceptional<?>> {

        @Override
        protected boolean matchesSafely(
                @NonNull final Exceptional<?> exceptional,
                @NonNull final Description mismatchDescription) {

            mismatchDescription.appendText("Exceptional not contains error");
            return !exceptional.isPresent();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Exceptional error should be present");
        }
    }

    static class HasValueMatcher<T> extends TypeSafeDiagnosingMatcher<Exceptional<T>> {

        @NonNull
        private final Matcher<? super T> matcher;

        HasValueMatcher(@NonNull final Matcher<? super T> matcher) {
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(
                @NonNull final Exceptional<T> exceptional,
                @NonNull final Description mismatchDescription) {

            if (!exceptional.isPresent()) {
                final Throwable error = exceptional.getException();
                if (error == null) {
                    mismatchDescription.appendText("value is null");
                } else {
                    mismatchDescription.appendText("was error ").appendValue(error);
                }
                return false;
            }
            final T value = exceptional.get();
            mismatchDescription.appendText("Exceptional value ");
            matcher.describeMismatch(value, mismatchDescription);
            return matcher.matches(value);
        }

        @Override
        public void describeTo(@NonNull final Description description) {
            description.appendText("Exceptional value ").appendDescriptionOf(matcher);
        }
    }

    static class HasErrorThat<T> extends TypeSafeDiagnosingMatcher<Exceptional<T>> {

        @NonNull
        private final Matcher<Throwable> errorMatcher;

        HasErrorThat(@NonNull final Matcher<Throwable> errorMatcher) {
            this.errorMatcher = errorMatcher;
        }

        @Override
        protected boolean matchesSafely(
                @NonNull final Exceptional<T> exceptional,
                @NonNull final Description mismatchDescription) {

            final Throwable exception = exceptional.getException();
            if (exception == null) {
                mismatchDescription.appendText("Exceptional not contains error");
                return false;
            } else {
                mismatchDescription.appendText("Exceptional error ");
                errorMatcher.describeMismatch(exception, mismatchDescription);
                return errorMatcher.matches(exception);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Exceptional error ").appendDescriptionOf(errorMatcher);
        }
    }
}
