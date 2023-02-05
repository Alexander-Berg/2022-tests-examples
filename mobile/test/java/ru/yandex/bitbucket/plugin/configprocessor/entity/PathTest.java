package ru.yandex.bitbucket.plugin.configprocessor.entity;

import org.junit.Test;

import static ru.yandex.bitbucket.plugin.testutil.TestUtils.assertSerializable;

public class PathTest {
    @Test
    public void isSerializable() {
        assertSerializable(new Path("path"));
    }
}
