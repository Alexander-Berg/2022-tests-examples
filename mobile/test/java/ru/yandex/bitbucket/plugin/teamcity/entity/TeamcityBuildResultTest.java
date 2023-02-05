package ru.yandex.bitbucket.plugin.teamcity.entity;

import org.junit.Test;

import ru.yandex.bitbucket.plugin.testutil.TestUtils;

public class TeamcityBuildResultTest {
    @Test
    public void isSerializable() {
        TestUtils.assertSerializable(new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 1L, "hash", "message"));
    }
}
