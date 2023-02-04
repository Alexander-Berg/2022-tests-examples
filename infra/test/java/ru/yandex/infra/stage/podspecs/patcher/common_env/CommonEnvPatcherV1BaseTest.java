package ru.yandex.infra.stage.podspecs.patcher.common_env;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.stage.GlobalContext;
import ru.yandex.infra.stage.StageContext;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.podspecs.PatcherTestUtils;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TUtilityContainer;
import ru.yandex.yp.client.pods.TWorkload;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.groupById;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.hasLiteralEnv;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherUtils.DEPLOY_BOX_ID_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherUtils.DEPLOY_PROJECT_ID_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherUtils.DEPLOY_STAGE_ID_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherUtils.DEPLOY_UNIT_ID_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherUtils.DEPLOY_WORKLOAD_ID_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV1Base.DEPLOY_UNIT_ID_LABEL_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV1Base.PROJECT_ID_LABEL_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV1Base.STAGE_ID_LABEL_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV1Base.STAGE_URL_LABEL_KEY;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

abstract class CommonEnvPatcherV1BaseTest extends PatcherTestBase<CommonEnvPatcherV1Context> {

    protected static final CommonEnvPatcherV1Context DEFAULT_PATCHER_CONTEXT = new CommonEnvPatcherV1Context(
            List.of("PATH", "USER", "HOME"), "https://test.deploy.yandex-team.ru/stages/%s");

    @Test
    void addLabels() {
        var patchResult = patch(DEFAULT_PATCHER_CONTEXT, createPodSpecBuilder(), DEFAULT_UNIT_CONTEXT);

        var expectedLabels = createCommonLabelsAttribute(
                DEFAULT_STAGE_CONTEXT.getStageId(), DEFAULT_UNIT_CONTEXT.getDeployUnitId(),
                DEFAULT_STAGE_CONTEXT.getProjectId()
        );
        assertThatEquals(patchResult.getLabels(), expectedLabels.endMap().build());
    }

    private YTreeBuilder createCommonLabelsAttribute(String stageId, String deployUnitId, String projectId) {
        return new YTreeBuilder().beginMap()
                .key(DEPLOY_UNIT_ID_LABEL_KEY).value(deployUnitId)
                .key(STAGE_ID_LABEL_KEY).value(stageId)
                .key(PROJECT_ID_LABEL_KEY).value(projectId)
                .key(STAGE_URL_LABEL_KEY).value(String.format(DEFAULT_PATCHER_CONTEXT.stageUrlLabelFormat, stageId));
    }

    @Test
    void addCommonEnvVars() {
        String workloadId = "workload_id";
        String boxId = "box_id";

        var podSpecBuilder = createPodSpecBuilder();

        podSpecBuilder.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder().setId(boxId))
                .addWorkloads(TWorkload.newBuilder().setId(workloadId).setBoxRef(boxId).build());

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TBox box = groupById(podAgentSpec.getBoxesList(), TBox::getId).get(boxId);
        TWorkload workload = groupById(podAgentSpec.getWorkloadsList(), TWorkload::getId).get(workloadId);

        String stageId = DEFAULT_STAGE_CONTEXT.getStageId();
        String projectId = DEFAULT_STAGE_CONTEXT.getProjectId();
        String deployUnitId = DEFAULT_UNIT_CONTEXT.getDeployUnitId();
        assertThat(hasLiteralEnv(box, DEPLOY_BOX_ID_ENV_NAME, boxId), equalTo(true));
        assertThat(hasLiteralEnv(box, DEPLOY_UNIT_ID_ENV_NAME, deployUnitId), equalTo(true));
        assertThat(hasLiteralEnv(box, DEPLOY_STAGE_ID_ENV_NAME, stageId), equalTo(true));
        assertThat(hasLiteralEnv(box, DEPLOY_PROJECT_ID_ENV_NAME, projectId), equalTo(true));

        DEFAULT_STAGE_CONTEXT.getEnvVars().forEach((key, value) -> assertThat(hasLiteralEnv(box, key, value), equalTo(true)));

        assertThat(hasLiteralEnv(workload, DEPLOY_WORKLOAD_ID_ENV_NAME, workloadId), equalTo(true));
    }

    @Test
    void boxEnvVarsShouldOverrideStageEnvVars() {
        String boxId = "box_id";
        String boxVarName = "key_shared_with_box";
        String boxVarValue = "value from box";

        var podSpecBuilder = createPodSpecBuilder();
        podSpecBuilder.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(boxId)
                        .addEnv(PodSpecUtils.literalEnvVar(boxVarName, boxVarValue))
                        .build())
                .addWorkloads(TWorkload.newBuilder()
                        .setId("workload_id")
                        .setBoxRef(boxId)
                        .build());

        Map<String, String> stageVars = ImmutableMap.of("key1", "value1", boxVarName, "value from stage");

        StageContext stageContext = new StageContext(TestData.DEFAULT_STAGE_FQID, "stage_id", 100500, "abc:111", TestData.STAGE_ACL, 1,
                "project_id", emptyMap(), emptyMap(), TestData.RUNTIME_DEPLOY_CONTROLS, stageVars, GlobalContext.EMPTY);

        DeployUnitContext duContext = DEFAULT_UNIT_CONTEXT.withStageContext(stageContext);

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podSpecBuilder, duContext).getPodAgentSpec();

        TBox box = groupById(podAgentSpec.getBoxesList(), TBox::getId).get(boxId);

        assertThat(hasLiteralEnv(box, boxVarName, boxVarValue), equalTo(true));
        assertThat(hasLiteralEnv(box, "key1", "value1"), equalTo(true));
    }

    protected abstract boolean shouldAddResourceRequestEnvVars();

    protected abstract boolean shouldAddPathVarInWorkflow();

    @Test
    void resourcesEnvVarsPresence() {
        String boxId = "box_id";
        String workloadId = "workload_id";

        var podSpecBuilder = createPodSpecBuilder();

        podSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addBoxes(TBox.newBuilder().setId(boxId))
                .addWorkloads(TWorkload.newBuilder()
                        .setId(workloadId)
                        .setBoxRef(boxId)
                );

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TBox box = groupById(podAgentSpec.getBoxesList(), TBox::getId).get(boxId);
        var boxVars = PatcherTestUtils.getAllLiteralVars(box.getEnvList());

        TWorkload workload = groupById(podAgentSpec.getWorkloadsList(), TWorkload::getId).get(workloadId);
        var workloadVars = PatcherTestUtils.getAllLiteralVars(workload.getEnvList());

        boolean areVarsExpected = shouldAddResourceRequestEnvVars();

        for (var resource : ResourceRequestParameter.values()) {
            assertThat(boxVars.containsKey(resource.getEnvVarName()), equalTo(areVarsExpected));
            assertThat(workloadVars.containsKey(resource.getEnvVarName()), equalTo(false));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "0,0,0,0,",
            "0,0,1,0,1",
            "0,2,0,2,",
            "0,2,1,2,1",
            "3,0,0,3,",
            "3,0,1,3,1",
            "3,2,0,2,",
            "3,2,1,2,1",
    })
    void checkResourcesEnvVarsValue(long podValue,
                                    long boxValue,
                                    long workloadValue,
                                    String expectedBoxValue,
                                    String expectedWorkloadValue) {
        String boxId = "box_id";
        String workloadId = "workload_id";

        var podSpecBuilder = createPodSpecBuilder();

        podSpecBuilder.getSpecBuilder()
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder().setVcpuGuarantee(podValue))
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(boxId)
                        .setComputeResources(TComputeResources.newBuilder().setVcpuGuarantee(boxValue))
                )
                .addWorkloads(TWorkload.newBuilder()
                        .setId(workloadId)
                        .setBoxRef(boxId)
                        .setStart(TUtilityContainer.newBuilder()
                                .setComputeResources(TComputeResources.newBuilder().setVcpuGuarantee(workloadValue)))
                );

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        final String envVarName = ResourceRequestParameter.VCPU_GUARANTEE.getEnvVarName();

        if (!shouldAddResourceRequestEnvVars()) {
            expectedBoxValue = null;
            expectedWorkloadValue = null;
        }

        TBox box = groupById(podAgentSpec.getBoxesList(), TBox::getId).get(boxId);
        var boxVars = PatcherTestUtils.getAllLiteralVars(box.getEnvList());
        assertThat(boxVars.get(envVarName), equalTo(expectedBoxValue));

        TWorkload workload = groupById(podAgentSpec.getWorkloadsList(), TWorkload::getId).get(workloadId);
        var workloadVars = PatcherTestUtils.getAllLiteralVars(workload.getEnvList());
        assertThat(workloadVars.get(envVarName), equalTo(expectedWorkloadValue));
    }

    @Test
    void patchEnvVarsOverloadedByPortoTest() {
        String boxId = "box_id";
        String workloadId = "workload_id";
        String workloadIdWithVar = "workload_id_with_var";

        String varName = DEFAULT_PATCHER_CONTEXT.envVarsOverloadedByPortoNames.get(0);

        String boxVarValue = "/bin/java";
        String workloadVarValue = "/bin/java/other";

        var podSpecBuilder = createPodSpecBuilder();

        podSpecBuilder.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder().setId(boxId)
                        .addEnv(PodSpecUtils.literalEnvVar(varName, boxVarValue)))
                .addWorkloads(TWorkload.newBuilder().setId(workloadId).setBoxRef(boxId).build())
                .addWorkloads(TWorkload.newBuilder().setId(workloadIdWithVar).setBoxRef(boxId)
                        .addEnv(PodSpecUtils.literalEnvVar(varName, workloadVarValue))
                        .build());

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        Map<String, TWorkload> workloadMapById = groupById(podAgentSpec.getWorkloadsList(), TWorkload::getId);
        TWorkload workload = workloadMapById.get(workloadId);
        TWorkload workloadWithPath = workloadMapById.get(workloadIdWithVar);

        assertThat(hasLiteralEnv(workload, varName, boxVarValue), equalTo(shouldAddPathVarInWorkflow()));
        assertThat(hasLiteralEnv(workloadWithPath, varName, workloadVarValue), equalTo(true));
    }

    @Test
    void patchEnvVarsOverloadedByPortoNotCorruptOtherVarsTest() {
        String boxId = "box_id";
        String workloadId = "workload_id";

        String varNameNotToBeFixed = "VAR_NOT_TO_BE_INHERITED";
        String varValue = "box_value";

        var podSpecBuilder = createPodSpecBuilder();

        TPodAgentSpec podAgentSpecBefore = podSpecBuilder.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder().setId(boxId)
                        .addEnv(PodSpecUtils.literalEnvVar(varNameNotToBeFixed, varValue)))
                .addWorkloads(TWorkload.newBuilder().setId(workloadId).setBoxRef(boxId).build())
                .build();


        Map<String, TWorkload> workloadMapById = groupById(podAgentSpecBefore.getWorkloadsList(), TWorkload::getId);
        TWorkload workload = workloadMapById.get(workloadId);
        List<TEnvVar> workloadEnvListBefore = workload.getEnvList();

        TPodAgentSpec podAgentSpecPatched =
                patch(DEFAULT_PATCHER_CONTEXT, podSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        workloadMapById = groupById(podAgentSpecPatched.getWorkloadsList(), TWorkload::getId);
        workload = workloadMapById.get(workloadId);
        List<TEnvVar> workloadEnvListAfter = workload.getEnvList().stream()
                .filter(envVar -> !DEPLOY_WORKLOAD_ID_ENV_NAME.equals(envVar.getName()))
                .collect(Collectors.toList());

        assertThatEquals(workloadEnvListBefore, workloadEnvListAfter);
    }
}
