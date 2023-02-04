package ru.yandex.solomon.labels.query;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.shard.ShardKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Baryshnikov
 */
public class SelectorsTest {

    @Test
    public void parseEmpty() {
        Selectors actual = Selectors.parse("");
        Selectors expected = Selectors.of();
        assertEquals(expected, actual);
    }

    @Test
    public void parseAny() {
        Selectors actual = Selectors.parse("host=*");
        Selectors expected = Selectors.of(Selector.any("host"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseAnyNegative() {
        Selectors actual = Selectors.parse("host!=*");
        Selectors expected = Selectors.of(Selector.absent("host"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseAbsent() {
        Selectors actual = Selectors.parse("host!=-");
        Selectors expected = Selectors.of(Selector.any("host"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseSingleGlob() {
        Selectors actual = Selectors.parse("host=solomon-*");
        Selectors expected = Selectors.of(Selector.glob("host", "solomon-*"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseNotSingleGlob() {
        Selectors actual = Selectors.parse("host!=solomon-*");
        Selectors expected = Selectors.of(Selector.notGlob("host", "solomon-*"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseMultiGlob() {
        Selectors actual = Selectors.parse("host=solomon-*|cluster");
        Selectors expected = Selectors.of(Selector.glob("host", "solomon-*|cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseNotMultiGlob() {
        Selectors actual = Selectors.parse("host!=solomon-*|cluster");
        Selectors expected = Selectors.of(Selector.notGlob("host", "solomon-*|cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseExact() {
        Selectors actual = Selectors.parse("host==cluster");
        Selectors expected = Selectors.of(Selector.exact("host", "cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseExactNegative() {
        Selectors actual = Selectors.parse("host!==cluster");
        Selectors expected = Selectors.of(Selector.notExact("host", "cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseRegex() {
        Selectors actual = Selectors.parse("host=~cluster");
        Selectors expected = Selectors.of(Selector.regex("host", "cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseRegexNegative() {
        Selectors actual = Selectors.parse("host!~cluster");
        Selectors expected = Selectors.of(Selector.notRegex("host", "cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseWithWhitespaces() {
        Selectors actual = Selectors.parse(" host = cluster  ");
        Selectors expected = Selectors.of(Selector.glob("host", "cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseWithDoubleQuotes() {
        Selectors actual = Selectors.parse("host=\"cluster\"");
        Selectors expected = Selectors.of(Selector.glob("host", "cluster"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseWithSingleQuotes() {
        Selectors actual = Selectors.parse("host='total Man'");
        Selectors expected = Selectors.of(Selector.glob("host", "total Man"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseWithQuotedKey() {
        Selectors actual = Selectors.parse("\"stream-name\"=\"stream-name\"");
        Selectors expected = Selectors.of(Selector.glob("stream-name", "stream-name"));
        assertEquals(expected, actual);
    }

    @Test
    public void parseSeveralSelectors() {
        Selectors actual =
                Selectors.parse("cluster=proxy, service=frontend, host=\"summary of hosts, except Man\"");

        Selectors expected = Selectors.builder(3)
                .add(Selector.glob("cluster", "proxy"))
                .add(Selector.glob("service", "frontend"))
                .add(Selector.glob("host", "summary of hosts, except Man"))
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void parseEqualSelectors() {
        Selectors actual =
                Selectors.parse("cluster=proxy, service!~fetcher, service==frontend");
        Selectors expected = Selectors.builder(3)
                .add(Selector.glob("cluster", "proxy"))
                .add(Selector.notRegex("service", "fetcher"))
                .add(Selector.exact("service", "frontend"))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void parseSelectorsWithQuotes() {
        Selectors actual =
                Selectors.parse("cluster=\"proxy\", service==frontend, path==\"\\\\totalCount\"");

        Selectors expected = Selectors.builder()
                .add(Selector.glob("cluster", "proxy"))
                .add(Selector.exact("service", "frontend"))
                .add(Selector.exact("path", "\\totalCount"))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void parseSelectorsWithHyphenInKey() {
        Selectors actual =
                Selectors.parse("{'metric-name'='name'}");

        Selectors expected = Selectors.builder()
                .add(Selector.glob("metric-name", "name"))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void parseSelectorsWithQuoteInValue() {
        Selectors actual =
                Selectors.parse("{name='\\'name\\''}");

        Selectors expected = Selectors.builder()
                .add(Selector.glob("name", "'name'"))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void parseSelectorsWithMetricName() {
        Selectors actual = Selectors.parse("some.sensor.name{}");

        Selectors expected = Selectors.of("some.sensor.name");

        assertEquals(expected, actual);
    }

    @Test
    public void parseSelectorsWithMetricNameAndLabelSelectors() {
        Selectors actual =
            Selectors.parse("some.sensor.name{cluster='production', service='gateway'}");

        Selectors expected = Selectors.builder()
            .setNameSelector("some.sensor.name")
            .add(Selector.glob("cluster", "production"))
            .add(Selector.glob("service", "gateway"))
            .build();

        assertEquals(expected, actual);
    }

    @Test
    public void parseSelectorsWithQuotedMetricName() {
        Selectors actual =
            Selectors.parse("\"some \\\"sensor\\\" name\"{cluster='production', service='gateway'}");

        Selectors expected = Selectors.builder()
            .setNameSelector("some \"sensor\" name")
            .add(Selector.glob("cluster", "production"))
            .add(Selector.glob("service", "gateway"))
            .build();

        assertEquals(expected, actual);
    }

    @Test
    public void matchesLabels() {
        Labels labels = Labels.of(
                "host", "cluster",
                "path", "/requestsError"
        );

        Selectors selectors = Selectors.of(Selector.exact("path", "/requestsError"));

        assertTrue(selectors.match(labels));
    }

    @Test
    public void matchesMap() {
        var labels = Map.of(
                "host", "cluster",
                "path", "/requestsError"
        );

        Selectors selectors = Selectors.of(Selector.exact("path", "/requestsError"));

        assertTrue(selectors.match(labels));
    }

    @Test
    public void andOverride() {
        Selectors baseSelectors = Selectors.builder()
                .add(Selector.exact("project", "solomon"))
                .add(Selector.any("cluster"))
                .add(Selector.exact("host", "solomon-front-man-13"))
                .build();

        Selectors newSelectors = Selectors.builder()
                .add(Selector.exact("project", "solomon"))
                .add(Selector.exact("cluster", "proxy"))
                .add(Selector.exact("service", "frontend"))
                .build();

        Selectors actualMergedSelectors = baseSelectors.toBuilder()
                .addOverride(newSelectors)
                .build();

        Selectors expectedMergedSelectors = Selectors.builder()
                .add(Selector.exact("host", "solomon-front-man-13"))
                .add(Selector.exact("project", "solomon"))
                .add(Selector.exact("cluster", "proxy"))
                .add(Selector.exact("service", "frontend"))
                .build();

        assertEquals(expectedMergedSelectors, actualMergedSelectors);
    }

    @Test
    public void isSingleShard() {
        Selectors selectors = Selectors.builder()
                .add(Selector.exact("project", "junk"))
                .add(Selector.glob("cluster", "foo"))
                .add(Selector.glob("service", "bar"))
                .add(Selector.notRegex("host", "(Man|solomon-front-(man|myt)-00)"))
                .build();

        assertTrue(ShardSelectors.isSingleShard(selectors));
    }

    @Test
    public void parseAndFormat() {
        Selectors parsed = Selectors.parse(
                "project=solomon, " +
                        "cluster==pre, " +
                        "service=='test', " +
                        "sensor='request.latency', " +
                        "host='*', " +
                        "bin=~'\\w+', " +
                        "endpoint!='/ok'");

        String formatted = Selectors.format(parsed);

        Selectors parsedAgain = Selectors.parse(formatted);
        assertThat(parsed, Matchers.equalTo(parsedAgain));

        Labels labels = Labels.builder()
                .add("project", "solomon")
                .add("cluster", "pre")
                .add("service", "test")
                .add("sensor", "request.latency")
                .add("host", "solomon-test-01")
                .add("bin", "100")
                .add("endpoint", "/health")
                .build();

        assertThat(parsed.match(labels), Matchers.equalTo(true));
        assertThat(parsedAgain.match(labels), Matchers.equalTo(true));
    }

    @Test
    public void parseAndFormatMap() {
        Selectors parsed = Selectors.parse(
                "project=solomon, " +
                        "cluster==pre, " +
                        "service=='test', " +
                        "sensor='request.latency', " +
                        "host='*', " +
                        "bin=~'\\w+', " +
                        "endpoint!='/ok'");

        String formatted = Selectors.format(parsed);

        Selectors parsedAgain = Selectors.parse(formatted);
        assertThat(parsed, Matchers.equalTo(parsedAgain));

        Map<String, String> labels = new HashMap<>();
        labels.put("project", "solomon");
        labels.put("cluster", "pre");
        labels.put("service", "test");
        labels.put("sensor", "request.latency");
        labels.put("host", "solomon-test-01");
        labels.put("bin", "100");
        labels.put("endpoint", "/health");

        assertThat(parsed.match(labels), Matchers.equalTo(true));
        assertThat(parsedAgain.match(labels), Matchers.equalTo(true));
    }

    @Test
    public void format() {
        Selectors selectors = Selectors.of(Selector.glob("host", "cluster"));
        assertEquals("{host='cluster'}", Selectors.format(selectors));
    }

    @Test
    public void formatWithHyphenInKey() {
        Selectors selectors = Selectors.of(Selector.glob("metric-name", "name"));
        assertEquals("{'metric-name'='name'}", Selectors.format(selectors));
    }

    @Test
    public void formatWithQuoteInValue() {
        Selectors selectors = Selectors.of(Selector.glob("name", "'name'"));
        assertEquals("{name='\\'name\\''}", Selectors.format(selectors));
    }

    @Test
    public void formatEmpty() {
        Selectors selectors = Selectors.of();
        assertEquals("{}", Selectors.format(selectors));
    }

    @Test
    public void formatSelectorWithMetricName() {
        Selectors selectors = Selectors.of("some.sensor.name");
        assertEquals("some.sensor.name{}", Selectors.format(selectors));
    }

    @Test
    public void formatSelectorWithQuotedMetricName() {
        Selectors selectors = Selectors.of("some \"sensor \" name");
        assertEquals("\"some \\\"sensor \\\" name\"{}", Selectors.format(selectors));
    }

    @Test
    public void valueOrAbsent() {
        Selectors selectors = Selectors.parse("sensor==requestStarted, target=pre|-");

        checkPositiveMatch(selectors, Labels.of("sensor", "requestStarted", "target", "pre"));
        checkPositiveMatch(selectors, Labels.of("sensor", "requestStarted"));
        checkNagetiveMatch(selectors, Labels.of("sensor", "requestStarted", "target", "prod"));
    }

    @Test
    public void matchShardKey() {
        {
            Selectors selectors = Selectors.parse("project='so*', cluster='production|prestable', service!='metabase'");

            checkPositiveMatch(selectors, new ShardKey("solomon", "prestable", "stockpile"));
            checkPositiveMatch(selectors, new ShardKey("solomon", "production", "stockpile"));
            checkNagetiveMatch(selectors, new ShardKey("solomon", "test", "stockpile"));
            checkNagetiveMatch(selectors, new ShardKey("solomon", "production", "metabase"));
            checkNagetiveMatch(selectors, new ShardKey("yasm", "production", "gateway"));
        }

        {
            Selectors selectors = Selectors.parse("project='solomon'");
            checkPositiveMatch(selectors, new ShardKey("solomon", "prestable", "stockpile"));
            checkPositiveMatch(selectors, new ShardKey("solomon", "production", "stockpile"));
            checkNagetiveMatch(selectors, new ShardKey("yasm", "production", "gateway"));
        }

        {
            Selectors selectors = Selectors.parse("project='solomon', sensor='cpu.time'");
            checkNagetiveMatch(selectors, new ShardKey("solomon", "prestable", "stockpile"));
            checkNagetiveMatch(selectors, new ShardKey("solomon", "production", "stockpile"));
            checkNagetiveMatch(selectors, new ShardKey("yasm", "production", "gateway"));
        }
    }

    private void checkPositiveMatch(Selectors selector, Labels labels) {
        assertThat(selector + " should match " + labels, selector.match(labels), Matchers.equalTo(true));
    }

    private void checkPositiveMatch(Selectors selector, ShardKey key) {
        assertThat(selector + " should match " + key, selector.match(key), Matchers.equalTo(true));
    }

    private void checkNagetiveMatch(Selectors selector, Labels labels) {
        assertThat(selector + " should not match " + labels, selector.match(labels), Matchers.equalTo(false));
    }

    private void checkNagetiveMatch(Selectors selector, ShardKey key) {
        assertThat(selector + " should not match " + key, selector.match(key), Matchers.equalTo(false));
    }
}
