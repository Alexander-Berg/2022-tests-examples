package ru.yandex.webmaster3.core.searchquery;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class QueryUtilsTest {
    @Test
    public void testUtf() throws Exception {
        String expected = "utf запрос";
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8(expected));
    }

    @Test
    public void testSpaces() throws Exception {
        String expected = "utf запрос с пробелами";
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8("\butf \bзапрос с пробелами   "));
    }

    @Test
    public void testMixedCases() throws Exception {
        String expected = "запрос utf8 со смешанным регистром";
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8("Запрос UTF8 со СмеШанным регистром"));
    }

    @Test
    public void testSpecialChar() throws Exception {
        String expected = "some query with utf8_first_char";
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8("some \u007fquery    with UTF8_FIRST_CHAR \u007f"));
    }

    @Test
    public void testToLower() throws Exception {
        String expected = "weird query with that is longer with tolower ⱦ";
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8("weird query with that is longer with tolower Ⱦ"));
    }

    @Test
    public void testEmpty() throws Exception {
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8(""));
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8(" "));
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8("  "));
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8(" \u0009 "));
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8(" \r "));
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8(" \n "));
        Assert.assertEquals("", QueryUtils.CheckAndFixQueryStringUTF8(" \t "));
    }

    @Test
    public void testSpaces1() throws Exception {
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8("test"));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8("test "));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8("test  "));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8("test \n "));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8("test \n "));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8(" test"));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8("  test"));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8(" \n test"));
        Assert.assertEquals("test", QueryUtils.CheckAndFixQueryStringUTF8(" \b test \t\n\r "));

        Assert.assertEquals("test test", QueryUtils.CheckAndFixQueryStringUTF8(" \b test \t\n\r test\t\t\n"));
    }

    @Test
    public void testList() throws Exception {
        Assert.assertEquals(Collections.emptyList(), QueryUtils.CheckAndFixQueryStringUTF8((Collection<String>) null));
        Assert.assertEquals(Collections.emptyList(), QueryUtils.CheckAndFixQueryStringUTF8(Collections.emptyList()));
        Assert.assertEquals(Collections.emptyList(), QueryUtils.CheckAndFixQueryStringUTF8(Lists.newArrayList("", "\n\t")));

        List<String> expected = Lists.newArrayList("1", "2");
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8(Lists.newArrayList(
                "",
                "1 ",
                "\n\t",
                "\t2\r"
        )));
    }

    @Test
    public void testArray() throws Exception {
        Assert.assertEquals(Collections.emptyList(), QueryUtils.CheckAndFixQueryStringUTF8((String[]) null));
        Assert.assertEquals(Collections.emptyList(), QueryUtils.CheckAndFixQueryStringUTF8(new String[]{}));
        Assert.assertEquals(Collections.emptyList(), QueryUtils.CheckAndFixQueryStringUTF8(new String[]{"", "\n\t"}));

        List<String> expected = Lists.newArrayList("1", "2");
        Assert.assertEquals(expected, QueryUtils.CheckAndFixQueryStringUTF8(new String[]{
                "",
                "1 ",
                "\n\t",
                "\t2\r"
        }));
    }
}
