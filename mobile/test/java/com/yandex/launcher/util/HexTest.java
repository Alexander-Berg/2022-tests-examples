package com.yandex.launcher.util;

import com.yandex.google.common.base.Charsets;
import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.common.util.HexUtil;

import junit.framework.Assert;

import org.junit.Test;

public class HexTest extends BaseRobolectricTest {

    public HexTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void compareResult() {
        final String testString = "Test: compare hex result";
        final byte[] testBytes = testString.getBytes(Charsets.UTF_8);

        final String expectedResult = deprecatedToHexString(testBytes);
        final String actualResult = HexUtil.toHexString(testBytes);
        Assert.assertEquals(expectedResult, actualResult);
    }

    private static String deprecatedToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
