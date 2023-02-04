package ru.yandex.webmaster.common.util.xml;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class SimpleXmlBuilderTest {
    @Test
    public void testNullCharacters() throws Exception {
        SimpleXmlBuilder xml = new SimpleXmlBuilder();
        xml.element("test", "abc\u0000efg");

        Assert.assertEquals("<test>abcefg</test>", xml.finish());
    }

    @Test
    public void testEscapedCharacter1() throws Exception {
        SimpleXmlBuilder xml = new SimpleXmlBuilder();
        xml.element("test", "a\u0001b");

        Assert.assertEquals("<test>a&#x1;b</test>", xml.finish());
    }

    @Test
    public void testEscapedCharacter2() throws Exception {
        SimpleXmlBuilder xml = new SimpleXmlBuilder();
        xml.element("test", "a\u001fb");

        Assert.assertEquals("<test>a&#x1f;b</test>", xml.finish());
    }

    @Test
    public void testStandartEntities() throws Exception {
        SimpleXmlBuilder xml = new SimpleXmlBuilder();
        xml.element("test", "a<\"&'>b");

        Assert.assertEquals("<test>a&lt;&quot;&amp;&apos;&gt;b</test>", xml.finish());
    }

    @Test
    public void testDateTimeForma() throws Exception {
        SimpleXmlBuilder xml = new SimpleXmlBuilder();
        String expectedDate = "2013-11-28T10:00:00+04:00";
        DateTime dateTime = DateTime.parse(expectedDate);
        xml.open("a");
        xml.element("java-date", dateTime.toDate());
        xml.element("joda-dateTime", dateTime);
        xml.close();

        Assert.assertEquals(
                "<a><java-date>" + expectedDate + "</java-date><joda-dateTime>" + expectedDate + "</joda-dateTime></a>",
                xml.finish());
    }
}
