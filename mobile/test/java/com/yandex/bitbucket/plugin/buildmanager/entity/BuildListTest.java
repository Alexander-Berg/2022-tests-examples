package com.yandex.bitbucket.plugin.buildmanager.entity;

import org.junit.Test;

import ru.yandex.bitbucket.plugin.configprocessor.entity.Branch;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.configprocessor.entity.Path;
import ru.yandex.bitbucket.plugin.testutil.TestUtils;

import java.util.Collections;

public class BuildListTest {
    @Test
    public void isSerializable() {
        ConfigTCBuild build = new ConfigTCBuild();
        build.setName("name");
        build.setId("id");
        build.setIncludedPaths(Collections.singletonList(new Path("path")));
        build.setIncludedTargetBranches(Collections.singletonList(new Branch("branch")));

        TestUtils.assertSerializable(new BuildList("revision", Collections.singletonList(build)));
    }
}
