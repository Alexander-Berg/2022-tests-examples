package ru.yandex.infra.stage.podspecs.patcher.dynamic_resource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.infra.stage.StageContext;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SandboxReleaseGetter;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.util.AssertUtils;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_RESOURCE_INFO;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_REVISION;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.groupById;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.MEGABYTE;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;


abstract class DynamicResourcePatcherV1BaseTest extends PatcherTestBase<DynamicResourcePatcherV1Context> {
    private static final int RELEASE_GETTER_TIMEOUT_SECONDS = 1;
    protected static final long DEFAULT_ANON_LIMIT_FOR_BOX = MEGABYTE;
    private static final String DRU_WORKLOAD_SUFFIX = "__dru";

    private static void addBox(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String boxId) {
        TBox box = TBox.newBuilder()
                .setId(boxId)
                .setComputeResources(TComputeResources.newBuilder().setAnonymousMemoryLimit(DEFAULT_ANON_LIMIT_FOR_BOX).build())
                .build();
        builder.getSpecBuilder().addBoxes(box);
    }

    private static void addWorkload(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String workloadId,
                                    String boxId) {
        TWorkload workload = TWorkload.newBuilder().setId(workloadId).setBoxRef(boxId).build();
        builder.getSpecBuilder().addWorkloads(workload);
    }

    private DynamicResourcePatcherV1Context createPatcherContext() {
        SandboxReleaseGetter sandboxReleaseGetter = Mockito.mock(SandboxReleaseGetter.class);

        CompletableFuture<ResourceWithMeta> resourceWithMetaCompletableFuture = new CompletableFuture<>();
        resourceWithMetaCompletableFuture.complete(new ResourceWithMeta(TestData.DOWNLOADABLE_RESOURCE2,
                Optional.empty()));
        Mockito.doReturn(resourceWithMetaCompletableFuture)
                .when(sandboxReleaseGetter)
                .getReleaseByResourceId(DEFAULT_SIDECAR_REVISION, true);

        ResourceSupplier universalSupplier = new FixedResourceSupplier(TestData.DOWNLOADABLE_RESOURCE,
                Optional.empty(), sandboxReleaseGetter);

        return new DynamicResourcePatcherV1Context(universalSupplier, universalSupplier, RELEASE_GETTER_TIMEOUT_SECONDS);
    }

    private static Collection<Arguments> provideParametersForDRUPatching() {
        DeployUnitContext contextWithoutSidecar = DEFAULT_UNIT_CONTEXT.withStageContext(
                StageContext::withDynamicResources,
                ImmutableMap.of(TestData.DYNAMIC_RESOURCE_ID, TestData.DYNAMIC_RESOURCE_SPEC)
        );

        DeployUnitContext contextWithSidecar = contextWithoutSidecar.withDeployUnitSpec(
                DeployUnitSpec::withDruLayerResourceInfo, DEFAULT_SIDECAR_RESOURCE_INFO
        );

        return ImmutableList.of(
                Arguments.of(contextWithoutSidecar, TestData.DOWNLOADABLE_RESOURCE),
                Arguments.of(contextWithSidecar, TestData.DOWNLOADABLE_RESOURCE2)
        );
    }

    private static TPodTemplateSpec.Builder createTemplateSpec(String boxToPatch) {
        String emptyBox = "some_id";
        String workloadId1 = "workload_1";

        TPodTemplateSpec.Builder templateSpec = TPodTemplateSpec.newBuilder();

        DataModel.TPodSpec.TPodAgentPayload.Builder agentPayloadBuilder = templateSpec
                .getSpecBuilder()
                .getPodAgentPayloadBuilder();

        addBox(agentPayloadBuilder, boxToPatch);
        addBox(agentPayloadBuilder, emptyBox);
        addWorkload(agentPayloadBuilder, workloadId1, boxToPatch);

        return templateSpec;
    }

    protected abstract AllComputeResources getExpectedAdditionalBoxResources();

    protected abstract AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec);


    private void ensureBoxResources(TPodTemplateSpec.Builder podSpec, String boxId) {
        var actualSpecResources = podSpec.getSpec().getResourceRequests();
        var boxSpec = podSpec.getSpec().getPodAgentPayload().getSpec().getBoxesList().stream()
                .filter(box -> box.getId().equals(boxId))
                .findFirst()
                .orElseThrow();

        AllComputeResources expectedResources = getExpectedAdditionalBoxResources();
        AssertUtils.assertResourceRequestEquals(actualSpecResources, expectedResources);

        var actualComputeResources = boxSpec.getComputeResources();
        AllComputeResources expectedGuarantee = getExpectedBoxGuaranteeResources(boxSpec);
        assertThatEquals(actualComputeResources.getMemoryGuarantee(), expectedGuarantee.getMemoryGuarantee());
        assertThatEquals(actualComputeResources.getVcpuGuarantee(), expectedGuarantee.getVcpuGuarantee());
        assertThatEquals(actualComputeResources.getAnonymousMemoryLimit(), DEFAULT_ANON_LIMIT_FOR_BOX + expectedResources.getAnonymousMemoryLimit());
    }


    @ParameterizedTest
    @MethodSource("provideParametersForDRUPatching")
    void patchWithDRU(DeployUnitContext context, DownloadableResource expectedDRULayer) {
        String boxToPatch = TestData.DEFAULT_BOX_ID;

        TPodTemplateSpec.Builder templateSpec = createTemplateSpec(boxToPatch);

        patch(createPatcherContext(), templateSpec, context);

        TPodAgentSpec.Builder agentSpec = templateSpec.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();

        Map<String, TLayer> layerMap = groupById(agentSpec.getResources().getLayersList(), TLayer::getId);
        assertThat(layerMap.keySet(), containsInAnyOrder("dru-layer_"));
        TLayer druLayer = layerMap.get("dru-layer_");
        assertThat(druLayer.getUrl(), equalTo(expectedDRULayer.getUrl()));

        List<TWorkload> druWorkloads = agentSpec.getWorkloadsList().stream()
                .filter(w -> w.getId().endsWith(DRU_WORKLOAD_SUFFIX))
                .collect(Collectors.toList());

        assertThat(druWorkloads.size(), equalTo(1));

        TWorkload druWorkload = druWorkloads.get(0);

        assertThat(druWorkload.getStart().getCommandLine(), containsString("dru"));
        assertThat(druWorkload.getBoxRef(), equalTo(boxToPatch));

        List<TBox> boxes = agentSpec.getBoxesList().stream()
                .filter(box -> druWorkload.getBoxRef().equals(box.getId()))
                .collect(Collectors.toList());

        assertThat(boxes.size(), equalTo(1));
        assertThat(boxes.get(0).getBindSkynet(), equalTo(true));
        ensureBoxResources(templateSpec, boxToPatch);
    }
}
