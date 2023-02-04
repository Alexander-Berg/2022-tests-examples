package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.LogbrokerDestroyPolicy;
import ru.yandex.infra.stage.dto.LogbrokerTopicConfig;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.dto.SecretRef;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SandboxReleaseGetter;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.podspecs.SidecarDiskVolumeDescription;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfig;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactory;
import ru.yandex.infra.stage.protobuf.Converter;
import ru.yandex.infra.stage.util.AssertUtils;
import ru.yandex.infra.stage.util.NamedArgument;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.ETransmitSystemLogs;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TMountedVolume;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TTransmitSystemLogsPolicy;
import ru.yandex.yp.client.pods.TUtilityContainer;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.DEFAULT_LOGBROKER_TOPIC_CONFIG;
import static ru.yandex.infra.stage.TestData.DEFAULT_LOGBROKER_TOPIC_REQUEST;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_RESOURCE_INFO;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_REVISION;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST;
import static ru.yandex.infra.stage.TestData.LOGBROKER_CONFIG;
import static ru.yandex.infra.stage.TestData.LOGBROKER_TOPIC_DESCRIPTION;
import static ru.yandex.infra.stage.TestData.NOT_USED_BY_INFRA_VOLUME_REQUEST;
import static ru.yandex.infra.stage.TestData.POD_SPEC;
import static ru.yandex.infra.stage.TestData.POD_SPEC_MULTIPLE_USER_DISKS;
import static ru.yandex.infra.stage.TestData.USED_BY_INFRA_VOLUME_REQUEST;
import static ru.yandex.infra.stage.dto.AllComputeResourcesTest.DEFAULT_RESOURCES;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.groupById;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.hasLiteralEnv;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.hasSecret;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.testIds;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.MEGABYTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.SYSTEM_BOX_SPECIFIC_TYPE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.DEPLOY_LOGNAME_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.DEPLOY_LOGS_ENDPOINT_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.DEPLOY_LOGS_SECRET_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.ERROR_BOOSTER_HTTP_HOST;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.ERROR_BOOSTER_HTTP_PORT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.ERROR_BOOSTER_SENTRY_DSN;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.ERROR_BOOSTER_SENTRY_DSN_HARDCODE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.ERROR_BOOSTER_SYSLOG_HOST;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.ERROR_BOOSTER_SYSLOG_PORT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOCALHOST;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_AGENT_WORKLOAD_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_DEFAULT_STATIC_SECRET;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_THREAD_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_TOOLS_LAYER_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.MAX_LOGBROKER_BOX_VCPU;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.DELIVERY_ENABLED_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.DELIVERY_ENABLED_VALUE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.DELIVERY_TVM_DST_CLIENT_ID_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGBROKER_AGENT_CONF_RESOURCE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGBROKER_TOOLS_HDD_CAPACITY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGBROKER_UNIFIED_AGENT_CONF_RESOURCE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGBROKER_USE_NEW_PROTOCOL_FLAG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGNAME_LABEL_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGS_ENDPOINT_LABEL_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGS_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.PUSH_AGENT_CONFIG_COPY_CMD;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.STDERR_LOG_TYPE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.STDOUT_LOG_TYPE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.UNIFIED_AGENT_CONFIG_COPY_CMD;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.UNIFIED_AGENT_NEW_PROTOCOL_SUPPORTED_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.UNIFIED_AGENT_NEW_PROTOCOL_SUPPORTED_VALUE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.UNIFIED_AGENT_VERSION_LABEL_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryTest.DEFAULT_UNIFIED_AGENT_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryTest.createUnifiedAgentConfigFactoryMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryTest.generateDifferentUnifiedAgentConfigArguments;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigV2.SYSLOG_HTTP_PORT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigV2.SYSLOG_INPUT_PORT;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.JsonUtils.jsonToYaml;

abstract class LogbrokerPatcherV1BaseTest extends PatcherTestBase<LogbrokerPatcherV1Context> {

    protected static final AllComputeResources DEFAULT_LOGBROKER_BOX_COMPUTING_RESOURCES_FROM_V2_TO_LAST =
            new AllComputeResources(400, 512 * MEGABYTE, 448 * MEGABYTE, LOGBROKER_TOOLS_HDD_CAPACITY,
                    LOGBROKER_THREAD_LIMIT);

    protected static final String EXPECTED_PATCHER_THROTTLING_LIMITS_KEY_FROM_V1_TO_V2 = "limits_from_patcher_v1_to_v2";
    private static final String EXPECTED_PATCHER_THROTTLING_LIMITS_KEY_FROM_V3_TO_LAST =
            "limits_from_patcher_v3_to_last";

    private static final ResourceSupplier DEFAULT_LOGBROKER_AGENT_LAYER_SUPPLIER =
            FixedResourceSupplier.withMeta(TestData.DOWNLOADABLE_RESOURCE, TestData.RESOURCE_META);

    private static final int DEFAULT_RELEASE_GETTER_TIMEOUT_SECONDS = 5;

    private static final boolean DEFAULT_USE_PATCH_BOX_SPECIFIC_TYPE = false;

    private static final LogbrokerBoxResourcesConfig DEFAULT_BOX_RESOURCES_CONFIG =
            new LogbrokerBoxResourcesConfig(emptyMap());


    private static final UnifiedAgentConfigFactory DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY =
            createUnifiedAgentConfigFactoryMock(DEFAULT_UNIFIED_AGENT_CONFIG);

    private static final LogbrokerPatcherV1Context DEFAULT_PATCHER_CONTEXT =
            new LogbrokerPatcherV1Context(DEFAULT_LOGBROKER_AGENT_LAYER_SUPPLIER, Optional.empty(), Optional.empty(),
                    DEFAULT_USE_PATCH_BOX_SPECIFIC_TYPE, DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY,
                    DEFAULT_BOX_RESOURCES_CONFIG, DEFAULT_RELEASE_GETTER_TIMEOUT_SECONDS);

    private static final long DEFAULT_EXPECTED_SANDBOX_RESOURCE_TASK_ID = TestData.TASK_ID;

    private static final Checksum DEFAULT_EXPECTED_SANDBOX_RESOURCE_CHECKSUM = new Checksum("logbrokerPatcherTest",
            Checksum.Type.MD5);

    private static final SandboxResourceInfo DEFAULT_LOGBROKER_TOOLS_SIDECAR_INFO = DEFAULT_SIDECAR_RESOURCE_INFO;

    private static final Optional<SandboxResourceMeta> DEFAULT_LOGBROKER_DEFAULT_BINARY_RESOURCE_META =
            Optional.of(TestData.RESOURCE_META);

    private static final List<TLayer> DEFAULT_POD_AGENT_PAYLOAD_LAYERS = emptyList();

    private static final String DEFAULT_USER_BOX_ID = "box_id";
    private static final String DEFAULT_USER_WORKLOAD_ID = "workload_id";
    private static final String DEFAULT_LOGBROKER_DISK_ALLOCATION_ID = "logbroker_allocation_id";

    protected AllComputeResources getDefaultLogbrokerBoxComputingResources() {
        return DEFAULT_LOGBROKER_BOX_COMPUTING_RESOURCES_FROM_V2_TO_LAST;
    }

    @Test
    void useLogbrokerToolsTest() {
        String boxId = DEFAULT_USER_BOX_ID;
        String workloadId = DEFAULT_USER_WORKLOAD_ID;

        TPodAgentSpec spec1 =
                createPodTemplateSpecBuilder(boxId, workloadId, false, Optional.empty()).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec2 =
                createPodTemplateSpecBuilder(boxId, workloadId, true, Optional.empty()).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec3 = createPodTemplateSpecBuilder(boxId, workloadId, false,
                Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_NONE)).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec4 = createPodTemplateSpecBuilder(boxId, workloadId, false,
                Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_DISABLED)).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec5 = createPodTemplateSpecBuilder(boxId, workloadId, false,
                Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_ENABLED)).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec6 = createPodTemplateSpecBuilder(boxId, workloadId, true,
                Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_NONE)).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec7 = createPodTemplateSpecBuilder(boxId, workloadId, true,
                Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_DISABLED)).getSpec().getPodAgentPayload().getSpec();
        TPodAgentSpec spec8 = createPodTemplateSpecBuilder(boxId, workloadId, true,
                Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_ENABLED)).getSpec().getPodAgentPayload().getSpec();

        LogbrokerConfig logbrokerConfig1 =
                LOGBROKER_CONFIG.toBuilder().withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.UNKNOWN).build();
        LogbrokerConfig logbrokerConfig2 =
                LOGBROKER_CONFIG.toBuilder().withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.DEFAULT).build();
        LogbrokerConfig logbrokerConfig3 =
                LOGBROKER_CONFIG.toBuilder().withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY).build();

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec1, logbrokerConfig1), equalTo(false));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec1, logbrokerConfig2), equalTo(false));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec1, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec2, logbrokerConfig1), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec2, logbrokerConfig2), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec2, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec3, logbrokerConfig1), equalTo(false));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec3, logbrokerConfig2), equalTo(false));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec3, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec4, logbrokerConfig1), equalTo(false));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec4, logbrokerConfig2), equalTo(false));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec4, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec5, logbrokerConfig1), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec5, logbrokerConfig2), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec5, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec6, logbrokerConfig1), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec6, logbrokerConfig2), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec6, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec7, logbrokerConfig1), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec7, logbrokerConfig2), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec7, logbrokerConfig3), equalTo(true));

        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec8, logbrokerConfig1), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec8, logbrokerConfig2), equalTo(true));
        assertThat(LogbrokerPatcherUtils.useLogbrokerTools(spec8, logbrokerConfig3), equalTo(true));
    }

    @Test
    void getPortoLogsVolumeDiskIdWhenOneUserDisk() {
        portoLogsDiskIdTestScenario(POD_SPEC, Optional.empty(), USED_BY_INFRA_VOLUME_REQUEST.getId());
    }

    @Test
    void getPortoLogsVolumeDiskIdWhenMultipleUserDisks() {
        portoLogsDiskIdTestScenario(POD_SPEC_MULTIPLE_USER_DISKS,
                Optional.of(NOT_USED_BY_INFRA_VOLUME_REQUEST.getId()), NOT_USED_BY_INFRA_VOLUME_REQUEST.getId());
    }

    @Test
    void addLogbrokerSidecarDiskWhenOneUserDisk() {
        Optional<SidecarVolumeSettings> volSettings1 = Optional.empty();
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        Optional<SidecarVolumeSettings> volSettings3 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings4 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        addLogbrokerSidecarDiskScenario(POD_SPEC, volSettings1, USED_BY_INFRA_VOLUME_REQUEST.getStorageClass());
        addLogbrokerSidecarDiskScenario(POD_SPEC, volSettings2, USED_BY_INFRA_VOLUME_REQUEST.getStorageClass());
        addLogbrokerSidecarDiskScenario(POD_SPEC, volSettings3, SidecarDiskVolumeDescription.HDD_STORAGE_CLASS);
        addLogbrokerSidecarDiskScenario(POD_SPEC, volSettings4, SidecarDiskVolumeDescription.SSD_STORAGE_CLASS);
    }

    @Test
    void addLogbrokerSidecarDiskWhenMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings1 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        addLogbrokerSidecarDiskScenario(POD_SPEC_MULTIPLE_USER_DISKS, volSettings1,
                SidecarDiskVolumeDescription.HDD_STORAGE_CLASS);
        addLogbrokerSidecarDiskScenario(POD_SPEC_MULTIPLE_USER_DISKS, volSettings2,
                SidecarDiskVolumeDescription.SSD_STORAGE_CLASS);
    }

    @Test
    void addLogbrokerLabels() {
        var deployUnitContext = DEFAULT_UNIT_CONTEXT;
        var labels =
                patch(DEFAULT_PATCHER_CONTEXT, createPodTemplateSpecBuilderWithLogs(), deployUnitContext).getLabels();
        assertThatEquals(createLogbrokerLabelsAttribute(deployUnitContext), labels);
    }

    @Test
    void notAddLabelsIfLogsAreAbsent() {
        var labels = patch(DEFAULT_PATCHER_CONTEXT, createPodSpecBuilder(), DEFAULT_UNIT_CONTEXT).getLabels();
        assertThat("labels should be absent", labels.getAttributes().isEmpty());
    }

    @Test
    void addLogbrokerEnvironment() {

        String logbrokerDiskVolumeAllocationId = DEFAULT_LOGBROKER_DISK_ALLOCATION_ID;
        Set<String> diskVolumeRequestInSpec = ImmutableSet.of(logbrokerDiskVolumeAllocationId);
        String logbrokerVirtualDiskRefInSpec = logbrokerDiskVolumeAllocationId;
        String rootVirtualDiskRefInSpec = USED_BY_INFRA_VOLUME_REQUEST.getId();

        addLogbrokerEnvironmentScenario(false, logbrokerDiskVolumeAllocationId, diskVolumeRequestInSpec,
                logbrokerVirtualDiskRefInSpec, rootVirtualDiskRefInSpec);
    }

    @Test
    void addLogbrokerEnvironmentWhenDisableDiskIsolationLabelInSpec() {

        String logbrokerDiskVolumeAllocationId = DEFAULT_LOGBROKER_DISK_ALLOCATION_ID;
        Set<String> diskVolumeRequestInSpec = ImmutableSet.of();
        String logbrokerVirtualDiskRefInSpec = "";
        String rootVirtualDiskRefInSpec = "";

        addLogbrokerEnvironmentScenario(true, logbrokerDiskVolumeAllocationId, diskVolumeRequestInSpec,
                logbrokerVirtualDiskRefInSpec, rootVirtualDiskRefInSpec);
    }

    @Test
    void addDefaultStaticSecretToUserWorkloadEnvWhenLogsTransmitDisabled() {
        String workloadId = DEFAULT_USER_WORKLOAD_ID;

        TPodTemplateSpec.Builder templateSpecBuilder = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID, workloadId,
                false, Optional.empty());

        var patcherContext =
                DEFAULT_PATCHER_CONTEXT.toBuilder().withDiskVolumeAllocationId("allocation_id").withPatchBoxSpecificType(true).build();

        var podAgentSpec = patch(patcherContext, templateSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TWorkload userWorkload =
                podAgentSpec.getWorkloadsList().stream().filter(w -> w.getId().equals(workloadId)).findFirst().orElseThrow();

        assertThat(hasLiteralEnv(userWorkload, DEPLOY_LOGS_SECRET_ENV_NAME, LOGBROKER_DEFAULT_STATIC_SECRET),
                equalTo(true));
    }

    @Test
    void notOverwriteStaticSecretEnvValueInUserWorkloadWhenLogsTransmitDisabled() {
        String workloadId = DEFAULT_USER_WORKLOAD_ID;
        String logbrokerCustomStaticSecret = "custom_static_secret_value";

        TPodTemplateSpec.Builder templateSpecBuilder = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID, workloadId,
                false, Optional.empty());

        TPodAgentSpec.Builder podAgentSpecBuilder =
                templateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();

        podAgentSpecBuilder.getWorkloadsBuilderList().forEach(workloadBuilder -> {
            workloadBuilder.addEnv(PodSpecUtils.literalEnvVar(DEPLOY_LOGS_SECRET_ENV_NAME,
                    logbrokerCustomStaticSecret));
            workloadBuilder.build();
        });

        var patcherContext =
                DEFAULT_PATCHER_CONTEXT.toBuilder().withDiskVolumeAllocationId(DEFAULT_LOGBROKER_DISK_ALLOCATION_ID).withPatchBoxSpecificType(true).build();

        var podAgentSpec = patch(patcherContext, templateSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TWorkload userWorkload =
                podAgentSpec.getWorkloadsList().stream().filter(w -> w.getId().equals(workloadId)).findFirst().orElseThrow();

        assertThat(hasLiteralEnv(userWorkload, DEPLOY_LOGS_SECRET_ENV_NAME, logbrokerCustomStaticSecret),
                equalTo(true));
    }

    private static TPodTemplateSpec.Builder createPodTemplateSpecBuilder(String userBoxId, String workloadId,
                                                                         boolean withUserLogs,
                                                                         Optional<ETransmitSystemLogs> transmitSystemLogs) {
        return createPodTemplateSpecBuilder(userBoxId, workloadId, withUserLogs, transmitSystemLogs,
                ImmutableList.of(USED_BY_INFRA_VOLUME_REQUEST));
    }

    private static TPodTemplateSpec.Builder createPodTemplateSpecBuilder(String userBoxId, String workloadId,
                                                                         boolean withUserLogs,
                                                                         Optional<ETransmitSystemLogs> transmitSystemLogs, List<DataModel.TPodSpec.TDiskVolumeRequest> userAllocations) {
        TPodTemplateSpec.Builder templateSpecBuilder = createPodSpecBuilder();

        DataModel.TPodSpec.Builder podSpecBuilder = templateSpecBuilder.getSpecBuilder();
        userAllocations.forEach(podSpecBuilder::addDiskVolumeRequests);

        TBox box = TBox.newBuilder().setId(userBoxId).build();

        DataModel.TPodSpec.TPodAgentPayload.Builder agentPayloadBuilder = podSpecBuilder.getPodAgentPayloadBuilder();
        agentPayloadBuilder.getSpecBuilder().addAllWorkloads(ImmutableList.of(TestData.createWorkload(workloadId,
                userBoxId, withUserLogs))).addBoxes(box);
        transmitSystemLogs.ifPresent(t -> agentPayloadBuilder.getSpecBuilder().setTransmitSystemLogsPolicy(TTransmitSystemLogsPolicy.newBuilder().setTransmitSystemLogs(t).build()));

        return templateSpecBuilder;
    }

    private static YTreeNode createLogbrokerLabelsAttribute(DeployUnitContext deployUnitContext) {
        var logbrokerTopicConfig = deployUnitContext.getLogbrokerTopicConfig().orElseThrow();
        var deliveryTvmDstClientId = String.valueOf(logbrokerTopicConfig.getTopicDescription().getTvmClientId());

        YTreeBuilder nodeBuilder =
                new YTreeBuilder().beginMap().key(DELIVERY_ENABLED_KEY).value(DELIVERY_ENABLED_VALUE).key(DELIVERY_TVM_DST_CLIENT_ID_KEY).value(deliveryTvmDstClientId).key(LOGNAME_LABEL_KEY).value(DEFAULT_STAGE_CONTEXT.getStageId()).key(LOGS_ENDPOINT_LABEL_KEY).value(LogbrokerPatcherV1Base.logEndpoint()).key(UNIFIED_AGENT_NEW_PROTOCOL_SUPPORTED_KEY).value(UNIFIED_AGENT_NEW_PROTOCOL_SUPPORTED_VALUE);

        return new YTreeBuilder().beginMap().key(LOGS_KEY).value(nodeBuilder.endMap().build()).endMap().build();
    }

    @Test
    void customLogbrokerAgentTest() {
        var logbrokerConfig =
                LOGBROKER_CONFIG.toBuilder().withLogbrokerAgentLayer(Optional.of(TestData.DOWNLOADABLE_RESOURCE2)).build();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withLogbrokerConfig(logbrokerConfig);

        var podAgentSpec =
                patch(DEFAULT_PATCHER_CONTEXT, createPodTemplateSpecBuilderWithLogs(), deployUnitContext).getPodAgentSpec();

        var actualLogbrokerAgentResourceLayerUrl =
                podAgentSpec.getResources().getLayersList().stream().filter(layer -> layer.getId().equals(LOGBROKER_TOOLS_LAYER_ID)).findAny().orElseThrow().getUrl();

        assertThatEquals(actualLogbrokerAgentResourceLayerUrl, TestData.DOWNLOADABLE_RESOURCE2.getUrl());
    }

    protected String getExpectedPatcherThrottlingLimitsKey() {
        return EXPECTED_PATCHER_THROTTLING_LIMITS_KEY_FROM_V3_TO_LAST;
    }

    protected String getLogbrokerBoxId() {
        return LogbrokerPatcherUtils.LOGBROKER_BOX_ID;
    }

    protected boolean errorBoosterEnvironmentsExport() {
        return true;
    }

    protected boolean errorBoosterHttpEnvironmentsExport() {
        return true;
    }

    protected boolean autoEnableSystemLogsWhenUserLogsEnabled() {
        return true;
    }

    @Test
    void unifiedAgentConfigFactoryArgumentsTest() {
        var unifiedAgentConfigFactory = createUnifiedAgentConfigFactoryMock(DEFAULT_UNIFIED_AGENT_CONFIG);

        var initialPatcherContext =
                DEFAULT_PATCHER_CONTEXT.toBuilder().withUnifiedAgentConfigFactory(unifiedAgentConfigFactory).build();

        new UnifiedAgentPatchScenarioBuilder().setInitialPatcherContext(initialPatcherContext).buildAndPatch();

        verify(unifiedAgentConfigFactory).build(anyCollection(), // layers are unpredictable
                eq(DEFAULT_LOGBROKER_DEFAULT_BINARY_RESOURCE_META),
                eq(Optional.of(DEFAULT_LOGBROKER_TOOLS_SIDECAR_INFO)), eq(DEFAULT_LOGBROKER_TOPIC_REQUEST),
                any(ResourceSupplier.class), eq(DEFAULT_UNIT_CONTEXT.getStageContext().getStageId()),
                eq(DEFAULT_UNIT_CONTEXT.getDeployUnitId()), eq(DEFAULT_UNIT_CONTEXT.getFullDeployUnitId()),
                eq(DEFAULT_UNIT_CONTEXT.getSpec().getDetails().extractClusters()), eq(DEFAULT_LOGBROKER_TOPIC_CONFIG)
                , eq(getExpectedPatcherThrottlingLimitsKey()), eq(LOGBROKER_DEFAULT_STATIC_SECRET));
    }

    private static Stream<NamedArgument<UnifiedAgentConfig>> unifiedAgentConfigFactoryUsedTestParameters() {
        return generateDifferentUnifiedAgentConfigArguments();
    }

    @ParameterizedTest
    @MethodSource("unifiedAgentConfigFactoryUsedTestParameters")
    void unifiedAgentConfigFactoryUsedTest(NamedArgument<UnifiedAgentConfig> expectedUnifiedAgentConfig) {
        var unifiedAgentConfigFactory = createUnifiedAgentConfigFactoryMock(expectedUnifiedAgentConfig.getArgument());

        var initialPatcherContext =
                DEFAULT_PATCHER_CONTEXT.toBuilder().withUnifiedAgentConfigFactory(unifiedAgentConfigFactory).build();

        var scenarioBuilder = new UnifiedAgentPatchScenarioBuilder().setInitialPatcherContext(initialPatcherContext);

        unifiedAgentConfigScenario(scenarioBuilder, expectedUnifiedAgentConfig.getArgument());
    }

    private void unifiedAgentConfigScenario(UnifiedAgentPatchScenarioBuilder scenarioBuilder,
                                            UnifiedAgentConfig expectedUnifiedAgentConfig) {
        var patchResult = scenarioBuilder.buildAndPatch();

        var podAgentSpec = patchResult.getPodAgentSpec();

        String actualUnifiedAgentConfigResourceUrl =
                podAgentSpec.getResources().getStaticResourcesList().stream().filter(l -> l.getId().equals(LOGBROKER_UNIFIED_AGENT_CONF_RESOURCE_ID)).map(TResource::getUrl).findAny().orElseThrow();

        String expectedUnifiedAgentConfigResourceUrl = String.format("raw:%s",
                jsonToYaml(expectedUnifiedAgentConfig.toJsonString()));

        assertThatEquals(actualUnifiedAgentConfigResourceUrl, expectedUnifiedAgentConfigResourceUrl);
    }

    @Test
    void unifiedAgentChecksumTest() {
        var patchResult = new UnifiedAgentPatchScenarioBuilder().buildAndPatch();

        var podAgentSpec = patchResult.getPodAgentSpec();

        String actualChecksum =
                podAgentSpec.getResources().getLayersList().stream().filter(l -> LOGBROKER_TOOLS_LAYER_ID.equals(l.getId())).map(TLayer::getChecksum).findAny().orElseThrow();

        assertThatEquals(actualChecksum, DEFAULT_EXPECTED_SANDBOX_RESOURCE_CHECKSUM.toAgentFormat());
    }

    // TODO parameterize with version and without
    @Test
    void unifiedAgentVersionTest() {
        var expectedUnifiedAgentVersion = "20.5.6";

        var patchResult =
                new UnifiedAgentPatchScenarioBuilder().setResourceFromSpecMeta(ImmutableMap.of(UNIFIED_AGENT_VERSION_LABEL_KEY, expectedUnifiedAgentVersion)).buildAndPatch();

        var labels = patchResult.getLabels();

        var actualUnifiedAgentVersion = labels.asMap().get(UNIFIED_AGENT_VERSION_LABEL_KEY).stringValue();

        assertThatEquals(actualUnifiedAgentVersion, expectedUnifiedAgentVersion);
    }

    private static Optional<SandboxResourceMeta> createResourceMeta(Map<String, String> metaAttributes) {
        var sandboxResourceMeta = new SandboxResourceMeta(DEFAULT_EXPECTED_SANDBOX_RESOURCE_TASK_ID, 2L,
                metaAttributes);

        return Optional.of(sandboxResourceMeta);
    }

    private class UnifiedAgentPatchScenarioBuilder {

        private LogbrokerPatcherV1Context initialPatcherContext;
        private Optional<SandboxResourceMeta> resourceFromSpecMeta;
        private Optional<SandboxResourceMeta> defaultSandboxResourceMeta;

        UnifiedAgentPatchScenarioBuilder() {
            this.initialPatcherContext = DEFAULT_PATCHER_CONTEXT;
            this.resourceFromSpecMeta = DEFAULT_LOGBROKER_DEFAULT_BINARY_RESOURCE_META;
            this.defaultSandboxResourceMeta = DEFAULT_LOGBROKER_DEFAULT_BINARY_RESOURCE_META;
        }

        UnifiedAgentPatchScenarioBuilder setInitialPatcherContext(LogbrokerPatcherV1Context initialPatcherContext) {
            this.initialPatcherContext = initialPatcherContext;
            return this;
        }

        UnifiedAgentPatchScenarioBuilder setResourceFromSpecMeta(Map<String, String> metaAttributes) {
            return setResourceFromSpecMeta(createResourceMeta(metaAttributes));
        }

        UnifiedAgentPatchScenarioBuilder setResourceFromSpecMeta(Optional<SandboxResourceMeta> resourceFromSpecMeta) {
            this.resourceFromSpecMeta = resourceFromSpecMeta;
            return this;
        }

        private PatchResult buildAndPatch() {
            String logbrokerDiskVolumeAllocationId = DEFAULT_LOGBROKER_DISK_ALLOCATION_ID;

            FixedResourceSupplier logbrokerAgentLayerSupplier = createLogbrokerAgentLayerSupplier();

            var deployUnitContext =
                    DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(DeployUnitSpec::withLogbrokerToolsResourceInfo,
                            DEFAULT_LOGBROKER_TOOLS_SIDECAR_INFO);

            var patcherContext =
                    initialPatcherContext.toBuilder().withLogbrokerAgentLayerSupplier(logbrokerAgentLayerSupplier).withDiskVolumeAllocationId(logbrokerDiskVolumeAllocationId).withAllSidecarDiskAllocationIds(ImmutableList.of(logbrokerDiskVolumeAllocationId)).withPatchBoxSpecificType(true).build();

            var podTemplateSpecBuilder = createPodTemplateSpecBuilderWithLogs();

            podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder().getResourcesBuilder().clearLayers().addAllLayers(DEFAULT_POD_AGENT_PAYLOAD_LAYERS);

            return patch(patcherContext, podTemplateSpecBuilder, deployUnitContext);
        }

        private FixedResourceSupplier createLogbrokerAgentLayerSupplier() {
            var sandboxReleaseGetter = mock(SandboxReleaseGetter.class);
            var downloadableResource = new DownloadableResource("getInfraComponentFromSpec",
                    DEFAULT_EXPECTED_SANDBOX_RESOURCE_CHECKSUM);

            var resourceWithMetaCompletableFuture =
                    CompletableFuture.completedFuture(new ResourceWithMeta(downloadableResource, resourceFromSpecMeta));

            Mockito.doReturn(resourceWithMetaCompletableFuture).when(sandboxReleaseGetter).getReleaseByResourceId(DEFAULT_SIDECAR_REVISION, true);

            return new FixedResourceSupplier(TestData.DOWNLOADABLE_RESOURCE2, defaultSandboxResourceMeta,
                    sandboxReleaseGetter);
        }
    }

    private static Stream<Arguments> logbrokerDestroyPolicyTestParameters() {
        var streamBuilder = Stream.<Arguments>builder();

        int defaultMaxTries = LogbrokerPatcherUtils.DEFAULT_UNIFIED_AGENT_POLICY_MAX_TRIES;
        int userDefinedMaxTries = defaultMaxTries + 1;
        var policyMaxTriesToExpectedMaxTries = ImmutableMap.of(OptionalInt.empty(), defaultMaxTries,
                OptionalInt.of(userDefinedMaxTries), userDefinedMaxTries);

        long defaultRestartPeriodMs = LogbrokerPatcherUtils.DEFAULT_UNIFIED_AGENT_POLICY_RESTART_PERIOD_MS;
        long userDefinedRestartPeriodMs = defaultRestartPeriodMs * 10;
        var policyRestartPeriodMsToExpectedRestartPeriodMs = ImmutableMap.of(OptionalLong.empty(),
                defaultRestartPeriodMs, OptionalLong.of(userDefinedRestartPeriodMs), userDefinedRestartPeriodMs);

        for (var maxTries : policyMaxTriesToExpectedMaxTries.entrySet()) {
            for (var restartPeriodMs : policyRestartPeriodMsToExpectedRestartPeriodMs.entrySet()) {
                streamBuilder.add(Arguments.of(maxTries.getKey(), restartPeriodMs.getKey(), maxTries.getValue(),
                        restartPeriodMs.getValue()));
            }
        }

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("logbrokerDestroyPolicyTestParameters")
    void logbrokerDestroyPolicyTest(OptionalInt policyMaxTries, OptionalLong policyRestartPeriodMs,
                                    int expectedMaxTries, long expectedRestartPeriodMs) {
        LogbrokerDestroyPolicy destroyPolicy = new LogbrokerDestroyPolicy(policyMaxTries, policyRestartPeriodMs);

        var logbrokerConfig = LOGBROKER_CONFIG.toBuilder().withDestroyPolicy(destroyPolicy).build();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withLogbrokerConfig(logbrokerConfig);

        var podAgentSpec =
                patch(DEFAULT_PATCHER_CONTEXT, createPodTemplateSpecBuilderWithLogs(), deployUnitContext).getPodAgentSpec();

        TWorkload pushAgentWorkload =
                podAgentSpec.getWorkloadsList().stream().filter(w -> w.getId().equals(LOGBROKER_AGENT_WORKLOAD_ID)).findFirst().orElseThrow();

        var actualDestroyPolicy = pushAgentWorkload.getDestroyPolicy();

        assertThatEquals(actualDestroyPolicy.getMaxTries(), expectedMaxTries);

        var actualTimeLimit = actualDestroyPolicy.getContainer().getTimeLimit();
        assertThatEquals(actualTimeLimit.getMinRestartPeriodMs(), expectedRestartPeriodMs);
        assertThatEquals(actualTimeLimit.getMaxRestartPeriodMs(), expectedRestartPeriodMs);
    }

    private static Stream<Arguments> patchLogbrokerSecretsTestParameters() {
        var topicSecretAlias = "topic_secret_alias";
        var topicSecret = TestData.SECRET;

        NamedArgument<DataModel.TPodSpec.TSecret> topicSecretArgument = NamedArgument.of("topic_secret",
                Converter.toYpProto(topicSecret));

        var notPrefix = "not_";

        NamedArgument<DataModel.TPodSpec.TSecret> notTopicSecretArgument = NamedArgument.of("not_topic_secret",
                Converter.toYpProto(new SecretRef(notPrefix + topicSecret.getSecretRef().getUuid(),
                                notPrefix + topicSecret.getSecretRef().getVersion()),
                        notPrefix + topicSecret.getDelegationToken()));

        var notTopicSecretAlias = notPrefix + topicSecretAlias;

        NamedArgument<LogbrokerTopicConfig> withoutSecret = NamedArgument.of("custom topic config",
                DEFAULT_LOGBROKER_TOPIC_CONFIG);

        NamedArgument<LogbrokerTopicConfig> withSecret = NamedArgument.of("communal topic config",
                new LogbrokerTopicConfig(LOGBROKER_TOPIC_DESCRIPTION, "communaltvmtoken"));

        Map<String, NamedArgument<DataModel.TPodSpec.TSecret>> withTopicSecretAlias = Map.of(topicSecretAlias,
                notTopicSecretArgument);

        Map<String, NamedArgument<DataModel.TPodSpec.TSecret>> withOtherSecretAlias = Map.of(notTopicSecretAlias,
                topicSecretArgument);

        var withoutAnySecrets = emptyMap();

        var streamBuilder = Stream.<Arguments>builder();

        List.of(withTopicSecretAlias, withOtherSecretAlias, withoutAnySecrets).forEach(secrets -> streamBuilder.add(Arguments.of(secrets, withoutSecret)));

        streamBuilder.add(Arguments.of(withTopicSecretAlias, withSecret));

        List.of(withOtherSecretAlias, withoutAnySecrets).forEach(secrets -> streamBuilder.add(Arguments.of(secrets,
                withSecret)));

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("patchLogbrokerSecretsTestParameters")
    void logbrokerShouldNotChangeAnySecretsTest(Map<String, NamedArgument<DataModel.TPodSpec.TSecret>> userInitialSecrets, NamedArgument<LogbrokerTopicConfig> topicConfig) {
        var podTemplateSpecBuilder = createPodTemplateSpecBuilderWithLogs();

        var podSpecBuilder = podTemplateSpecBuilder.getSpecBuilder();
        podSpecBuilder.putAllSecrets(NamedArgument.toArgumentsMap(userInitialSecrets));

        var duContext = DEFAULT_UNIT_CONTEXT.withLogbrokerTopicConfig(topicConfig.getArgument());

        var podSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, duContext).getPodSpec();

        var actualUserPatchedSecrets = podSpec.getSecrets();
        assertThatEquals(actualUserPatchedSecrets, NamedArgument.toArgumentsMap(userInitialSecrets));
    }

    @Test
    void addTvmTokenForCommunalTopic() {

        String expectedTvmSecretVarValue = "communaltvmtoken is not secret at all";

        DeployUnitContext duContext =
                DEFAULT_UNIT_CONTEXT.withLogbrokerTopicConfig(new LogbrokerTopicConfig(LOGBROKER_TOPIC_DESCRIPTION,
                        expectedTvmSecretVarValue));

        var podTemplateSpecBuilder = createPodTemplateSpecBuilderWithLogs();
        DataModel.TPodSpec podSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, duContext).getPodSpec();

        TWorkload pushAgentWorkload =
                podSpec.getPodAgentPayload().getSpec().getWorkloadsList().stream().filter(w -> w.getId().equals(LOGBROKER_AGENT_WORKLOAD_ID)).findFirst().orElseThrow();

        assertThat(hasLiteralEnv(pushAgentWorkload, LogbrokerPatcherUtils.LOGBROKER_AGENT_TVM_SECRET_ENV_NAME,
                expectedTvmSecretVarValue), equalTo(true));
    }

    @Test
    void logbrokerBoxComputeResourcesTest() {
        var boxResourcesConfig = mock(LogbrokerBoxResourcesConfig.class);

        var expectedResources = DEFAULT_RESOURCES;

        when(boxResourcesConfig.getActualBoxResources(anyString(), any(AllComputeResources.class),
                any(Optional.class))).thenReturn(expectedResources);

        var logbrokerConfigWithResourcesRequest =
                DEFAULT_UNIT_CONTEXT.getSpec().getLogbrokerConfig().toBuilder().withPodAdditionalResourcesRequest(Optional.of(EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST)).build();

        logbrokerBoxComputeResourcesScenario(DEFAULT_PATCHER_CONTEXT.toBuilder().withBoxResourcesConfig(boxResourcesConfig).build(), DEFAULT_UNIT_CONTEXT.withLogbrokerConfig(logbrokerConfigWithResourcesRequest), expectedResources);

        verify(boxResourcesConfig).getActualBoxResources(eq(DEFAULT_UNIT_CONTEXT.getFullDeployUnitId()),
                eq(getDefaultLogbrokerBoxComputingResources()),
                eq(logbrokerConfigWithResourcesRequest.getPodAdditionalResourcesRequest()));
    }


    @Test
    void errorBoosterEnvTest() {
        TPodTemplateSpec.Builder templateSpecBuilder = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID,
                DEFAULT_USER_WORKLOAD_ID, false, Optional.empty());
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, templateSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        podAgentSpec.getWorkloadsList().forEach(w -> {
            assertThat(hasLiteralEnv(w, ERROR_BOOSTER_SYSLOG_HOST, LOCALHOST),
                    equalTo(errorBoosterEnvironmentsExport()));
            assertThat(hasLiteralEnv(w, ERROR_BOOSTER_SYSLOG_PORT, String.valueOf(SYSLOG_INPUT_PORT)),
                    equalTo(errorBoosterEnvironmentsExport()));
            assertThat(hasLiteralEnv(w, ERROR_BOOSTER_SENTRY_DSN, ERROR_BOOSTER_SENTRY_DSN_HARDCODE),
                    equalTo(errorBoosterEnvironmentsExport()));
            assertThat(hasLiteralEnv(w, ERROR_BOOSTER_HTTP_PORT, String.valueOf(SYSLOG_HTTP_PORT)),
                    equalTo(errorBoosterHttpEnvironmentsExport()));
            assertThat(hasLiteralEnv(w, ERROR_BOOSTER_HTTP_HOST, LOCALHOST),
                    equalTo(errorBoosterHttpEnvironmentsExport()));
        });

        podAgentSpec.getBoxesList().forEach(b -> {
            assertThat(hasLiteralEnv(b, ERROR_BOOSTER_SYSLOG_HOST, LOCALHOST),
                    equalTo(errorBoosterEnvironmentsExport()));
            assertThat(hasLiteralEnv(b, ERROR_BOOSTER_SYSLOG_PORT, String.valueOf(SYSLOG_INPUT_PORT)),
                    equalTo(errorBoosterEnvironmentsExport()));
            assertThat(hasLiteralEnv(b, ERROR_BOOSTER_SENTRY_DSN, ERROR_BOOSTER_SENTRY_DSN_HARDCODE),
                    equalTo(errorBoosterEnvironmentsExport()));
            assertThat(hasLiteralEnv(b, ERROR_BOOSTER_HTTP_PORT, String.valueOf(SYSLOG_HTTP_PORT)),
                    equalTo(errorBoosterHttpEnvironmentsExport()));
            assertThat(hasLiteralEnv(b, ERROR_BOOSTER_HTTP_HOST, LOCALHOST),
                    equalTo(errorBoosterHttpEnvironmentsExport()));
        });
    }

    @Test
    void autoEnableSystemLogsWhenUserLogsEnabledTest() {
        TPodTemplateSpec.Builder templateSpecBuilder1 = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID,
                DEFAULT_USER_WORKLOAD_ID, true, Optional.empty());
        var podAgentSpec1 =
                patch(DEFAULT_PATCHER_CONTEXT, templateSpecBuilder1, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TPodTemplateSpec.Builder templateSpecBuilder2 = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID,
                DEFAULT_USER_WORKLOAD_ID, true, Optional.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_DISABLED));
        var podAgentSpec2 =
                patch(DEFAULT_PATCHER_CONTEXT, templateSpecBuilder2, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TPodTemplateSpec.Builder templateSpecBuilder3 = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID,
                DEFAULT_USER_WORKLOAD_ID, false, Optional.empty());
        var podAgentSpec3 =
                patch(DEFAULT_PATCHER_CONTEXT, templateSpecBuilder3, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        assertThat(podAgentSpec1.getTransmitSystemLogsPolicy().getTransmitSystemLogs() == ETransmitSystemLogs.ETransmitSystemLogsPolicy_ENABLED, equalTo(autoEnableSystemLogsWhenUserLogsEnabled()));
        assertThat(podAgentSpec2.getTransmitSystemLogsPolicy().getTransmitSystemLogs(),
                equalTo(ETransmitSystemLogs.ETransmitSystemLogsPolicy_DISABLED));
        assertThat(podAgentSpec3.getTransmitSystemLogsPolicy().getTransmitSystemLogs(),
                equalTo(ETransmitSystemLogs.ETransmitSystemLogsPolicy_NONE));
    }

    private void logbrokerBoxComputeResourcesScenario(LogbrokerPatcherV1Context patcherContext,
                                                      DeployUnitContext deployUnitContext,
                                                      AllComputeResources expectedBoxComputeResources) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID,
                DEFAULT_USER_WORKLOAD_ID, true, Optional.empty());

        var podAgentSpec = patch(patcherContext, podTemplateSpecBuilder, deployUnitContext).getPodAgentSpec();

        TBox logbrokerBox =
                podAgentSpec.getBoxesList().stream().filter(b -> getLogbrokerBoxId().equals(b.getId())).findFirst().orElseThrow();

        var actualBoxComputeResources = logbrokerBox.getComputeResources();
        assertThatEquals(actualBoxComputeResources.getVcpuGuarantee(), expectedBoxComputeResources.getVcpuGuarantee());
        assertThatEquals(actualBoxComputeResources.getVcpuLimit(), expectedBoxComputeResources.getVcpuLimit());
        assertThatEquals(actualBoxComputeResources.getMemoryGuarantee(),
                expectedBoxComputeResources.getMemoryGuarantee());
        assertThatEquals(actualBoxComputeResources.getMemoryLimit(), expectedBoxComputeResources.getMemoryLimit());
        assertThatEquals(actualBoxComputeResources.getAnonymousMemoryLimit(),
                expectedBoxComputeResources.getAnonymousMemoryLimit());
        // TODO think about validating disk capacity
        assertThatEquals(actualBoxComputeResources.getThreadLimit(), expectedBoxComputeResources.getThreadLimit());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, MAX_LOGBROKER_BOX_VCPU})
    void podResourcesWithRequestedVcpuGuaranteeTest(long requestedValue) {
        podResourcesWithUserRequestScenario(requestedValue, AllComputeResources.Builder::withVcpuGuarantee);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, MAX_LOGBROKER_BOX_VCPU})
    void podResourcesWithRequestedVcpuLimitTest(long requestedValue) {
        podResourcesWithUserRequestScenario(requestedValue, AllComputeResources.Builder::withVcpuLimit);
    }

    private void podResourcesWithUserRequestScenario(long requestedValue, BiFunction<AllComputeResources.Builder,
            Long, AllComputeResources.Builder> transformer) {

        var requestedResources = transformer.apply(getDefaultLogbrokerBoxComputingResources().toBuilder(),
                requestedValue).build();

        var expectedPodAdditionalResources = requestedResources.toBuilder().build();

        podResourcesWithUserRequestScenario(Optional.of(requestedResources), expectedPodAdditionalResources);
    }

    @Test
    void podResourcesWithoutUserRequestTest() {
        podResourcesWithUserRequestScenario(Optional.empty(), getDefaultLogbrokerBoxComputingResources());
    }

    private void podResourcesWithUserRequestScenario(Optional<AllComputeResources> podAdditionalResourcesRequest,
                                                     AllComputeResources expectedPodAdditionalResources) {
        var logbrokerConfig =
                DEFAULT_UNIT_CONTEXT.getSpec().getLogbrokerConfig().toBuilder().withPodAdditionalResourcesRequest(podAdditionalResourcesRequest).build();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withLogbrokerConfig(logbrokerConfig);

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createPodTemplateSpecBuilder(DEFAULT_USER_BOX_ID,
                DEFAULT_USER_WORKLOAD_ID, true, Optional.empty());

        long defaultPodVcpuGuaranteeRequest = 50L;
        long defaultPodVcpuLimitRequest = 100L;

        var podResourcesRequestsBuilder =
                podTemplateSpecBuilder.getSpec().getResourceRequests().toBuilder().setVcpuGuarantee(defaultPodVcpuGuaranteeRequest).setVcpuLimit(defaultPodVcpuLimitRequest);

        podTemplateSpecBuilder.setSpec(podTemplateSpecBuilder.getSpecBuilder().setResourceRequests(podResourcesRequestsBuilder));

        var expectedPodResources =
                podResourcesRequestsBuilder.setVcpuGuarantee(podResourcesRequestsBuilder.getVcpuGuarantee() + expectedPodAdditionalResources.getVcpuGuarantee()).setVcpuLimit(podResourcesRequestsBuilder.getVcpuLimit() + expectedPodAdditionalResources.getVcpuLimit()).build();

        var podSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, deployUnitContext).getPodSpec();

        assertThatEquals(podSpec.getResourceRequests().getVcpuGuarantee(), expectedPodResources.getVcpuGuarantee());
        assertThatEquals(podSpec.getResourceRequests().getVcpuLimit(), expectedPodResources.getVcpuLimit());
    }

    private static void portoLogsDiskIdTestScenario(DataModel.TPodSpec podSpec,
                                                    Optional<String> configPortoLogsVirtualDisk,
                                                    String expectedPortoLogDiskId) {
        Optional<String> portoLogsDiskId =
                LogbrokerPatcherV1Base.getPortoLogsVolumeDiskAllocation(podSpec.toBuilder(),
                        Optional.of(ImmutableList.of(DEFAULT_LOGBROKER_DISK_ALLOCATION_ID)),
                        LOGBROKER_CONFIG.toBuilder().withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY).withLogsVirtualDiskIdRef(configPortoLogsVirtualDisk).build());

        assertThat(portoLogsDiskId.orElseThrow(), equalTo(expectedPortoLogDiskId));
    }

    private void addLogbrokerSidecarDiskScenario(DataModel.TPodSpec spec,
                                                 Optional<SidecarVolumeSettings> sidecarVolumeSettings,
                                                 String expectedLogbrokerDiskStorageClass) {
        TPodTemplateSpec.Builder templateSpecBuilder = createPodSpecBuilder().setSpec(spec);

        String logbrokerDiskVolumeAllocationId = DEFAULT_LOGBROKER_DISK_ALLOCATION_ID;

        LogbrokerConfig logbrokerConfig =
                LOGBROKER_CONFIG.toBuilder().withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY).withLogsVirtualDiskIdRef(Optional.of(USED_BY_INFRA_VOLUME_REQUEST.getId())).withSidecarVolumeSettings(sidecarVolumeSettings).build();

        var patcherContext =
                DEFAULT_PATCHER_CONTEXT.toBuilder().withDiskVolumeAllocationId(logbrokerDiskVolumeAllocationId).withAllSidecarDiskAllocationIds(ImmutableList.of(logbrokerDiskVolumeAllocationId)).withPatchBoxSpecificType(true).build();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withLogbrokerConfig(logbrokerConfig);

        var podSpec = patch(patcherContext, templateSpecBuilder, deployUnitContext).getPodSpec();

        String logbrokerDiskStorageClass =
                podSpec.getDiskVolumeRequestsList().stream().filter(disk -> disk.getId().equals(logbrokerDiskVolumeAllocationId)).findFirst().orElseThrow().getStorageClass();

        assertThatEquals(logbrokerDiskStorageClass, expectedLogbrokerDiskStorageClass);
    }

    private void addLogbrokerEnvironmentScenario(boolean hasDisableDiskIsolationLabel,
                                                 String logbrokerDiskVolumeAllocationId,
                                                 Set<String> diskVolumeRequestInSpec,
                                                 String logbrokerVirtualDiskRefInSpec,
                                                 String rootVirtualDiskRefInSpec) {

        String userBoxId = DEFAULT_USER_BOX_ID;
        String workloadId = DEFAULT_USER_WORKLOAD_ID;
        TPodTemplateSpec.Builder templateSpecBuilder = createPodTemplateSpecBuilder(userBoxId, workloadId, true,
                Optional.empty());

        if (hasDisableDiskIsolationLabel) {
            templateSpecBuilder.setLabels(TAttributeDictionary.newBuilder().addAttributes(DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE).build());
        }

        var patcherContext =
                DEFAULT_PATCHER_CONTEXT.toBuilder().withDiskVolumeAllocationId(logbrokerDiskVolumeAllocationId).withAllSidecarDiskAllocationIds(ImmutableList.of(logbrokerDiskVolumeAllocationId)).withPatchBoxSpecificType(true).build();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT;

        var patchResult = patch(patcherContext, templateSpecBuilder, deployUnitContext);
        var podSpec = patchResult.getPodSpec();

        testIds(podSpec.getDiskVolumeRequestsList(), diskVolumeRequestInSpec,
                DataModel.TPodSpec.TDiskVolumeRequest::getId);

        AssertUtils.assertResourceRequestEquals(podSpec.getResourceRequests(),
                getDefaultLogbrokerBoxComputingResources());

        List<DataModel.TPodSpec.TDiskVolumeRequest> volumeRequests =
                podSpec.getDiskVolumeRequestsList().stream().filter(r -> r.getId().equals(USED_BY_INFRA_VOLUME_REQUEST.getId())).collect(Collectors.toList());
        assertThat(volumeRequests, hasSize(1));

        var podAgentSpec = patchResult.getPodAgentSpec();

        Map<String, TLayer> layerMap = groupById(podAgentSpec.getResources().getLayersList(), TLayer::getId);
        assertThat(layerMap.keySet(), containsInAnyOrder(LogbrokerPatcherUtils.LOGBROKER_BASE_LAYER_ID,
                LOGBROKER_TOOLS_LAYER_ID));
        assertThat(layerMap.get(LOGBROKER_TOOLS_LAYER_ID).getMeta(),
                equalTo(PodSpecUtils.resourceMeta(TestData.RESOURCE_META)));

        assertThat(layerMap.get(LOGBROKER_TOOLS_LAYER_ID).getVirtualDiskIdRef(),
                equalTo(logbrokerVirtualDiskRefInSpec));
        assertThat(layerMap.get(LogbrokerPatcherUtils.LOGBROKER_BASE_LAYER_ID).getVirtualDiskIdRef(),
                equalTo(logbrokerVirtualDiskRefInSpec));

        Map<String, TResource> staticResourceMap = groupById(podAgentSpec.getResources().getStaticResourcesList(),
                TResource::getId);
        Set<String> expectedResourceIds = ImmutableSet.of(LOGBROKER_AGENT_CONF_RESOURCE_ID,
                LOGBROKER_UNIFIED_AGENT_CONF_RESOURCE_ID);
        testIds(podAgentSpec.getResources().getStaticResourcesList(), expectedResourceIds, TResource::getId);

        assertThat(staticResourceMap.get(LOGBROKER_AGENT_CONF_RESOURCE_ID).getVirtualDiskIdRef(),
                equalTo(logbrokerVirtualDiskRefInSpec));

        Set<String> expectedBoxIds = ImmutableSet.of(getLogbrokerBoxId());
        testIds(podAgentSpec.getBoxesList(), expectedBoxIds, TBox::getId);

        Set<String> expectedWorkloadIds = ImmutableSet.of(LOGBROKER_AGENT_WORKLOAD_ID,
                LogbrokerPatcherUtils.LOGBROKER_MONITOR_WORKLOAD_ID);

        testIds(podAgentSpec.getWorkloadsList(), expectedWorkloadIds, TWorkload::getId);

        Set<String> expectedVolumeIds = ImmutableSet.of(LogbrokerPatcherUtils.LOGBROKER_AGENT_LOGS_VOLUME_ID,
                LogbrokerPatcherUtils.LOGBROKER_AGENT_PORTO_LOGS_VOLUME_ID,
                LogbrokerPatcherUtils.LOGBROKER_AGENT_STATE_VOLUME_ID);

        testIds(podAgentSpec.getVolumesList(), expectedVolumeIds, TVolume::getId);

        Map<String, TVolume> volumeMap = groupById(podAgentSpec.getVolumesList(), TVolume::getId);
        assertThat(volumeMap.get(LogbrokerPatcherUtils.LOGBROKER_AGENT_LOGS_VOLUME_ID).getVirtualDiskIdRef(),
                equalTo(logbrokerVirtualDiskRefInSpec));
        assertThat(volumeMap.get(LogbrokerPatcherUtils.LOGBROKER_AGENT_STATE_VOLUME_ID).getVirtualDiskIdRef(),
                equalTo(logbrokerVirtualDiskRefInSpec));
        assertThat(volumeMap.get(LogbrokerPatcherUtils.LOGBROKER_AGENT_PORTO_LOGS_VOLUME_ID).getVirtualDiskIdRef(),
                equalTo(rootVirtualDiskRefInSpec));

        TBox userBox =
                podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(userBoxId)).findFirst().orElseThrow();

        testIds(userBox.getVolumesList(), ImmutableSet.of(LogbrokerPatcherUtils.LOGBROKER_AGENT_PORTO_LOGS_VOLUME_ID)
                , TMountedVolume::getVolumeRef);

        TBox logbrokerBox =
                podAgentSpec.getBoxesList().stream().filter(b -> b.getId().equals(getLogbrokerBoxId())).findFirst().orElseThrow();

        assertThat(logbrokerBox.getSpecificType(), equalTo(SYSTEM_BOX_SPECIFIC_TYPE));
        assertThat(logbrokerBox.getVirtualDiskIdRef(), equalTo(logbrokerVirtualDiskRefInSpec));

        testIds(logbrokerBox.getVolumesList(), ImmutableSet.of(LogbrokerPatcherUtils.LOGBROKER_AGENT_LOGS_VOLUME_ID),
                TMountedVolume::getVolumeRef);

        TWorkload pushAgentWorkload =
                podAgentSpec.getWorkloadsList().stream().filter(w -> w.getId().equals(LOGBROKER_AGENT_WORKLOAD_ID)).findFirst().orElseThrow();

        assertThat(pushAgentWorkload.getStart().getStdoutFile(),
                equalTo(LogbrokerPatcherV1Base.unifiedAgentLogFilePathInBox(STDOUT_LOG_TYPE).toString()));

        assertThat(pushAgentWorkload.getStart().getStderrFile(),
                equalTo(LogbrokerPatcherV1Base.unifiedAgentLogFilePathInBox(STDERR_LOG_TYPE).toString()));

        var logbrokerTopicConfig = deployUnitContext.getLogbrokerTopicConfig().orElseThrow();
        assertThat(hasSecret(pushAgentWorkload, LogbrokerPatcherUtils.LOGBROKER_AGENT_TVM_SECRET_ENV_NAME,
                logbrokerTopicConfig.getCustomTopicSecretSelector().orElseThrow()), equalTo(true));

        assertThat(hasLiteralEnv(pushAgentWorkload, LogbrokerPatcherV1Base.LOGBROKER_AGENT_STAGE_ID_ENV_NAME,
                DEFAULT_STAGE_CONTEXT.getStageId()), equalTo(true));

        assertThat(hasLiteralEnv(pushAgentWorkload, LogbrokerPatcherV1Base.LOGBROKER_AGENT_PROJECT_ID_ENV_NAME,
                DEFAULT_STAGE_CONTEXT.getProjectId()), equalTo(true));

        assertThat(hasLiteralEnv(pushAgentWorkload, LogbrokerPatcherV1Base.LOGBROKER_AGENT_DEPLOY_UNIT_ENV_NAME,
                DEFAULT_UNIT_CONTEXT.getDeployUnitId()), equalTo(true));

        assertThat(pushAgentWorkload.getStart().getStdoutAndStderrLimit(),
                equalTo(LogbrokerPatcherV1Base.LOGBROKER_AGENT_STDERR_STDOUT_LIMIT_BYTES));

        testIds(podAgentSpec.getResources().getStaticResourcesList(), expectedResourceIds, TResource::getId);

        Set<String> expectedPushAgentInitCommands = ImmutableSet.of(UNIFIED_AGENT_CONFIG_COPY_CMD,
                PUSH_AGENT_CONFIG_COPY_CMD);
        testIds(pushAgentWorkload.getInitList(), expectedPushAgentInitCommands, TUtilityContainer::getCommandLine);

        TWorkload userWorkload =
                podAgentSpec.getWorkloadsList().stream().filter(w -> w.getId().equals(workloadId)).findFirst().orElseThrow();

        assertThat(userWorkload.getStart().getStdoutFile(),
                equalTo(LogbrokerPatcherV1Base.portoLogFilePathInBox(workloadId, "stdout").toString()));
        assertThat(userWorkload.getStart().getStderrFile(),
                equalTo(LogbrokerPatcherV1Base.portoLogFilePathInBox(workloadId, "stderr").toString()));
        assertThat(hasLiteralEnv(userWorkload, DEPLOY_LOGNAME_ENV_NAME, DEFAULT_STAGE_CONTEXT.getStageId()),
                equalTo(true));
        assertThat(hasLiteralEnv(userWorkload, DEPLOY_LOGS_ENDPOINT_ENV_NAME, LogbrokerPatcherV1Base.logEndpoint()),
                equalTo(true));
        assertThat(hasLiteralEnv(userWorkload, DEPLOY_LOGS_SECRET_ENV_NAME, LOGBROKER_DEFAULT_STATIC_SECRET),
                equalTo(true));

        TResource pushAgentConfig = podAgentSpec.getResources().getStaticResources(0);
        assertThat(pushAgentConfig.getId(), equalTo(LOGBROKER_AGENT_CONF_RESOURCE_ID));
        assertThat(pushAgentConfig.getVirtualDiskIdRef(), equalTo(logbrokerVirtualDiskRefInSpec));

        Map<String, String> podAgentConfiguration = podSpec.getPodAgentPayload().getMeta().getConfigurationMap();

        assertThat(podAgentConfiguration, hasEntry(LogbrokerPatcherV1Base.LOGBROKER_LOGNAME_PATH,
                DEFAULT_STAGE_CONTEXT.getStageId()));
        assertThat(podAgentConfiguration, hasEntry(LogbrokerPatcherV1Base.LOGBROKER_DEPLOY_UNIT_PATH,
                DEFAULT_UNIT_CONTEXT.getDeployUnitId()));
        assertThat(podAgentConfiguration, hasEntry(LogbrokerPatcherV1Base.LOGBROKER_PROJECT_ID_PATH,
                DEFAULT_STAGE_CONTEXT.getProjectId()));

        assertThat(podAgentConfiguration, hasEntry(LogbrokerPatcherV1Base.LOGBROKER_USE_NEW_PROTOCOL_FLAG_PATH,
                LOGBROKER_USE_NEW_PROTOCOL_FLAG));
        assertThat(podAgentConfiguration, hasEntry(LogbrokerPatcherV1Base.LOGBROKER_USE_STRINGS_BATCHING_FLAG_PATH,
                LogbrokerPatcherV1Base.LOGBROKER_USE_STRINGS_BATCHING_FLAG));
    }

    private static TPodTemplateSpec.Builder createPodTemplateSpecBuilderWithLogs() {
        TPodTemplateSpec.Builder result = createPodSpecBuilder();
        result.getSpecBuilder().addDiskVolumeRequests(USED_BY_INFRA_VOLUME_REQUEST).getPodAgentPayloadBuilder().getSpecBuilder().addWorkloads(TestData.createWorkloadWithLogs(DEFAULT_USER_WORKLOAD_ID, ""));
        return result;
    }
}
