package ru.yandex.wmconsole.service;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster.common.util.HostnamePart;
import ru.yandex.webmaster.common.util.HostnamePartUtils;

/**
 * @author aherman
 */
public class HostRegionServiceTest {
    @Test
    public void testUrlTemplateParse1() {
        HostnamePart hostnamePart = HostnamePartUtils.cleanHostnamePart("lenta");
        Assert.assertNotNull(hostnamePart);
        Assert.assertEquals("a LIKE '%%lenta%%'", HostRegionService.toHostnameLikeSql("a", hostnamePart));
    }

    @Test
    public void testUrlTemplateParse2() {
        HostnamePart hostnamePart = HostnamePartUtils.cleanHostnamePart("http://lenta");
        Assert.assertNotNull(hostnamePart);
        Assert.assertEquals("a LIKE 'lenta%%'", HostRegionService.toHostnameLikeSql("a", hostnamePart));
    }

    @Test
    public void testUrlTemplateParse3() {
        HostnamePart hostnamePart = HostnamePartUtils.cleanHostnamePart("https://lenta");
        Assert.assertNotNull(hostnamePart);
        Assert.assertEquals("a LIKE 'https://lenta%%'", HostRegionService.toHostnameLikeSql("a", hostnamePart));
    }

    @Test
    public void testUrlTemplateParse4() {
        HostnamePart hostnamePart = HostnamePartUtils.cleanHostnamePart("президент.рф");
        Assert.assertNotNull(hostnamePart);
        Assert.assertEquals("a LIKE '%%xn--d1abbgf6aiiy.xn--p1ai'", HostRegionService.toHostnameLikeSql("a",
                hostnamePart));
    }

    @Test
    public void testUrlTemplateParse5() {
        HostnamePart hostnamePart = HostnamePartUtils.cleanHostnamePart("http://президент.рф");
        Assert.assertNotNull(hostnamePart);
        Assert.assertEquals("a LIKE 'xn--d1abbgf6aiiy.xn--p1ai'", HostRegionService.toHostnameLikeSql("a",
                hostnamePart));
    }

    @Test
    public void testUrlTemplateParse6() {
        HostnamePart hostnamePart = HostnamePartUtils.cleanHostnamePart("https://президент.рф");
        Assert.assertNotNull(hostnamePart);
        Assert.assertEquals("a LIKE 'https://xn--d1abbgf6aiiy.xn--p1ai'", HostRegionService.toHostnameLikeSql("a",
                hostnamePart));
    }
}
