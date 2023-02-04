package ru.yandex.solomon.labels.query;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.validate.LabelsValidator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.labels.query.SelectorType.EXACT;
import static ru.yandex.solomon.labels.query.SelectorType.GLOB;
import static ru.yandex.solomon.labels.query.SelectorType.NOT_EXACT;
import static ru.yandex.solomon.labels.query.SelectorType.NOT_GLOB;
import static ru.yandex.solomon.labels.query.SelectorType.NOT_REGEX;
import static ru.yandex.solomon.labels.query.SelectorType.REGEX;
import static ru.yandex.solomon.labels.query.SelectorsValidator.isKeyValid;
import static ru.yandex.solomon.labels.query.SelectorsValidator.isValid;
import static ru.yandex.solomon.labels.query.SelectorsValidator.isValueValid;

/**
 * @author Vladimir Gordiychuk
 */
public class SelectorsValidatorTest {

    @Test
    public void glob() {
        for (SelectorType type : Arrays.asList(GLOB, NOT_GLOB)) {
            assertTrue(isValueValid(type, "*"));
            assertTrue(isValueValid(type, "?"));
            assertTrue(isValueValid(type, "???"));
            assertTrue(isValueValid(type, "?|??|???"));
            assertTrue(isValueValid(type, "solomon-??"));
            assertTrue(isValueValid(type, "a|b|c|something-*"));
            assertTrue(isValueValid(type, "exact"));
            assertTrue(isValueValid(type, "!@#$@#$%^&*"));

            assertFalse(isValueValid(type, "some\u0001value"));
            assertFalse(isValueValid(type, "привет"));
            assertFalse(isValueValid(type, "group_lines('min', selector)"));
        }
    }

    @Test
    public void regexp() {
        for (SelectorType type : Arrays.asList(REGEX, NOT_REGEX)) {
            assertTrue(isValueValid(type, "[a-z]+"));
            assertTrue(isValueValid(type, ".*"));
            assertTrue(isValueValid(type, "exact"));

            assertFalse(isValueValid(type, "!@#$@#$%^&*["));
            assertFalse(isValueValid(type, "("));
        }
    }

    @Test
    public void exact() {
        for (SelectorType type : Arrays.asList(EXACT, NOT_EXACT)) {
            assertTrue(isValueValid(type, "ab"));
            assertTrue(isValueValid(type, "aB"));
            assertTrue(isValueValid(type, "a-b"));
            assertTrue(isValueValid(type, "a_b"));
            assertTrue(isValueValid(type, "a.b"));
            assertTrue(isValueValid(type, "a.b"));

            assertFalse(isValueValid(type, "some\u0001value"));
            assertFalse(isValueValid(type, "привет"));
            assertFalse(isValueValid(type, "-"));
            assertFalse(isValueValid(type, "host-*"));
            assertFalse(isValueValid(type, "???"));
            assertFalse(isValueValid(type, "a|b"));
            assertFalse(LabelsValidator.isValueValid("\"value\""));
            assertFalse(LabelsValidator.isValueValid("'value'"));
            assertFalse(LabelsValidator.isValueValid("`value`"));
            assertFalse(isValueValid(type, "\\w+"));
        }
    }

    @Test
    public void key() {
        assertTrue(isKeyValid("a"));
        assertTrue(isKeyValid("a123"));
        assertTrue(isKeyValid("abcdefghijklmnopqrstuvwxyz123456"));
        assertTrue(isKeyValid("shard_id"));

        assertFalse(isKeyValid(""));
        assertFalse(isKeyValid("a$"));
        assertFalse(isKeyValid("a#"));
    }

    @Test
    public void selector() {
        assertTrue(isValid(SelectorType.EXACT, "host", "solomon-01"));
        assertTrue(isValid(SelectorType.GLOB, "host", "*"));
        assertTrue(isValid(SelectorType.GLOB, "host", "solomon-man-??|solomon-sas-*"));
        assertTrue(isValid(SelectorType.EXACT, "my.elapsedTimeMs", "100"));
        assertTrue(isValid(SelectorType.REGEX, "my.elapsedTimeMs", "]"));

        assertFalse(isValid(SelectorType.GLOB, "not valid key", "*"));
    }

    @Test
    public void projectSelector() {
        assertTrue(isValid(SelectorType.EXACT, "project", "solomon"));
        assertTrue(isValid(SelectorType.GLOB, "project", "solomon"));

        assertFalse(isValid(SelectorType.ABSENT, "project", "-"));
        assertFalse(isValid(SelectorType.ANY, "project", "*"));

        assertFalse(isValid(SelectorType.NOT_EXACT, "project", "solomon"));

        assertFalse(isValid(SelectorType.GLOB, "project", "project_*"));
        assertFalse(isValid(SelectorType.GLOB, "project", "solomon|kikimr"));
        assertFalse(isValid(SelectorType.NOT_GLOB, "project", "solomon"));

        assertFalse(isValid(SelectorType.REGEX, "project", "(solomon|kikimr)"));
        assertFalse(isValid(SelectorType.NOT_REGEX, "project", "(solomon|kikimr)"));
    }
}
