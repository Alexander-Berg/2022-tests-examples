package ru.yandex.infra.stage.podspecs.patcher.coredump;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SandboxReleaseGetter;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TResource;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_SPEC;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.groupById;


public class CoredumpPatcherV2Test extends CoredumpPatcherV1BaseTest {
    private static final String INSTANCECTL_URL = "instancectl_custom_url";
    private static final String GDB_URL = "gdb_custom_url";
    private final int INSTANCECTL_REVISION = 1234;
    private final int GDB_REVISION = 2345;

    @Override
    protected Function<CoredumpPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return CoredumpPatcherV2::new;
    }

    protected CoredumpPatcherV1Context createPatcherContext() {
        var sandboxReleaseGetter = Mockito.mock(SandboxReleaseGetter.class);

        var instanceCtlResource = new DownloadableResource(INSTANCECTL_URL, Checksum.EMPTY);
        var gdbResource = new DownloadableResource(GDB_URL, Checksum.EMPTY);

        Mockito.doReturn(CompletableFuture.completedFuture(new ResourceWithMeta(instanceCtlResource, Optional.empty())))
                .when(sandboxReleaseGetter).getReleaseByResourceId(INSTANCECTL_REVISION, true);
        Mockito.doReturn(CompletableFuture.completedFuture(new ResourceWithMeta(gdbResource, Optional.empty())))
                .when(sandboxReleaseGetter).getReleaseByResourceId(GDB_REVISION, true);

        var instanceCtlSupplier = FixedResourceSupplier.withSupplierAndMeta(INSTANCECTL_DEFAULT,
                TestData.RESOURCE_META,
                sandboxReleaseGetter);
        var gdbSupplier = FixedResourceSupplier.withSupplierAndMeta(GDB_DEFAULT,
                TestData.RESOURCE_META,
                sandboxReleaseGetter);

        return new CoredumpPatcherV1Context(instanceCtlSupplier, gdbSupplier, RELEASE_GETTER_TIMEOUT_SECONDS);
    }

    @Test
    void checkCoredumpToolsCustomRevisions() {
        String boxId = "box_1";
        String workloadId = "workload_1";
        var templateSpec = createTemplateSpec(boxId, workloadId);
        var context = DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withCoredumpConfig(ImmutableMap.of(workloadId, TestData.COREDUMP_CONFIG_WITH_AGGREGATION))
                        .withCoredumpToolResourceInfo(new SandboxResourceInfo(INSTANCECTL_REVISION, emptyMap()))
                        .withGdbLayerResourceInfo(new SandboxResourceInfo(GDB_REVISION, emptyMap()))
                        .build()
        );

        var resources = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec().getResources();

        var staticResourceMap = groupById(resources.getStaticResourcesList(), TResource::getId);
        var instanceCtlBin = staticResourceMap.get("instancectl-binary_");
        assertThat(instanceCtlBin.getUrl(), equalTo(INSTANCECTL_URL));

        var layerMap = groupById(resources.getLayersList(), TLayer::getId);
        var gdbLayer = layerMap.get("gdb-layer_");
        assertThat(gdbLayer.getUrl(), equalTo(GDB_URL));
    }

}
