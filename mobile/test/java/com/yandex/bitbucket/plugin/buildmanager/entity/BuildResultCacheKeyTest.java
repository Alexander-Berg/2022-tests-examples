package com.yandex.bitbucket.plugin.buildmanager.entity;

import org.junit.Test;

import ru.yandex.bitbucket.plugin.testutil.TestUtils;

public class BuildResultCacheKeyTest {
    @Test
    public void isSerializable() {
        TestUtils.assertSerializable(new BuildResultCacheKey(1, 1L, "id"));
    }
}
