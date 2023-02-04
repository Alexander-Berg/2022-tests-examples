package ru.yandex.infra.stage.podspecs.patcher.security;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.BoxJugglerConfig;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DeployUnitSpecDetails;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.LogrotateConfig;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.dto.SecuritySettings;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.logrotate.LogrotatePatcherV1Base;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.EContainerIsolationMode;
import ru.yandex.yp.client.pods.EVolumeCreateMode;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TMountedVolume;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TRootfsVolume;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yt.ytree.TAttribute;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.COREDUMP_CONFIG_WITH_AGGREGATION;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.DOWNLOADABLE_RESOURCE;
import static ru.yandex.infra.stage.TestData.LOGBROKER_CONFIG;
import static ru.yandex.infra.stage.TestData.SANDBOX_ATTRIBUTES;
import static ru.yandex.infra.stage.TestData.SECURITY_SETTINGS;
import static ru.yandex.infra.stage.TestData.TASK_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.COREDUMP_FOLDERS_LAYER_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_CHECKS_VOLUME_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_CHECKS_VOLUME_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_FOLDERS_LAYER_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_LOGS_VOLUME_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_LOGS_VOLUME_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_SCHEDULER_VOLUME_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_SCHEDULER_VOLUME_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_STATE_VOLUME_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.JUGGLER_STATE_VOLUME_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.LOGBROKER_FOLDERS_LAYER_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.LOGROTATE_FOLDERS_LAYER_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.POD_AGENT_FOLDERS_LAYER_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.PORTOSHELL_FOLDERS_LAYER_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.PORTOSHELL_HOMES_VOLUME_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.PORTOSHELL_HOMES_VOLUME_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.TMP_VOLUME_BASE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.TMP_VOLUME_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.USE_ENV_SECRET_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.createRemoveFoldersContentCommand;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

abstract class SecurityPatcherV1BaseTest extends PatcherTestBase<SecurityPatcherV1Context> {
    protected static final SandboxResourceMeta POD_AGENT_BINARY_RESOURCE_META_AFFECTED = TestData.RESOURCE_META;

    private static final long FIRST_AFFECTED_POD_AGENT_VERSION = POD_AGENT_BINARY_RESOURCE_META_AFFECTED.getResourceId();

    protected static final SandboxResourceInfo POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED = new SandboxResourceInfo(FIRST_AFFECTED_POD_AGENT_VERSION, emptyMap());

    protected static final SandboxResourceMeta POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED = new SandboxResourceMeta(TASK_ID,
            FIRST_AFFECTED_POD_AGENT_VERSION - 1,
            SANDBOX_ATTRIBUTES
    );

    protected static final SandboxResourceInfo POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED = new SandboxResourceInfo(FIRST_AFFECTED_POD_AGENT_VERSION - 1, emptyMap());

    protected static final YTreeNode EMPTY_LABELS = new YTreeBuilder().beginMap().endMap().build();

    protected static final TAttribute EMPTY_SOX_LABEL = TAttribute.newBuilder().setKey("any_label").setValue(ByteString.EMPTY).build();

    protected static final Optional<SecuritySettings> SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION = Optional.of(SECURITY_SETTINGS.toBuilder()
            .withDisableDefaultlyEnabledChildOnlyIsolation(false)
            .withDisableDefaultlyEnabledSecretEnv(false)
            .build());

    protected static final Optional<SecuritySettings> SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION = Optional.of(SECURITY_SETTINGS.toBuilder()
            .withDisableDefaultlyEnabledChildOnlyIsolation(true)
            .withDisableDefaultlyEnabledSecretEnv(true)
            .build());

    protected static final Optional<SecuritySettings> SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_DISABLE_FALSE_ISOLATION = Optional.of(SECURITY_SETTINGS.toBuilder()
            .withDisableDefaultlyEnabledChildOnlyIsolation(false)
            .withDisableDefaultlyEnabledSecretEnv(true)
            .build());

    protected static final Optional<SecuritySettings> SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_DISABLE_TRUE_ISOLATION = Optional.of(SECURITY_SETTINGS.toBuilder()
            .withDisableDefaultlyEnabledChildOnlyIsolation(true)
            .withDisableDefaultlyEnabledSecretEnv(false)
            .build());

    protected static final YTreeNode LABELS_WITH_ENV_SECRET = new YTreeBuilder().beginMap().key(USE_ENV_SECRET_KEY).value(true).endMap().build();

    protected static final SecurityPatcherV1Context DEFAULT_PATCHER_CONTEXT = new SecurityPatcherV1Context(
            FixedResourceSupplier.withMeta(TestData.DOWNLOADABLE_RESOURCE, POD_AGENT_BINARY_RESOURCE_META_AFFECTED),
            FIRST_AFFECTED_POD_AGENT_VERSION,
            new FoldersLayerUrls(
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            )
    );

    protected void enableSoxSecurityFeaturesTestScenario(
            boolean isSox,
            SandboxResourceMeta defaultPodAgentBinaryMeta,
            Optional<SandboxResourceInfo> userSpecifiedPodAgentBinaryInfo,
            TAttribute soxLabel,
            Optional<SecuritySettings> securitySettings,
            YTreeNode expectedLabels,
            boolean childOnlyIsolationEnabledForUserBoxes
    ) {
        String userBoxId = "user_box_id";
        TPodTemplateSpec.Builder podSpecBuilder = createPodTemplateSpecWithBoxes(ImmutableList.of(userBoxId,
                        LogbrokerPatcherUtils.LOGBROKER_BOX_ID,
                        TvmPatcherUtils.TVM_BOX_ID),
                EVolumeCreateMode.EVolumeCreateMode_READ_WRITE
        );

        var specDetails = mock(DeployUnitSpecDetails.class);
        when(specDetails.getLabels()).thenReturn(TAttributeDictionary.newBuilder()
                .addAttributes(soxLabel)
                .build()
        );

        DeployUnitSpec.Builder specBuilder = DEPLOY_UNIT_SPEC.toBuilder()
                .withDetails(specDetails)
                .withSoxService(isSox)
                .withPodAgentResourceInfo(userSpecifiedPodAgentBinaryInfo);

        securitySettings.ifPresentOrElse(s -> specBuilder.withSecuritySettings(s), () -> specBuilder.withSecuritySettings(null)/*reset security settings*/);

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(specBuilder.build());

        ResourceSupplier podAgentBinarySupplier = FixedResourceSupplier.withMeta(TestData.DOWNLOADABLE_RESOURCE, defaultPodAgentBinaryMeta);

        SecurityPatcherV1Context.Builder contextBuilder = DEFAULT_PATCHER_CONTEXT.toBuilder().withDefaultPodAgentBinarySupplier(podAgentBinarySupplier);

        var patchResult = patch(
                contextBuilder.build(),
                podSpecBuilder,
                deployUnitContext
        );

        var labels = patchResult.getLabels();

        assertThatEquals(labels, expectedLabels);

        assertThatEquals(boxIsolationChildOnly(podSpecBuilder, userBoxId), childOnlyIsolationEnabledForUserBoxes);

        assertThatEquals(boxIsolationChildOnly(podSpecBuilder, LogbrokerPatcherUtils.LOGBROKER_BOX_ID), false);
        assertThatEquals(boxIsolationChildOnly(podSpecBuilder, TvmPatcherUtils.TVM_BOX_ID), false);
    }

    @Test
    void addPodAgentPortoshellAndTmpFoldersLayersAndVolumesTest() {
        String userBoxId = "user_box_id";
        String userWorkloadId = "user_workload_id";
        var deployUnitSpec = DEPLOY_UNIT_SPEC
                .withPodAgentResourceInfo(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED);


        var podAgentSpec = performPatch(userBoxId, userWorkloadId, deployUnitSpec, false);

        TBox box = podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().get();

        assertLayerAddedToBox(String.format("%s_", POD_AGENT_FOLDERS_LAYER_BASE_ID), box, podAgentSpec);
        assertLayerAddedToBox(String.format("%s_", PORTOSHELL_FOLDERS_LAYER_BASE_ID), box, podAgentSpec);
        assertVolumeMountedToBox(String.format("%s_%s", PORTOSHELL_HOMES_VOLUME_BASE_ID, userBoxId), PORTOSHELL_HOMES_VOLUME_MOUNT_POINT, box, podAgentSpec);
        assertVolumeMountedToBox(String.format("%s_%s", TMP_VOLUME_BASE_ID, userBoxId), TMP_VOLUME_MOUNT_POINT, box, podAgentSpec);
        checkClearCommandContainsFolders(ImmutableList.of(PORTOSHELL_HOMES_VOLUME_MOUNT_POINT, TMP_VOLUME_MOUNT_POINT), box, 0);
    }

    @Test
    void addLogbrokerFoldersLayerTest() {
        String userBoxId = "user_box_id";
        String userWorkloadId = "user_workload_id";
        var deployUnitSpec = DEPLOY_UNIT_SPEC.toBuilder()
                .withPodAgentResourceInfo(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED)
                .withLogbrokerConfig(LOGBROKER_CONFIG.toBuilder()
                        .withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY)
                        .build()
                ).build();

        var podAgentSpec = performPatch(userBoxId, userWorkloadId, deployUnitSpec, false);

        TBox box = podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().get();

        assertLayerAddedToBox(String.format("%s_", LOGBROKER_FOLDERS_LAYER_BASE_ID), box, podAgentSpec);
        checkClearCommandContainsFolders(ImmutableList.of(PORTOSHELL_HOMES_VOLUME_MOUNT_POINT, TMP_VOLUME_MOUNT_POINT), box, 0);
    }

    @Test
    void addCoredumpFoldersLayerTest() {
        String userBoxId = "user_box_id";
        String userWorkloadId = "user_workload_id";
        var deployUnitSpec = DEPLOY_UNIT_SPEC.toBuilder()
                .withPodAgentResourceInfo(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED)
                .withCoredumpConfig(ImmutableMap.of(userWorkloadId, COREDUMP_CONFIG_WITH_AGGREGATION))
                .build();

        var podAgentSpec = performPatch(userBoxId, userWorkloadId, deployUnitSpec, false);

        TBox box = podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().get();

        assertLayerAddedToBox(String.format("%s_", COREDUMP_FOLDERS_LAYER_BASE_ID), box, podAgentSpec);
        checkClearCommandContainsFolders(ImmutableList.of(PORTOSHELL_HOMES_VOLUME_MOUNT_POINT, TMP_VOLUME_MOUNT_POINT), box, 0);
    }

    @Test
    void addJugglerFoldersLayerAndVolumesWithJugglerTest() {
        String userBoxId = "user_box_id";
        String userWorkloadId = "user_workload_id";

        var boxJugglerConfigs = ImmutableMap.of(
                userBoxId,
                new BoxJugglerConfig(
                        List.of(DOWNLOADABLE_RESOURCE),
                        OptionalInt.empty(),
                        Optional.empty()
                )
        );

        var deployUnitSpec = DEPLOY_UNIT_SPEC.toBuilder()
                .withPodAgentResourceInfo(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED)
                .withBoxJugglerConfigs(boxJugglerConfigs)
                .build();

        var podAgentSpec = performPatch(userBoxId, userWorkloadId, deployUnitSpec, false);

        TBox box = podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().get();

        assertLayerAddedToBox(String.format("%s_", JUGGLER_FOLDERS_LAYER_BASE_ID), box, podAgentSpec);
        assertVolumeMountedToBox(String.format("%s_%s", JUGGLER_LOGS_VOLUME_BASE_ID, userBoxId), JUGGLER_LOGS_VOLUME_MOUNT_POINT, box, podAgentSpec);
        assertVolumeMountedToBox(String.format("%s_%s", JUGGLER_SCHEDULER_VOLUME_BASE_ID, userBoxId), JUGGLER_SCHEDULER_VOLUME_MOUNT_POINT, box, podAgentSpec);
        assertVolumeMountedToBox(String.format("%s_%s", JUGGLER_STATE_VOLUME_BASE_ID, userBoxId), JUGGLER_STATE_VOLUME_MOUNT_POINT, box, podAgentSpec);
        assertVolumeMountedToBox(String.format("%s_%s", JUGGLER_CHECKS_VOLUME_BASE_ID, userBoxId), JUGGLER_CHECKS_VOLUME_MOUNT_POINT, box, podAgentSpec);

        checkClearCommandContainsFolders(ImmutableList.of(
                PORTOSHELL_HOMES_VOLUME_MOUNT_POINT,
                TMP_VOLUME_MOUNT_POINT,
                JUGGLER_LOGS_VOLUME_MOUNT_POINT,
                JUGGLER_SCHEDULER_VOLUME_MOUNT_POINT,
                JUGGLER_STATE_VOLUME_MOUNT_POINT,
                JUGGLER_CHECKS_VOLUME_MOUNT_POINT
        ), box, 0);
    }

    @Test
    void addLogrotateFolderLayerTest() {
        String userBoxId = "user_box_id";
        String userWorkloadId = "user_workload_id";
        var deployUnitSpec = DEPLOY_UNIT_SPEC.toBuilder()
                .withPodAgentResourceInfo(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED)
                .withLogrotateConfig(ImmutableMap.of(userBoxId, new LogrotateConfig("", 0)))
                .build();

        var podAgentSpec = performPatch(userBoxId, userWorkloadId, deployUnitSpec, false);

        TBox box = podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().get();

        assertLayerAddedToBox(String.format("%s_", LOGROTATE_FOLDERS_LAYER_BASE_ID), box, podAgentSpec);
        checkClearCommandContainsFolders(ImmutableList.of(PORTOSHELL_HOMES_VOLUME_MOUNT_POINT, TMP_VOLUME_MOUNT_POINT), box, 0);
    }

    @Test
    void addLogrotateInitScriptForLogsDirCreationTest() {
        String userBoxId = "user_box_id";
        String userWorkloadId = "user_workload_id";
        var deployUnitSpec = DEPLOY_UNIT_SPEC.toBuilder()
                .withPodAgentResourceInfo(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED)
                .withLogrotateConfig(ImmutableMap.of(userBoxId, new LogrotateConfig("", 0)))
                .build();

        var podAgentSpec = performPatch(userBoxId, userWorkloadId, deployUnitSpec, true);

        TBox box = podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().get();

        String expectedInitScriptLogsDirCreation = "bash -c 'mkdir -p " + LogrotatePatcherV1Base.LOG_ROTATE_LOG_MOUNT_POINT
                + "; chown loadbase:loadbase " + LogrotatePatcherV1Base.LOG_ROTATE_LOG_MOUNT_POINT + "'";

        assertThatEquals(box.getInit(0).getCommandLine(), expectedInitScriptLogsDirCreation);
        checkClearCommandContainsFolders(ImmutableList.of(PORTOSHELL_HOMES_VOLUME_MOUNT_POINT, TMP_VOLUME_MOUNT_POINT), box, 1);
    }

    private TPodAgentSpec performPatch(String userBoxId, String userWorkloadId, DeployUnitSpec deployUnitSpec, boolean specHasVarLogVolume) {
        TPodTemplateSpec.Builder podSpecBuilder = createPodTemplateSpecWithBoxes(ImmutableList.of(userBoxId),
                EVolumeCreateMode.EVolumeCreateMode_READ_ONLY
        );

        TPodAgentSpec.Builder podAgentSpec = podSpecBuilder.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder();

        podAgentSpec.addWorkloads(TWorkload.newBuilder()
                .setId(userWorkloadId)
                .setBoxRef(userBoxId));

        if (specHasVarLogVolume) {
            String varLogVolumeId = "var_log_volume_id";
            podAgentSpec.addVolumes(TVolume.newBuilder()
                    .setId(varLogVolumeId))
                    .getBoxesBuilder(0)
                    .addVolumes(TMountedVolume.newBuilder()
                            .setVolumeRef(varLogVolumeId)
                            .setMountPoint("/var/log").build()
                    );
        }

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(deployUnitSpec);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podSpecBuilder,
                deployUnitContext
        );

        return patchResult.getPodAgentSpec();
    }

    private static void checkClearCommandContainsFolders(List<String> folders, TBox box, int initScriptNum) {
        assertThatEquals(box.getInit(initScriptNum).getCommandLine(), createRemoveFoldersContentCommand(folders));
    }

    private static void assertLayerAddedToBox(String layerId, TBox box, TPodAgentSpec spec) {
        assertThatEquals(box.getRootfs().getLayerRefsList().stream().anyMatch(l -> l.equals(layerId)), true);
        assertThatEquals(spec.getResources().getLayersList().stream().anyMatch(l -> l.getId().equals(layerId)), true);
    }

    private static void assertVolumeMountedToBox(String volId, String mountPoint, TBox box, TPodAgentSpec spec) {
        assertThatEquals(box.getVolumesList().stream().anyMatch(v -> v.getVolumeRef().equals(volId) && v.getMountPoint().equals(mountPoint)), true);
        assertThatEquals(spec.getVolumesList().stream().anyMatch(v -> v.getId().equals(volId)), true);
    }

    private static TPodTemplateSpec.Builder createPodTemplateSpecWithBoxes(
            ImmutableList<String> boxIds,
            EVolumeCreateMode rootfsCreateMode
    ) {
        TPodTemplateSpec.Builder spec = TPodTemplateSpec.newBuilder();
        spec.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addAllBoxes(boxIds
                        .stream()
                        .map(id -> TBox.newBuilder()
                                .setId(id)
                                .setRootfs(TRootfsVolume.newBuilder()
                                        .setCreateMode(rootfsCreateMode)
                                        .build())
                                .build())
                        .collect(Collectors.toList()));
        return spec;
    }

    private static boolean boxIsolationChildOnly(TPodTemplateSpec.Builder podSpecBuilder, String boxId) {
       return podSpecBuilder
               .getSpec()
               .getPodAgentPayload()
               .getSpec()
               .getBoxesList()
               .stream()
               .filter(b -> b.getId().equals(boxId))
               .anyMatch(b -> b.getIsolationMode() == EContainerIsolationMode.EContainerIsolationMode_CHILD_ONLY);
    }
}
