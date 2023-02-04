package ru.yandex.webmaster3.storage.util.yt;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class YtPathTest {
    @Test
    public void testIsRoot() throws Exception {

    }

    @Test
    public void testGetName() throws Exception {

    }

    @Test
    public void testGetParent() throws Exception {
        YtPath path = YtPath.fromString("freud://home/webmaster");
        Assert.assertEquals("freud://home", path.getParent().toString());
        Assert.assertEquals("freud://", path.getParent().getParent().toString());
        Assert.assertEquals("freud://", path.getParent().getParent().getParent().toString());
    }

    @Test
    public void testRelativePath() throws Exception {
        YtPath path = YtPath.path(YtPath.fromString("freud://home/webmaster/test"), "@");
        Assert.assertEquals("freud://home/webmaster/test/@", path.toString());

        YtPath path1 = YtPath.path(YtPath.fromString("freud://home/webmaster/test"), "test1");
        Assert.assertEquals("freud://home/webmaster/test/test1", path1.toString());

        YtPath path2 = YtPath.path(YtPath.fromString("freud://home/webmaster/test"), "test1/test2");
        Assert.assertEquals("freud://home/webmaster/test/test1/test2", path2.toString());
    }

}
