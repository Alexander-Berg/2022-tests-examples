package ru.yandex.solomon.labels;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Label;
import ru.yandex.monlib.metrics.labels.Labels;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleg Baryshnikov
 */
public class LabelsFormatTest {

    @Test
    public void parseEmpty() {
        assertEquals(Labels.empty(), LabelsFormat.parse(""));
    }

    @Test
    public void parseSingleLabel() {
        assertEquals(Labels.of("key", "value"), LabelsFormat.parse("key=value"));
    }

    @Test
    public void parseSeveralLabels() {
        Labels actual = LabelsFormat.parse("key1=value1, key2=value2");
        Labels expected = Labels.of("key1", "value1", "key2", "value2");
        assertEquals(expected, actual);
    }

    @Test
    public void parseQuotedLabels() {
        Labels actual = LabelsFormat.parse("'key1'='value 1', 'key2'='value 2'");
        Labels expected = Labels.of("key1", "value 1", "key2", "value 2");
        assertEquals(expected, actual);
    }

    @Test
    public void formatEmpty() {
        String expected = "";
        String actual = LabelsFormat.format(Labels.empty());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void formatLabel() {
        String expected = "project=\"solomon\"";
        Labels labels = Labels.of("project", "solomon");
        Label label = labels.at(0);
        Assert.assertEquals(expected, LabelsFormat.format(labels));
        Assert.assertEquals(expected, LabelsFormat.format(label));
    }

    @Test
    public void formatLabels() {
        String expected = "cluster=\"man\", project=\"solomon\"";
        String actual = LabelsFormat.format(Labels.of("project", "solomon", "cluster", "man"));
        Assert.assertEquals(expected, actual);
    }
}
