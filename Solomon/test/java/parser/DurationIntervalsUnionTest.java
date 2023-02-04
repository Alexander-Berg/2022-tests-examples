package ru.yandex.solomon.expression.parser;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Ivan Tsybulin
 */
public class DurationIntervalsUnionTest {

    private final static long HOUR_MILLIS = 3600_000L;

    @Test(expected=IllegalArgumentException.class)
    public void parseEmpty() {
        DurationIntervalsUnion.parse("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void parseInvalid1() {
        DurationIntervalsUnion.parse("[12h-]");
    }

    @Test(expected=IllegalArgumentException.class)
    public void parseInvalid2() {
        DurationIntervalsUnion.parse("[0h-1h][2h-3h]");
    }

    @Test(expected=IllegalArgumentException.class)
    public void parseInvalid3() {
        DurationIntervalsUnion.parse("[0h-2h] + [1h-3h]");
    }

    @Test(expected=IllegalArgumentException.class)
    public void parseInvalid4() {
        DurationIntervalsUnion.parse("[20h-8h]");
    }

    @Test
    public void parseSingle() {
        DurationIntervalsUnion diu = DurationIntervalsUnion.parse("[12h-18h30m]");

        assertThat(diu.contains(42), equalTo(false));
        assertThat(diu.contains(12 * HOUR_MILLIS - 1), equalTo(false));
        assertThat(diu.contains(12 * HOUR_MILLIS), equalTo(true));
        assertThat(diu.contains(15 * HOUR_MILLIS), equalTo(true));
        assertThat(diu.contains(18 * HOUR_MILLIS + HOUR_MILLIS / 2), equalTo(true));
        assertThat(diu.contains(18 * HOUR_MILLIS + HOUR_MILLIS / 2 + 1), equalTo(false));
    }

    @Test
    public void parseMulti() {
        DurationIntervalsUnion diu = DurationIntervalsUnion.parse("[12h-14h] + [20h-24h]");

        assertThat(diu.contains(11 * HOUR_MILLIS), equalTo(false));
        assertThat(diu.contains(13 * HOUR_MILLIS), equalTo(true));
        assertThat(diu.contains(15 * HOUR_MILLIS), equalTo(false));
        assertThat(diu.contains(21 * HOUR_MILLIS), equalTo(true));
        assertThat(diu.contains(24 * HOUR_MILLIS), equalTo(true));
    }

}
