package ru.yandex.bitbucket.plugin.configprocessor.entity;

import org.junit.Test;

import static ru.yandex.bitbucket.plugin.testutil.TestUtils.assertSerializable;

public class BranchTest {
    @Test
    public void isSerializable() {
        assertSerializable(new Branch("branch"));
    }
}
