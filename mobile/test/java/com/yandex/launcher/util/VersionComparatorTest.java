package com.yandex.launcher.util;

import com.yandex.launcher.BaseRobolectricTest;

import junit.framework.Assert;

import org.junit.Test;

public class VersionComparatorTest extends BaseRobolectricTest {

    public VersionComparatorTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void testVersion() throws Exception {
        final VersionComparator versionComparator = new VersionComparator();

        Assert.assertEquals(versionComparator.compare("1.1.0", "1.1.0"), 0);
        Assert.assertEquals(versionComparator.compare("1.1.0", "1.1"), 0);
        Assert.assertEquals(versionComparator.compare("1.2.0", "1.1"), 1);
        Assert.assertEquals(versionComparator.compare("1.2.0", "1.1a"), 1);
        Assert.assertEquals(versionComparator.compare("1.1.2", "1.1.2a"), 1);
        Assert.assertEquals(versionComparator.compare("1.1", "1.1-SNAPSHOT"), 1);
        Assert.assertEquals(versionComparator.compare("1.1.2a", "1.1.2b"), -1);
    }

    @Test
    public void testVersionIgnoreSuffix() throws Exception {
        final VersionComparator versionComparator = new VersionComparator(true);

        Assert.assertEquals(versionComparator.compare("1.1.0", "1.1.0"), 0);
        Assert.assertEquals(versionComparator.compare("1.1.0", "1.1"), 0);
        Assert.assertEquals(versionComparator.compare("1.2.0", "1.1"), 1);
        Assert.assertEquals(versionComparator.compare("1.2.0", "1.1a"), 1);
        Assert.assertEquals(versionComparator.compare("1.1.2", "1.1.2a"), 0);
        Assert.assertEquals(versionComparator.compare("1.1", "1.1-SNAPSHOT"), 0);
        Assert.assertEquals(versionComparator.compare("1.1.2a", "1.1.2b"), 0);

        // Extra checks for PHONE-2080 case
        Assert.assertEquals(versionComparator.compare("2.1.1-qa", "2.1.1.0"), 0);
        Assert.assertEquals(versionComparator.compare("2.1.1-qa", "2.1.1.1"), -1);
        Assert.assertEquals(versionComparator.compare("2.1.1.0-qa", "2.1.1"), 0);
        Assert.assertEquals(versionComparator.compare("2.1.1.1-qa", "2.1.1"), 1);
        Assert.assertEquals(versionComparator.compare("2.1.1-qa", "2.1.2"), -1);
    }
}