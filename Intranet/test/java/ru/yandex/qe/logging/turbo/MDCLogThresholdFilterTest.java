package ru.yandex.qe.logging.turbo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.FilterReply;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author intr13
 */
public class MDCLogThresholdFilterTest {

    private void safeRun(String threshold, String pattern, Runnable runnable) {
        MDC.put(MDCLogThresholdFilter.MDC_THRESHOLD_KEY, threshold);
        if (pattern != null) {
            MDC.put(MDCLogThresholdFilter.MDC_PATTERN_KEY, pattern);
        }
        try {
            runnable.run();
        } finally {
            MDC.remove(MDCLogThresholdFilter.MDC_THRESHOLD_KEY);
            if (pattern != null) {
                MDC.remove(MDCLogThresholdFilter.MDC_PATTERN_KEY);
            }
        }
    }

    @Test
    public void stopped() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        FilterReply result = filter.decide(
                null,
                null,
                Level.INFO,
                "blah",
                new Object[0],
                null
        );
        assertThat(result, Matchers.is(FilterReply.NEUTRAL));
    }

    @Test
    public void invalid_pattern() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), "[a", () -> {
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("test"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.NEUTRAL));
        });
    }

    @Test
    public void accept_by_pattern() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), "test", () -> {
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("test"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.ACCEPT));
        });
    }

    @Test
    public void accept_by_pattern2() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), "test|blah", () -> {
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("blah"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.ACCEPT));
        });
    }

    @Test
    public void accept_by_patterns() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), "test1", () -> {
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("test1"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(filter.pattern.get().getPattern().pattern(), Matchers.is("test1"));
            assertThat(result, Matchers.is(FilterReply.ACCEPT));
        });
        safeRun(Level.DEBUG.toString(), "test2", () -> {
            assertThat(filter.pattern.get().getPattern().pattern(), Matchers.is("test1"));
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("test2"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(filter.pattern.get().getPattern().pattern(), Matchers.is("test2"));
            assertThat(result, Matchers.is(FilterReply.ACCEPT));
        });
    }

    @Test
    public void ignore_by_pattern() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), "test", () -> {
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("kva"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.NEUTRAL));
        });
    }

    @Test
    public void ignore_by_pattern2() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), "test|blah", () -> {
            FilterReply result = filter.decide(
                    null,
                    new LoggerContext().getLogger("kva"),
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.NEUTRAL));
        });
    }

    @Test
    public void accept_greatest() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun(Level.DEBUG.toString(), null, () -> {
            FilterReply result = filter.decide(
                    null,
                    null,
                    Level.INFO,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.ACCEPT));
        });
    }

    @Test
    public void accept_greatest_default() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.setDefaultThreshold(Level.INFO);
        filter.start();
        safeRun("BLAH", null, () -> {
            FilterReply result = filter.decide(
                    null,
                    null,
                    Level.ERROR,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.ACCEPT));
        });
    }

    @Test
    public void ignore_lowest() {
        MDCLogThresholdFilter filter = new MDCLogThresholdFilter();
        filter.start();
        safeRun("BLAH", null, () -> {
            FilterReply result = filter.decide(
                    null,
                    null,
                    Level.TRACE,
                    "blah",
                    new Object[0],
                    null
            );
            assertThat(result, Matchers.is(FilterReply.NEUTRAL));
        });
    }
}
