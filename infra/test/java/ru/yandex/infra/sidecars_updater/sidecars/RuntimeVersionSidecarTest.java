package ru.yandex.infra.sidecars_updater.sidecars;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TStageSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RuntimeVersionSidecarTest {

    public static final long LAST_RUNTIME_VERSION = 5L;
    public static final Map<Sidecar.Type, Map<String, String>> DEFAULT_SIDECARS_ATTRIBUTES = Map.of(
            Sidecar.Type.RUNTIME_VERSION,
            Map.of(
                    "last", Long.toString(LAST_RUNTIME_VERSION)
            )
    );

    @Test
    public void constructorTest() {
        Sidecar sidecar = new RuntimeVersionSidecar(DEFAULT_SIDECARS_ATTRIBUTES);
        assertThat(sidecar.getRevision(), equalTo(LAST_RUNTIME_VERSION));
    }

    @Test
    public void setRevisionTest() {
        Sidecar sidecar = new RuntimeVersionSidecar(DEFAULT_SIDECARS_ATTRIBUTES);
        long anotherRevision = LAST_RUNTIME_VERSION + 1L;
        sidecar.setRevision(anotherRevision);
        assertThat(sidecar.getRevision(), equalTo(LAST_RUNTIME_VERSION));
    }

    @Test
    public void getRevisionGetterTest() {
        Sidecar sidecar = new RuntimeVersionSidecar(DEFAULT_SIDECARS_ATTRIBUTES);
        int deployUnitPatchersRevision = 3;
        TDeployUnitSpec deployUnitSpec = TDeployUnitSpec.newBuilder()
                .setPatchersRevision(deployUnitPatchersRevision)
                .build();
        assertThat(sidecar.getRevisionGetter().apply(deployUnitSpec),
                equalTo(Optional.of((long) deployUnitPatchersRevision)));
    }

    @Test
    public void isUsedByTest() {
        Sidecar sidecar = new RuntimeVersionSidecar(DEFAULT_SIDECARS_ATTRIBUTES);
        TDeployUnitSpec deployUnitSpec = TDeployUnitSpec.newBuilder().build();
        String deployUnitId = "deployUnit";
        TStageSpec stageSpec = TStageSpec.newBuilder().putDeployUnits(deployUnitId, deployUnitSpec).build();
        assertThat(sidecar.isUsedBy(stageSpec, deployUnitId), equalTo(true));
    }
}
