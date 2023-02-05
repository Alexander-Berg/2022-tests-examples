package com.yandex.launcher.testutils;

import static org.junit.Assert.assertEquals;

import com.yandex.launcher.BaseRobolectricTest;

import org.junit.Test;

public class SampleTest extends BaseRobolectricTest {

    public SampleTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void testResources() throws Exception {
        String content = TestUtils.getFileContent("sample_test_resource.txt");
        assertEquals("test", content);
    }
}
