package ru.yandex.solomon.labels;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;


/**
 * @author Sergey Polovko
 */
public class LabelsStringTest {

    @Test
    public void parse() {
        // empty
        {
            Labels expected = Labels.empty();
            Labels actual = LabelsString.parse("", '&');
            Assert.assertEquals(expected, actual);
        }

        // one
        {
            Labels expected = Labels.of("project", "solomon");
            Labels actual = LabelsString.parse("project=solomon", '&');
            Assert.assertEquals(expected, actual);
        }

        // two
        {
            Labels expected = Labels.of("project", "solomon", "cluster", "man");
            Labels actual = LabelsString.parse("project=solomon,cluster=man", ',');
            Assert.assertEquals(expected, actual);
        }

        // three
        {
            Labels expected = Labels.of("project", "solomon", "cluster", "man", "service", "stockpile");
            Labels actual = LabelsString.parse("project=solomon cluster=man service=stockpile", ' ');
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void format() {
        // empty
        {
            String expected = "";
            String actual = LabelsString.format(Labels.empty(), '&');
            Assert.assertEquals(expected, actual);
        }

        // one
        {
            String expected = "project=solomon";
            String actual = LabelsString.format(Labels.of("project", "solomon"), '&');
            Assert.assertEquals(expected, actual);
        }

        // two
        {
            String expected = "cluster=man;project=solomon";
            String actual = LabelsString.format(Labels.of("project", "solomon", "cluster", "man"), ';');
            Assert.assertEquals(expected, actual);
        }

        // three
        {
            String expected = "cluster=man project=solomon service=stockpile";
            String actual = LabelsString.format(Labels.of("project", "solomon", "cluster", "man", "service", "stockpile"), ' ');
            Assert.assertEquals(expected, actual);
        }
    }
}
