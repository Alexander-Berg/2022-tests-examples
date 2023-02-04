package ru.yandex.solomon.labels;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class FormatLabelTest {

    @Test
    public void formatLabel() {
        String r = FormatLabel.formatLabel("aaa {{type}} bbb {{path}} ccc", s -> {
            if (s.equals("type")) return "blank";
            if (s.equals("path")) return "straight";
            throw new RuntimeException("missing path");
        });
        Assert.assertEquals("aaa blank bbb straight ccc", r);
    }
}
