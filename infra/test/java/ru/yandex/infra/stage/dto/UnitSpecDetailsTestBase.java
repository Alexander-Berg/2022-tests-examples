package ru.yandex.infra.stage.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

abstract class UnitSpecDetailsTestBase {
    void assertRetained(ClusterAndType clusterAndType) {
        assertRetained(getSpec(), clusterAndType);
    }

    void assertRetained(DeployUnitSpecDetails spec, ClusterAndType clusterAndType) {
        assertThat(spec.shouldRetain(clusterAndType).isRetained(), equalTo(true));
    }

    protected abstract DeployUnitSpecDetails getSpec();
}
