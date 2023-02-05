package com.yandex.bitbucket.plugin.buildmanager.entity;

import org.junit.Test;

import static ru.yandex.bitbucket.plugin.testutil.TestUtils.assertSerializable;

public class BuildListStateTest {
    @Test
    public void isSerializable() {
        assertSerializable(new PullRequestState(PullRequestStatus.PENDING, "message"));
    }
}
