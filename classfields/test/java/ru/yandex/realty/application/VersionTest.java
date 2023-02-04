package ru.yandex.realty.application;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class VersionTest {
    @Test
    public void testCompare() throws Exception {
        Version version_1_0_0 = new Version(1, 0, 0);
        Version version_1_1_0 = new Version(1, 1, 0);
        Version version_2_0_0 = new Version(2, 0, 0);

        Version version_1_0_qwe = new Version(1, 0, "qwe");

        Assert.assertTrue("1.0.0 < 1.0.qwe", version_1_0_0.compareTo(version_1_0_qwe) < 0);
        Assert.assertTrue("1.0.0 < 1.1.0 ", version_1_0_0.compareTo(version_1_1_0) < 0);
        Assert.assertTrue("1.0.0 < 2.0.0", version_1_0_0.compareTo(version_2_0_0) < 0);
    }
}
