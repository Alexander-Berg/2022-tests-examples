package ru.yandex.realty.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

public class WaitForMatcherDecorator<T> extends TypeSafeMatcher<T> {

    private static final long DEFAULT_INTERVAL = SECONDS.toMillis(1);
    private static final long DEFAULT_TIMEOUT = SECONDS.toMillis(10);

    private Matcher<? super T> matcher;

    private long timeoutInMilliseconds;
    private long intervalInMilliseconds;

    private WaitForMatcherDecorator(Matcher<? super T> matcher,
                                    long timeoutInMilliseconds,
                                    long intervalInMilliseconds) {
        this.matcher = matcher;
        this.timeoutInMilliseconds = timeoutInMilliseconds;
        this.intervalInMilliseconds = intervalInMilliseconds;
    }

    @Factory
    public static <T> Matcher<? super T> withWaitFor(Matcher<? super T> matcher) {
        return withWaitFor(matcher, DEFAULT_TIMEOUT, DEFAULT_INTERVAL);
    }

    @Factory
    public static <T> Matcher<? super T> withWaitFor(Matcher<? super T> matcher, long timeoutInMilliseconds) {
        return withWaitFor(matcher, timeoutInMilliseconds, DEFAULT_INTERVAL);
    }

    @Factory
    public static <T> Matcher<T> withWaitFor(Matcher<T> matcher,
                                             long timeoutInMilliseconds,
                                             long intervalInMilliseconds) {
        return new WaitForMatcherDecorator<>(matcher, timeoutInMilliseconds, intervalInMilliseconds);
    }

    @Override
    protected boolean matchesSafely(T item) {
        long start = System.currentTimeMillis();
        long end = start + timeoutInMilliseconds;
        while (System.currentTimeMillis() < end) {
            if (matcher.matches(item)) {
                return true;
            }
            try {
                Thread.sleep(intervalInMilliseconds);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return matcher.matches(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("После ожидания [").appendValue(formatDurationWords(timeoutInMilliseconds, true, true))
                .appendText("]: ")
                .appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(T item, Description mismatchDescription) {
        matcher.describeMismatch(item, mismatchDescription);
    }
}
