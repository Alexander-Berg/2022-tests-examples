package ru.yandex.solomon.expression.parser;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.solomon.expression.parser.DurationParser.formatDuration;
import static ru.yandex.solomon.expression.parser.DurationParser.parseDuration;

/**
 * @author Vladimir Gordiychuk
 */
public class DurationParserTest {

    private static final String[] UNIT_NAMES = new String[] { "ms", "s", "m", "h", "d", "w" };

    private static final long[] UNIT_MILLIS = new long[] {
        ChronoUnit.MILLIS.getDuration().toMillis(),
        ChronoUnit.SECONDS.getDuration().toMillis(),
        ChronoUnit.MINUTES.getDuration().toMillis(),
        ChronoUnit.HOURS.getDuration().toMillis(),
        ChronoUnit.DAYS.getDuration().toMillis(),
        ChronoUnit.WEEKS.getDuration().toMillis(),
    };

    @Test
    public void parseMilliseconds() throws Exception {
        assertThat(parseDuration("-0ms"), equalTo(Duration.ofMillis(0)));
        assertThat(parseDuration("0ms"), equalTo(Duration.ofMillis(0)));
        assertThat(parseDuration("1ms"), equalTo(Duration.ofMillis(1)));
        assertThat(parseDuration("1000ms"), equalTo(Duration.ofMillis(1000)));
        assertThat(parseDuration("-1000ms"), equalTo(Duration.ofMillis(-1000)));
        assertThat(parseDuration("10ms10s"), equalTo(Duration.ofMillis(10010)));
    }

    @Test
    public void parseSeconds() throws Exception {
        assertThat(parseDuration("0s"), equalTo(Duration.ofSeconds(0)));
        assertThat(parseDuration("15s"), equalTo(Duration.ofSeconds(15)));
        assertThat(parseDuration("13s"), equalTo(Duration.ofSeconds(13)));
        assertThat(parseDuration("180s"), equalTo(Duration.ofSeconds(180)));
    }

    @Test
    public void parseMinutes() throws Exception {
        assertThat(parseDuration("0m"), equalTo(Duration.ofMinutes(0)));
        assertThat(parseDuration("51m"), equalTo(Duration.ofMinutes(51)));
        assertThat(parseDuration("61m"), equalTo(Duration.ofMinutes(61)));
        assertThat(parseDuration("1m60s"), equalTo(Duration.ofMinutes(2)));
        assertThat(parseDuration("1m50s"), equalTo(Duration.ofSeconds(60 + 50)));
    }

    @Test
    public void parseHours() throws Exception {
        assertThat(parseDuration("0h"), equalTo(Duration.ofHours(0)));
        assertThat(parseDuration("5h"), equalTo(Duration.ofHours(5)));
        assertThat(parseDuration("24h"), equalTo(Duration.ofHours(24)));
        assertThat(parseDuration("5h60m"), equalTo(Duration.ofHours(6)));
        assertThat(parseDuration("1h10m30s"), equalTo(
            Duration.ofSeconds(
                TimeUnit.HOURS.toSeconds(1) + TimeUnit.MINUTES.toSeconds(10) + 30)
        ));
    }

    @Test
    public void parseDays() throws Exception {
        assertThat(parseDuration("0d"), equalTo(Duration.ofDays(0)));
        assertThat(parseDuration("3d"), equalTo(Duration.ofDays(3)));
        assertThat(parseDuration("360d"), equalTo(Duration.ofDays(360)));
        assertThat(parseDuration("1d24h"), equalTo(Duration.ofDays(2)));
        assertThat(parseDuration("2d3h5m35s"), equalTo(Duration.ofSeconds(
            TimeUnit.DAYS.toSeconds(2)
                + TimeUnit.HOURS.toSeconds(3)
                + TimeUnit.MINUTES.toSeconds(5)
                + 35
        )));
    }

    @Test
    public void parseWeek() throws Exception {
        assertThat(parseDuration("0w"), equalTo(Duration.ZERO));
        assertThat(parseDuration("1w"), equalTo(Duration.ofDays(7)));
        assertThat(parseDuration("5w"), equalTo(Duration.ofDays(5 * 7)));
        assertThat(parseDuration("1w3d"), equalTo(Duration.ofDays(10)));
        assertThat(parseDuration("1w2d3h5m35s"), equalTo(Duration.ofSeconds(
            TimeUnit.DAYS.toSeconds(7 + 2)
                + TimeUnit.HOURS.toSeconds(3)
                + TimeUnit.MINUTES.toSeconds(5)
                + 35
        )));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseEmptyDuration() throws Exception {
        parseDuration("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseNullDuration() throws IllegalArgumentException {
        parseDuration(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseNotValidDuration() throws IllegalArgumentException {
        parseDuration("something 2d");
    }

    @Test
    public void formatMilliseconds() throws Exception {
        assertThat(formatDuration(Duration.ofMillis(0)), equalTo("0s"));
        assertThat(formatDuration(Duration.ofMillis(1)), equalTo("1ms"));
        assertThat(formatDuration(Duration.ofMillis(100)), equalTo("100ms"));
        assertThat(formatDuration(Duration.ofMillis(1001)), equalTo("1s1ms"));
        assertThat(formatDuration(Duration.ofMillis(-100)), equalTo("-100ms"));
    }

    @Test
    public void formatSeconds() throws Exception {
        assertThat(formatDuration(Duration.ofSeconds(0)), equalTo("0s"));
        assertThat(formatDuration(Duration.ofSeconds(13)), equalTo("13s"));
        assertThat(formatDuration(Duration.ofSeconds(33)), equalTo("33s"));
        assertThat(formatDuration(Duration.ofSeconds(-50)), equalTo("-50s"));
    }

    @Test
    public void formatMinutes() throws Exception {
        assertThat(formatDuration(Duration.ofMinutes(0)), equalTo("0s"));
        assertThat(formatDuration(Duration.ofMinutes(1)), equalTo("1m"));
        assertThat(formatDuration(Duration.ofMinutes(21)), equalTo("21m"));
        assertThat(formatDuration(Duration.ofMinutes(-10)), equalTo("-10m"));
        assertThat(formatDuration(Duration.ofSeconds(360)), equalTo("6m"));
        assertThat(formatDuration(Duration.ofSeconds(370)), equalTo("6m10s"));
    }

    @Test
    public void formatHours() throws Exception {
        assertThat(formatDuration(Duration.ofHours(0)), equalTo("0s"));
        assertThat(formatDuration(Duration.ofMinutes(60)), equalTo("1h"));
        assertThat(formatDuration(Duration.ofHours(12)), equalTo("12h"));
        assertThat(formatDuration(Duration.ofHours(-13)), equalTo("-13h"));
        assertThat(formatDuration(Duration.ofMinutes(65)), equalTo("1h5m"));
        assertThat(formatDuration(Duration.ofSeconds(
            TimeUnit.HOURS.toSeconds(2) + TimeUnit.MINUTES.toSeconds(5) + 15)),
            equalTo("2h5m15s")
        );
    }

    @Test
    public void formatDays() throws Exception {
        assertThat(formatDuration(Duration.ofDays(0)), equalTo("0s"));
        assertThat(formatDuration(Duration.ofDays(1)), equalTo("1d"));
        assertThat(formatDuration(Duration.ofDays(-3)), equalTo("-3d"));
        assertThat(formatDuration(Duration.ofHours(48)), equalTo("2d"));
        assertThat(formatDuration(Duration.ofHours(51)), equalTo("2d3h"));
    }

    @Test
    public void formatWeeks() throws Exception {
        assertThat(formatDuration(Duration.ofDays(7)), equalTo("1w"));
        assertThat(formatDuration(Duration.ofDays(9)), equalTo("1w2d"));
    }

    @Test
    public void randomFormatParse() throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int index = 0; index < 1000; index++) {
            long seconds = random.nextLong(0, TimeUnit.DAYS.toMillis(360 * 1000 * 2));
            Duration expected = Duration.ofSeconds(seconds);
            String formatted = formatDuration(expected);
            Duration parsed = parseDuration(formatted);

            assertThat("Formatted duration - " + formatted, parsed, equalTo(expected));
        }
    }

    @Test
    public void randomParseFormat() throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int index = 0; index < 1000; index++) {
            int partCount = random.nextInt(1, 5);

            StringBuilder sb = new StringBuilder();
            long expectedMillis = 0;

            for (int partIndex = 0; partIndex < partCount; partIndex++) {
                Pair<String, Long> pair = generateDurationPart(random);

                String partFormatted = pair.getLeft();
                long partMillis = pair.getRight();

                sb.append(partFormatted);
                expectedMillis += partMillis;
            }

            String formatted = sb.toString();
            Duration parsed = parseDuration(formatted);

            assertThat(formatted, parsed.toMillis(), equalTo(expectedMillis));
        }
    }

    private static Pair<String, Long> generateDurationPart(ThreadLocalRandom random) {
        int partNum = random.nextInt(10000);

        int unitIndex = random.nextInt(UNIT_NAMES.length);
        String partUnit = UNIT_NAMES[unitIndex];
        long partUnitMillis = UNIT_MILLIS[unitIndex];

        String part = partNum + partUnit;
        long partMillis = partNum * partUnitMillis;

        return Pair.of(part, partMillis);
    }
}
