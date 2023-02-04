package ru.yandex.realty;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.deployment.PropertiesInterpolator;

import java.util.Properties;

/**
 * Created by abulychev on 14.07.16.
 */
public class PropertiesInterpolatorTest {

    @Test
    public void test1() {
        Properties props = new Properties();
        props.setProperty("data.folder", "/tmp");
        props.setProperty("feeds.folder", "${data.folder}/feeds");

        Properties interpolated = PropertiesInterpolator.interpolate(props);
        Assert.assertEquals("/tmp", interpolated.getProperty("data.folder"));
        Assert.assertEquals("/tmp/feeds", interpolated.getProperty("feeds.folder"));
    }

    @Test
    public void test2() {
        Properties props = new Properties();
        props.setProperty("data.folder", "/tmp");
        props.setProperty("feeds.folder", "${tmp.folder}/feeds");

        Properties interpolated = PropertiesInterpolator.interpolate(props);
        Assert.assertEquals("/tmp", interpolated.getProperty("data.folder"));
        Assert.assertEquals("${tmp.folder}/feeds", interpolated.getProperty("feeds.folder"));
    }

    @Test
    public void test3() {
        Properties props = new Properties();
        props.setProperty("host", "127.0.0.1");
        props.setProperty("port", "80");
        props.setProperty("url", "http://${host}:${port}/");

        Properties interpolated = PropertiesInterpolator.interpolate(props);
        Assert.assertEquals("127.0.0.1", interpolated.getProperty("host"));
        Assert.assertEquals("80", interpolated.getProperty("port"));
        Assert.assertEquals("http://127.0.0.1:80/", interpolated.getProperty("url"));
    }
}
