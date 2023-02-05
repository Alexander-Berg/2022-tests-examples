package com.yandex.bitbucket.plugin.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConstantUtilsTest {
    @Test
    public void testMakeLogMessage() {
        assertEquals("Prefix. Pull request id=1, merge hash=mergeHash123.", ConstantUtils.makeLogMessage("Prefix.", 1L, "mergeHash123"));
    }
}
