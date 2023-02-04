package ru.yandex.solomon.labels.selector;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.labels.query.Selector;
import ru.yandex.solomon.labels.query.Selectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Maksim Leonov
 */
public class LabelSelectorSetTest {

    @Test
    public void testSimpleParse() {
        LabelSelectorSet testing;

        Map<String, String> sample = new HashMap<>();
        sample.put("aaa", "bbb");
        sample.put("ccc", "ddd");

        testing = LabelSelectorSet.parseEscaped("aaa=bbb&ccc=ddd");
        assertTrue("Simple parse", sample.equals(testing.toLinkedHashMap()));

        testing = LabelSelectorSet.parseEscaped("aaa=bbb&ccc=ddd&");
        assertTrue("Simple parse w trailing ampersand", sample.equals(testing.toLinkedHashMap()));

        testing = LabelSelectorSet.parseEscaped("aaa=bbb&ccc=ddd&eee=fff");
        Assert.assertFalse("Simple parse non-equality test", sample.equals(testing.toLinkedHashMap()));
    }

    @Test
    public void testSeparators() {
        LabelSelectorSet testing;

        Map<String, String> sample = new HashMap<>();
        sample.put("aaa", "bbb");
        sample.put("ccc", "ddd");

        testing = LabelSelectorSet.parseEscaped("aaa=bbb&ccc=ddd");
        assertTrue("'&' as separator", sample.equals(testing.toLinkedHashMap()));

        testing = LabelSelectorSet.parseEscaped("aaa=bbb;ccc=ddd");
        assertTrue("';' as separator", sample.equals(testing.toLinkedHashMap()));
    }

    @Test
    public void testEscaping() {
        separatorEscapeTest("No escaping test", "bbb", "bbb");
        separatorEscapeTest("Simple quotes test", "\"bbb\"", "bbb");
        separatorEscapeTest("Test with separator in quotes", "\"bbb&ccc\"", "bbb&ccc");
        separatorEscapeTest("Separator and escaped quote in quotes", "\"bbb\\\"&ccc\"", "bbb\"&ccc");
        separatorEscapeTest("Special characters", "\\n\" \\n\"", "\n \n");
        separatorEscapeFailTest("Escaping meaningless symbols", "a\\b\\cd");
    }

    @Test
    public void testNonEscapedSeparator() {
        separatorEscapeFailTest("Test with unescaped separator", "bbb&ccc");
        separatorEscapeFailTest("Separator and escaped quote", "bbb\\\"&ccc");
        separatorEscapeFailTest("Separator outside the quotes", "\"bbb\"&ccc");
    }

    @Test
    public void testBadFormat() {
        separatorEscapeFailTest("Unused escape symbol", "aaa\\");
        separatorEscapeFailTest("not closed quote", "\"aaa");
    }

    private void separatorEscapeTest(String message, String escapedValue, String expectedValue) {
        Map<String, String> sample = new HashMap<>();
        sample.put("aaa", expectedValue);
        sample.put("ccc", "ddd");

        LabelSelectorSet testing = LabelSelectorSet.parseEscaped("aaa=" + escapedValue + "&ccc=ddd");
        Assert.assertEquals(message, sample, testing.toLinkedHashMap());
    }

    private void separatorEscapeFailTest(String message, String escapedValue) {
        try {
            LabelSelectorSet.parseEscaped("aaa=" + escapedValue);
            Assert.fail(message);
        } catch (RuntimeException expected) {
            // OK
        }
    }

    private void checkEscapeUnescape(String escaped, String unescaped) {
        Assert.assertEquals(escaped, LabelSelectorSet.escapeValue(unescaped));
        Assert.assertEquals(unescaped, LabelSelectorSet.splitEscapedLabels(escaped).findFirst().orElseThrow(AssertionError::new));
    }

    @Test
    public void serializeEscapedTest() {
        checkEscapeUnescape("aaa", "aaa");
        checkEscapeUnescape("\"a b\"", "a b");
        checkEscapeUnescape("\"a\\\"a\"", "a\"a");
        checkEscapeUnescape("\"a\\na\"", "a\na");
        checkEscapeUnescape("\"a \\\" \\\\\\na\"", "a \" \\\na");
    }

    @Test
    public void labelValueWithSpaces() {
        LabelSelectorSet.parseEscaped("title=[prod] base_gen_etc2_hahn_0: reduce mkdb query_factors");
    }

    @Test
    public void matchViaGlobByQuestion() throws Exception {
        LabelSelectorSet selector = LabelSelectorSet.parseEscaped("project=kikimr&cluster=foo&service=bar&sensor=idleTime&host=kikimr-??");

        Labels labels = Labels.of(
            "project", "kikimr",
            "cluster", "foo",
            "service", "bar",
            "sensor", "idleTime");

        assertTrue(selector.matchesAll(labels.add("host", "kikimr-01")));
        assertTrue(selector.matchesAll(labels.add("host", "kikimr-02")));

        assertFalse(selector.matchesAll(labels.add("host", "kikimr-1")));
        assertFalse(selector.matchesAll(labels.add("host", "kikimr-5")));
        assertFalse(selector.matchesAll(labels.add("host", "kikimr-125")));
        assertFalse(selector.matchesAll(labels));
    }

    @Test
    public void matchAbsent() throws Exception {
        LabelSelectorSet selector = LabelSelectorSet.parseEscaped("sensor=*|-");

        assertTrue(selector.matchesAll(Labels.empty()));
        assertTrue(selector.matchesAll(Labels.of("sensor", "sensor-1")));
    }

    @Test
    public void toNewSelectors() {
        LabelSelectorSet labelSelectorSet =
            LabelSelectorSet.parseEscaped("label1=*&label2=-&label3=cluster&label4=solomon-*&label5=cluster|solomon-*&label6=!cluster");

        Selectors actualSelectors = labelSelectorSet.toNewSelectors();

        Selectors expectedSelectors = Selectors.of(
            Selector.any("label1"),
            Selector.absent("label2"),
            Selector.glob("label3", "cluster"),
            Selector.glob("label4", "solomon-*"),
            Selector.glob("label5", "cluster|solomon-*"),
            Selector.notGlob("label6", "cluster")
        );

        assertEquals(expectedSelectors, actualSelectors);
    }
}
