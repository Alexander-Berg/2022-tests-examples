package ru.yandex.bitbucket.plugin.configprocessor.entity;

import java.util.Collections;

import org.junit.Test;

import static ru.yandex.bitbucket.plugin.testutil.TestUtils.assertSerializable;

public class ConfigTCBuildTest {
    @Test
    public void isSerializable() {
        ConfigTCBuild configTCBuild = new ConfigTCBuild();
        configTCBuild.setName("name");
        configTCBuild.setId("id");
        configTCBuild.setIncludedPaths(Collections.singletonList(new Path("path")));
        configTCBuild.setIncludedTargetBranches(Collections.singletonList(new Branch("branch")));
        assertSerializable(configTCBuild);
    }
}
