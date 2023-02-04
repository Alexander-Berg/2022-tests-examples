package ru.yandex.wmconsole.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * User: azakharov
 * Date: 20.09.12
 * Time: 11:27
 */
public class UserErrorOptionsTest {

    @Test
    public void testSiteError() {
        UserErrorOptions opts = new UserErrorOptions(0L, true);
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(403));
        for (int i = 500; i < 600; i++) {
            Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(i));
        }
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1001));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1004));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1006));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1007));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(-2));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1010));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(-3));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1014));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(-4));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1020));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(1021));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(2006));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(2014));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(2020));
        Assert.assertEquals(SeverityEnum.SITE_ERROR, opts.getSeverityByCode(2024));
    }

    @Test
    public void testDisallowedByUser() {
        UserErrorOptions opts = new UserErrorOptions(0L, true);
        for (int i = 400; i < 500; i++) {
            if (i == 403)
                continue;
            Assert.assertEquals("code = " + i, SeverityEnum.DISALLOWED_BY_USER, opts.getSeverityByCode(i));
        }
        Assert.assertEquals(SeverityEnum.DISALLOWED_BY_USER, opts.getSeverityByCode(1003));
        Assert.assertEquals(SeverityEnum.DISALLOWED_BY_USER, opts.getSeverityByCode(2005));
        Assert.assertEquals(SeverityEnum.DISALLOWED_BY_USER, opts.getSeverityByCode(2025));
    }

    @Test
    public void testUnsupportedByRobot() {
        UserErrorOptions opts = new UserErrorOptions(0L, true);
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(-1));
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(1005));
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(2007));
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(2010));
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(2011));
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(2012));
        Assert.assertEquals(SeverityEnum.UNSUPPORTED_BY_ROBOT, opts.getSeverityByCode(2016));
    }
}
