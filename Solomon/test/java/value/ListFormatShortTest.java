package ru.yandex.solomon.expression.value;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class ListFormatShortTest {

    private void formatTestImpl(String expected, int... list) {
        String formatted = ListFormatShort.formatList(list.length, i -> Integer.toString(list[i]));
        Assert.assertEquals(expected, formatted);
    }

    @Test
    public void format() {
        formatTestImpl("[]");
        formatTestImpl("[2]", 2);
        formatTestImpl("[2, 3]", 2, 3);
        formatTestImpl("[2, 3, 4]", 2, 3, 4);
        formatTestImpl("[2, 3, 4, 5]", 2, 3, 4, 5);
        formatTestImpl("[2, 3, 4, 5, 6]", 2, 3, 4, 5, 6);
        formatTestImpl("[2, 3, 4, 5, 6, 7]", 2, 3, 4, 5, 6, 7);
        formatTestImpl("[2, 3, 4, 5, 6, 7, 8]", 2, 3, 4, 5, 6, 7, 8);
        formatTestImpl("[2, 3, 4, 5, 6, 7, 8, 9]", 2, 3, 4, 5, 6, 7, 8, 9);
        formatTestImpl("[2, 3, 4, ..., 8, 9, 10]", 2, 3, 4, 5, 6, 7, 8, 9, 10);
        formatTestImpl("[2, 3, 4, ..., 9, 10, 11]", 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        formatTestImpl("[2, 3, 4, ..., 10, 11, 12]", 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

}
