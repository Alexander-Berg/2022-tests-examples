package ru.yandex.infra.stage;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.bolts.collection.Either;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.BoxJugglerConfig;
import ru.yandex.infra.stage.dto.CoredumpConfig;
import ru.yandex.infra.stage.dto.CoredumpOutputPolicy;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.LogbrokerCommunalTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.LogbrokerConfig.SidecarBringupMode;
import ru.yandex.infra.stage.dto.LogbrokerCustomTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerDestroyPolicy;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.dto.McrsUnitSpec;
import ru.yandex.infra.stage.dto.PodAgentConfig;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.infra.stage.dto.TvmClient;
import ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils;
import ru.yandex.infra.stage.podspecs.revision.RevisionsHolder;
import ru.yandex.infra.stage.podspecs.revision.RevisionsHolderImpl;
import ru.yandex.infra.stage.podspecs.revision.model.RevisionScheme;
import ru.yandex.infra.stage.protobuf.Converter;
import ru.yandex.infra.stage.util.NamedArgument;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TMonitoringUnistatEndpoint;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TGenericVolume;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TResourceGang;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.StageValidatorImpl.LOGBROKER_BOX_RESOURCES_REQUEST_ERROR_PREFIX;
import static ru.yandex.infra.stage.StageValidatorImpl.LOGBROKER_CUSTOM_TOPIC_REQUEST_ERROR_PREFIX;
import static ru.yandex.infra.stage.StageValidatorImpl.LOGBROKER_DESTROY_POLICY_ERROR_PREFIX;
import static ru.yandex.infra.stage.StageValidatorImpl.LOGBROKER_DESTROY_POLICY_MAX_TRIES_ERROR_FORMAT;
import static ru.yandex.infra.stage.StageValidatorImpl.LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_ERROR_FORMAT;
import static ru.yandex.infra.stage.StageValidatorImpl.LOGBROKER_DESTROY_POLICY_TOTAL_RESTART_PERIOD_ERROR_FORMAT;
import static ru.yandex.infra.stage.StageValidatorImpl.MEMORY_GUARANTEE;
import static ru.yandex.infra.stage.StageValidatorImpl.MEMORY_LIMIT;
import static ru.yandex.infra.stage.StageValidatorImpl.MULTIPLE_BOXES_WITH_SAME_JUGGLER_PORTS_FORMAT;
import static ru.yandex.infra.stage.StageValidatorImpl.NO_SECRET_FOR_ALIAS_ERROR_FORMAT;
import static ru.yandex.infra.stage.StageValidatorImpl.VCPU_GUARANTEE;
import static ru.yandex.infra.stage.StageValidatorImpl.VCPU_LIMIT;
import static ru.yandex.infra.stage.StageValidatorImpl.errorWithPrefix;
import static ru.yandex.infra.stage.StageValidatorImpl.missingOrEmptyErrorMessage;
import static ru.yandex.infra.stage.StageValidatorImpl.valueNotInSegmentErrorMessage;
import static ru.yandex.infra.stage.StageValidatorImpl.valuesNotEqualErrorMessage;
import static ru.yandex.infra.stage.TestData.COREDUMP_CONFIG_WITHOUT_AGGREGATION;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST;
import static ru.yandex.infra.stage.TestData.LOGBROKER_CONFIG;
import static ru.yandex.infra.stage.TestData.MCRS_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.infra.stage.TestData.REPLICA_SET_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.STAGE_SPEC;
import static ru.yandex.infra.stage.TestData.TVM_CONFIG;
import static ru.yandex.infra.stage.TestData.USED_BY_INFRA_VOLUME_REQUEST;
import static ru.yandex.infra.stage.TestData.VALID_POD_RESOURCES;
import static ru.yandex.infra.stage.TestData.createBox;
import static ru.yandex.infra.stage.TestData.createWorkload;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.literalEnvVar;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.MAX_LOGBROKER_BOX_VCPU;

class StageValidatorImplTest {
    private static final String LOGBROKER_DISK_ALLOCATION_ID = "logbroker_disk_allocation";
    private static final String TVM_DISK_ALLOCATION_ID = "tvm_disk_allocation";
    private static final String POD_AGENT_DISK_ALLOCATION_ID = "pod_agent_disk_allocation";
    private static final RevisionsHolder REVISIONS_HOLDER_ONLY_DEFAULT = new RevisionsHolderImpl(
            emptyMap(), 0);
    private static final int CORRECT_PATCHERS_REVISION = 1, INCORRECT_PATCHERS_REVISION = 2;
    private static final RevisionsHolder REVISIONS_HOLDER_WITH_CORRECT_REVISION = new RevisionsHolderImpl(
            ImmutableMap.of(CORRECT_PATCHERS_REVISION, emptyList()), CORRECT_PATCHERS_REVISION);
    private static final LogbrokerCustomTopicRequest CORRECT_CUSTOM_TOPIC_REQUEST = new LogbrokerCustomTopicRequest(
            TestData.LOGBROKER_TOPIC_DESCRIPTION,
            TestData.SECRET_SELECTOR
    );

    private static final String CORRECT_CUSTOM_TOPIC_ALIAS = CORRECT_CUSTOM_TOPIC_REQUEST
            .getSecretSelector().getAlias();
    private static final DataModel.TSecretRef CORRECT_CUSTOM_TOPIC_SECRET_REF =
            Converter.toProto(TestData.SECRET_REF);

    private static final long MINIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS = 10000;
    private static final long MAXIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS = TimeUnit.HOURS.toMillis(5);

    private static final Config LOGBROKER_DESTROY_POLICY_CONFIG = ConfigFactory.parseMap(
            ImmutableMap.of(
                    LogbrokerPatcherUtils.MINIMAL_UNIFIED_AGENT_DESTROY_POLICY_RESTART_PERIOD_MS_CONFIG_KEY,
                    Duration.of(MINIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS, ChronoUnit.MILLIS),
                    LogbrokerPatcherUtils.MAXIMAL_UNIFIED_AGENT_DESTROY_POLICY_TOTAL_RESTART_PERIOD_MS_CONFIG_KEY,
                    Duration.of(MAXIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS, ChronoUnit.MILLIS)
            )
    );

    private static final StageValidatorImpl DEFAULT_VALIDATOR = new StageValidatorImpl(
            ImmutableSet.of(TestData.CLUSTER),
            ImmutableSet.of(TestData.BLACKBOX_ENVIRONMENT),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            REVISIONS_HOLDER_ONLY_DEFAULT,
            LOGBROKER_DESTROY_POLICY_CONFIG
    );

    private static final StageValidatorImpl VALIDATOR_WITH_SIDECAR_DISKS = DEFAULT_VALIDATOR.toBuilder()
            .withLogbrokerToolsAllocationId(LOGBROKER_DISK_ALLOCATION_ID)
            .withTvmToolsAllocationId(TVM_DISK_ALLOCATION_ID)
            .withPodAgentAllocationId(POD_AGENT_DISK_ALLOCATION_ID)
            .build();

    private static final String WORKLOAD_ID = "workload_id";
    private static final String STAGE_ID = "stage_id";
    private static final String BOX_ID = "box_id";

    enum ValidationResult {
        OK, FAIL
    }

    private static void noErrors(List<String> errors) {
        assertThat(errors, emptyIterable());
    }

    private static void hasError(Collection<String> errors, String expectedErrorMessage) {
        assertThat(expectedErrorMessage, in(errors));
    }

    @Test
    void failForEmptyDeployUnitsList() {
        List<String> errors = DEFAULT_VALIDATOR.validate(STAGE_SPEC.withDeployUnits(ImmutableMap.of()), STAGE_ID,
                Collections.emptySet());
        assertThat("Stage spec should contain at least one deploy unit", in(errors));
    }

    @Test
    void unitIdIsAddedForUnitSpecificErrors() {
        List<String> errors = DEFAULT_VALIDATOR.validate(STAGE_SPEC.withDeployUnits(ImmutableMap.of(
                        TestData.DEPLOY_UNIT_ID,
                        DEPLOY_UNIT_SPEC.withNetworkDefaults(TestData.EMPTY_NETWORK_DEFAULTS))),
                STAGE_ID, Collections.emptySet());

        assertThat(String.format("Deploy unit '%s': %s", TestData.DEPLOY_UNIT_ID,
                StageValidatorImpl.DEFAULT_NETWORK_ID_ERROR), in(errors));
    }

    @Test
    void failForUnknownClusters() {
        String otherCluster = "other";
        StageValidatorImpl validator = DEFAULT_VALIDATOR.withKnownClusters(ImmutableSet.of(otherCluster));
        String expectedMessage = String.format("Unknown clusters: '%s'; allowed are: '%s'", TestData.CLUSTER,
                otherCluster);
        assertThat(expectedMessage, in(validator.validateDeployUnitSpec(DEPLOY_UNIT_SPEC, STAGE_ID)));
    }

    @Test
    void failMcrsWithForDuplicatedClusterClusters() {
        TMultiClusterReplicaSetSpec mcrsSpec = TMultiClusterReplicaSetSpec.newBuilder()
                .addAllClusters(ImmutableList.of(TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                                .setCluster(TestData.CLUSTER)
                                .setSpec(TMultiClusterReplicaSetSpec.TReplicaSetSpecPreferences.newBuilder().setReplicaCount(1))
                                .build(),
                        TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                                .setCluster(TestData.CLUSTER)
                                .setSpec(TMultiClusterReplicaSetSpec.TReplicaSetSpecPreferences.newBuilder().setReplicaCount(2))
                                .build()))
                .build();

        String expectedMessage = String.format("Duplicated clusters in settings: %s, %s", TestData.CLUSTER, TestData.CLUSTER);
        var actualMessages = DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withDetails(
                        new McrsUnitSpec(mcrsSpec, POD_AGENT_CONFIG_EXTRACTOR)
                ),
                STAGE_ID
        );
        assertThat(expectedMessage, in(actualMessages));
    }

    @Test
    void failForUnknownBlackboxEnvironment() {
        String otherBlackbox = "other";
        StageValidatorImpl validator = DEFAULT_VALIDATOR.withBlackboxEnvironments(ImmutableSet.of(otherBlackbox));
        String expectedMessage = String.format("Unknown blackbox environment in TVM config: '%s'; allowed are: '%s'",
                TestData.BLACKBOX_ENVIRONMENT, otherBlackbox);
        assertThat(expectedMessage, in(validator.validateDeployUnitSpec(DEPLOY_UNIT_SPEC, STAGE_ID)));
    }

    @Test
    void allowEmptyClusters() {
        List<String> errors =
                DEFAULT_VALIDATOR.validateDeployUnitSpec(
                        DEPLOY_UNIT_SPEC.withDetails(
                                new ReplicaSetUnitSpec(
                                        TestData.REPLICA_SET_SPEC,
                                        emptyMap(),
                                        POD_AGENT_CONFIG_EXTRACTOR
                                )
                        ),
                        STAGE_ID
                );
        assertThat(errors, empty());
    }

    @Test
    void failForUnrecognizedLogbrokerSidecarBringupMode() {
        String expectedMessage = "Unrecognized sidecar bring up mode in logbroker config";
        List<String> errors =
                DEFAULT_VALIDATOR.validateDeployUnitSpec(
                        DEPLOY_UNIT_SPEC.withLogbrokerConfig(
                                LOGBROKER_CONFIG.toBuilder()
                                        .withSidecarBringupMode(SidecarBringupMode.UNRECOGNIZED)
                                        .build()
                        ),
                        STAGE_ID
                );
        assertThat(expectedMessage, in(errors));
    }

    @Test
    void failForRepeatedTvmAliases() {
        List<TvmClient> tvmClients = ImmutableList.of(TestData.TVM_CLIENT, TestData.TVM_CLIENT);
        List<String> errors =
                DEFAULT_VALIDATOR.validateDeployUnitSpec(
                        DEPLOY_UNIT_SPEC.withTvmConfig(TVM_CONFIG.withClients(tvmClients)),
                        STAGE_ID
                );
        String expectedMessage = String.format("Source alias '%s' is used multiple times in TVM config",
                TestData.TVM_SRC.getAlias());
        assertThat(expectedMessage, in(errors));
    }

    @Test
    void failForClashWithAllocationId() {
        String allocationId = "allocation_id";

        DataModel.TPodSpec.TDiskVolumeRequest diskVolumeRequest = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                .setId(allocationId)
                .setQuotaPolicy(DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                        .setCapacity(100L)
                        .build())
                .setStorageClass("hdd")
                .build();

        DataModel.TPodSpec podSpec = DataModel.TPodSpec.newBuilder().addDiskVolumeRequests(diskVolumeRequest).build();

        List<String> errors = StageValidatorImpl.validateClashWithInfraAllocation(podSpec, Optional.of(allocationId),
                StageValidatorImpl.SidecarToolType.LOGBROKER);
        assertThat(String.format("Allocation id '%s' is reserved for '%s'", allocationId,
                StageValidatorImpl.SidecarToolType.LOGBROKER.getValue()), in(errors));
    }

    @Test
    void failForClashLogbrokerWorkload() {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .addAllWorkloads(ImmutableList.of(
                        TWorkload.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_WORKLOAD_ID)
                                .build(),
                        TWorkload.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_MONITOR_WORKLOAD_ID)
                                .build()))
                .build();
        List<String> expectedErrors = ImmutableList.of(
                String.format("Workload '%s' is reserved for logbroker tools",
                        LogbrokerPatcherUtils.LOGBROKER_AGENT_WORKLOAD_ID),
                String.format("Workload '%s' is reserved for logbroker tools",
                        LogbrokerPatcherUtils.LOGBROKER_MONITOR_WORKLOAD_ID)
        );
        testLogbrokerClash(STAGE_ID, agentSpec, expectedErrors);
    }

    @Test
    void failForClashCommonEnv() {
        List<String> boxEnvNames = ImmutableList.of(
                CommonEnvPatcherUtils.DEPLOY_BOX_ID_ENV_NAME,
                CommonEnvPatcherUtils.DEPLOY_UNIT_ID_ENV_NAME,
                CommonEnvPatcherUtils.DEPLOY_STAGE_ID_ENV_NAME,
                CommonEnvPatcherUtils.DEPLOY_PROJECT_ID_ENV_NAME);

        TBox.Builder box = TBox.newBuilder();

        box.setId(BOX_ID);
        String varValue = "value";
        boxEnvNames.forEach(envName -> box.addEnv(literalEnvVar(envName, varValue)));

        TPodAgentSpec spec = TPodAgentSpec.newBuilder()
                .addWorkloads(TWorkload.newBuilder()
                        .setId(WORKLOAD_ID)
                        .addEnv(literalEnvVar(CommonEnvPatcherUtils.DEPLOY_WORKLOAD_ID_ENV_NAME, varValue))
                        .build())
                .addBoxes(box.build())
                .build();

        List<String> expectedErrors = new ArrayList<>();
        expectedErrors.add(String.format("Env var '%s' is reserved for Deploy, but it is found in workload '%s'",
                CommonEnvPatcherUtils.DEPLOY_WORKLOAD_ID_ENV_NAME, WORKLOAD_ID));
        expectedErrors.addAll(boxEnvNames.stream()
                .map(envName -> String.format("Env var '%s' is reserved for Deploy, but it is found in box '%s'",
                        envName, BOX_ID))
                .collect(Collectors.toList()));

        assertThat(StageValidatorImpl.validateCommonEnv(spec), equalTo(expectedErrors));
    }

    @Test
    void okForClashLogbrokerWorkloadWhenStageIsLogbrokerTest() {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .addAllWorkloads(ImmutableList.of(
                        TWorkload.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_WORKLOAD_ID)
                                .build(),
                        TWorkload.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_MONITOR_WORKLOAD_ID)
                                .build()))
                .build();
        List<String> expectedErrors = ImmutableList.of();

        testLogbrokerClash(String.format("%s_%s", LogbrokerPatcherUtils.LOGBROKER_TEST_STAGE_PREFIX, "1"), agentSpec,
                expectedErrors);
    }

    @Test
    void failForClashTvmLayer() {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .setResources(TResourceGang.newBuilder()
                        .addLayers(TLayer.newBuilder()
                                .setId(TvmPatcherUtils.TVM_BASE_LAYER_ID)
                                .build())
                        .build())
                .build();

        List<String> expectedErrors = ImmutableList.of(
                String.format("Layer '%s' is reserved for TVM", TvmPatcherUtils.TVM_BASE_LAYER_ID));

        testTvmClash(agentSpec, expectedErrors);
    }

    @ParameterizedTest
    @ValueSource(strings = {TvmPatcherUtils.TVM_BOX_ID, TvmPatcherUtils.TVM_BOX_ID_UNDERSCORED})
    void failForClashTvmBox(String boxId) {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(boxId)
                        .addEnv(literalEnvVar(TvmPatcherUtils.TVM_TOOL_URL_ENV, "aaa"))
                        .addEnv(literalEnvVar(TvmPatcherUtils.TVM_LOCAL_TOKEN_ENV, "aaa"))
                        .build())
                .build();
        List<String> expectedErrors = ImmutableList.of(
                String.format("Box '%s' is reserved for TVM", boxId),
                String.format("Env var '%s' is reserved for TVM, but it is found in box '%s'",
                        TvmPatcherUtils.TVM_LOCAL_TOKEN_ENV, boxId),
                String.format("Env var '%s' is reserved for TVM, but it is found in box '%s'",
                        TvmPatcherUtils.TVM_TOOL_URL_ENV, boxId)
        );
        testTvmClash(agentSpec, expectedErrors);
    }

    @Test
    void failForClashTvmWorkload() {
        String CLASH_ENV_WORKLOAD_ID = "clash_env_workload";
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .addAllWorkloads(ImmutableList.of(
                        TWorkload.newBuilder()
                                .setId(TvmPatcherUtils.TVM_WORKLOAD_ID)
                                .build(),
                        TWorkload.newBuilder()
                                .setId(CLASH_ENV_WORKLOAD_ID)
                                .addEnv(TEnvVar.newBuilder()
                                        .setName(TvmPatcherUtils.TVM_LOCAL_TOKEN_ENV)
                                        .build())
                                .addEnv(TEnvVar.newBuilder()
                                        .setName(TvmPatcherUtils.TVM_TOOL_URL_ENV)
                                        .build())
                                .build()))
                .build();
        List<String> expectedErrors = ImmutableList.of(
                String.format("Workload '%s' is reserved for TVM", TvmPatcherUtils.TVM_WORKLOAD_ID),
                String.format("Env var '%s' is reserved for TVM, " +
                                "but it is found in workload '%s'",
                        TvmPatcherUtils.TVM_LOCAL_TOKEN_ENV,
                        CLASH_ENV_WORKLOAD_ID),
                String.format("Env var '%s' is reserved for TVM, " +
                                "but it is found in workload '%s'",
                        TvmPatcherUtils.TVM_TOOL_URL_ENV,
                        CLASH_ENV_WORKLOAD_ID));
        testTvmClash(agentSpec, expectedErrors);
    }

    @Test
    void failForNumOfUsedThreadsGreaterThanAvailable() {
        long maxAvailableThreadsPerUserBoxes = 10;
        String userBoxId = "user_box_id";
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .addAllBoxes(ImmutableList.of(
                        TBox.newBuilder()
                                .setId(userBoxId)
                                .setComputeResources(TComputeResources.newBuilder()
                                        .setThreadLimit(maxAvailableThreadsPerUserBoxes + 1)
                                        .build())
                                .build()))
                .build();
        List<String> expectedErrors = ImmutableList.of(
                "11 threads used by user boxes is greater than 10 - max available threads for user boxes"
        );

        testThreadLimits(agentSpec, maxAvailableThreadsPerUserBoxes, expectedErrors);
    }

    @Test
    void failWhenNotEnoughThreadsToDistributePerUserBoxes() {
        long maxAvailableThreadsPerUserBoxes = 10;
        String userBoxId1 = "user_box_id_1";
        String userBoxId2 = "user_box_id_2";
        String userBoxId3 = "user_box_id_3";

        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .addAllBoxes(ImmutableList.of(
                        TBox.newBuilder()
                                .setId(userBoxId1)
                                .setComputeResources(TComputeResources.newBuilder()
                                        .setThreadLimit(maxAvailableThreadsPerUserBoxes - 1)
                                        .build())
                                .build(),
                        TBox.newBuilder()
                                .setId(userBoxId2)
                                .build(),
                        TBox.newBuilder()
                                .setId(userBoxId3)
                                .build()))
                .build();

        List<String> expectedErrors = ImmutableList.of(
                "Unable to distribute 1 threads for 2 user boxes, with min 1 threads for box"
        );

        testThreadLimits(agentSpec, maxAvailableThreadsPerUserBoxes, expectedErrors);
    }

    @Test
    void failForClashTvmResources() {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .setResources(TResourceGang.newBuilder()
                        .addAllStaticResources(ImmutableList.of(
                                TResource.newBuilder()
                                        .setId(TvmPatcherUtils.TVM_CONF_RESOURCE_ID)
                                        .build()))
                        .build())
                .build();
        List<String> expectedErrors = ImmutableList.of(
                String.format("Static resource '%s' is reserved for TVM", TvmPatcherUtils.TVM_CONF_RESOURCE_ID)
        );
        testTvmClash(agentSpec, expectedErrors);
    }

    @Test
    void successForEmptyTvmConfig() {
        // TODO: simplify after DEPLOY-993
        var validPodSpec = REPLICA_SET_UNIT_SPEC
                .getPodSpec().toBuilder()
                .setResourceRequests(VALID_POD_RESOURCES)
                .build();

        ReplicaSetUnitSpec validUnitSpec = REPLICA_SET_UNIT_SPEC.withPodSpec(validPodSpec);

        List<String> errors =
                DEFAULT_VALIDATOR.validateDeployUnitSpec(
                        DEPLOY_UNIT_SPEC.toBuilder()
                                .withoutTvmConfig()
                                .withDetails(validUnitSpec)
                                .build(),
                        STAGE_ID
                );
        assertThat(errors, empty());
    }

    @Test
    void failWhenMultipleUserDisksAndDisabledDiskIsolation() {
        TMultiClusterReplicaSetSpec.Builder builder = TestData.MULTI_CLUSTER_REPLICA_SET_SPEC_MULTIPLE_USER_DISKS.toBuilder();
        builder.getPodTemplateSpecBuilder().setLabels(TAttributeDictionary.newBuilder().addAttributes(DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE).build());

        McrsUnitSpec mcrsUnitSpec = new McrsUnitSpec(builder.build(), POD_AGENT_CONFIG_EXTRACTOR);

        List<String> errors = DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withoutTvmConfig()
                        .withDetails(mcrsUnitSpec)
                        .build(),
                STAGE_ID
        );

        assertThat("Only 1 user disk permitted when disk isolation disabled", in(errors));
    }

    @Test
    void okWhenMultipleUserDisksAndEnabledDiskIsolation() {
        List<String> errors = DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withoutTvmConfig()
                        .withDetails(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS)
                        .build(),
                STAGE_ID
        );
        assertThat(errors, empty());
    }

    @Test
    void failWhenMultipleUserDisksAndInvalidVirtualDiskRefsInPodAgentEntities() {
        String boxId = "box_id";
        String layerId = "layer_id";
        String volumeId = "volume_id";
        String staticResourceId = "static_resource_id";
        String invalidVirtualDiskRef = "invalid";

        TMultiClusterReplicaSetSpec.Builder builder =
                TestData.MULTI_CLUSTER_REPLICA_SET_SPEC_MULTIPLE_USER_DISKS.toBuilder();
        TPodAgentSpec.Builder podAgentSpecBuilder =
                builder.getPodTemplateSpecBuilder().getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();

        podAgentSpecBuilder.addBoxes(TBox.newBuilder().setId(boxId).setVirtualDiskIdRef(invalidVirtualDiskRef))
                .addVolumes(TVolume.newBuilder().setId(volumeId).setVirtualDiskIdRef(invalidVirtualDiskRef))
                .getResourcesBuilder()
                .addLayers(TLayer.newBuilder().setId(layerId).setVirtualDiskIdRef(invalidVirtualDiskRef))
                .addStaticResources(TResource.newBuilder().setId(staticResourceId).setVirtualDiskIdRef(invalidVirtualDiskRef));

        McrsUnitSpec mcrsUnitSpec = new McrsUnitSpec(builder.build(), POD_AGENT_CONFIG_EXTRACTOR);

        List<String> errors = DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withoutTvmConfig()
                        .withDetails(mcrsUnitSpec)
                        .build(),
                STAGE_ID
        );

        assertThat("Box(box_id) has invalid virtual disk id ref(invalid)", in(errors));
        assertThat("Volume(volume_id) has invalid virtual disk id ref(invalid)", in(errors));
        assertThat("Layer(layer_id) has invalid virtual disk id ref(invalid)", in(errors));
        assertThat("Static resource(static_resource_id) has invalid virtual disk id ref(invalid)", in(errors));
    }

    @Test
    void failForInvalidUnitId() {
        String badId = "comp*";
        List<String> errors = DEFAULT_VALIDATOR.validate(new StageSpec(ImmutableMap.of(
                badId, DEPLOY_UNIT_SPEC
        ), "abcde", 100500, false, emptyMap(), emptyMap()), STAGE_ID, Collections.emptySet());
        assertThat(String.format("Deploy unit id '%s' must match regexp '[A-Za-z0-9-_]+'", badId), in(errors));
    }

    @Test
    void failForInvalidDockerImageDescription() {
        String badDockerImageName = "image name";
        String badDockerImageTag = "image tag";
        Map<String, DockerImageDescription> imagesForBoxes = ImmutableMap.of(BOX_ID, new DockerImageDescription(
                "registry.yandex.net", badDockerImageName, badDockerImageTag));

        List<String> errors =
                DEFAULT_VALIDATOR.validate(STAGE_SPEC.withDeployUnits(ImmutableMap.of(TestData.DEPLOY_UNIT_ID,
                DEPLOY_UNIT_SPEC.withImagesForBoxes(imagesForBoxes))), STAGE_ID, Collections.emptySet());

        assertThat(String.format("Invalid docker image name '%s' was not caught", badDockerImageName),
                errors.stream().anyMatch(error -> error.contains(String.format("Docker image name '%s' must match " +
                        "pattern", badDockerImageName))));
        assertThat(String.format("Invalid docker image tag '%s' was not caught", badDockerImageTag),
                errors.stream().anyMatch(error -> error.contains(String.format("Docker image tag '%s' must match " +
                        "pattern", badDockerImageTag))));
    }

    @Test
    void failForZeroUsedByInfraVolumeRequests() {
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(DataModel.TPodSpec.newBuilder().build(), false,
                Optional.empty());
        assertThat("Pod spec must have exactly 1 'used_by_infra' disk volume request, found: 0", in(errors));
    }

    @Test
    void failForEnvNameDuplication() {
        String name = "NAME";
        TPodAgentSpec spec = TPodAgentSpec.newBuilder()
                .addWorkloads(TWorkload.newBuilder()
                        .setId(WORKLOAD_ID)
                        .addEnv(literalEnvVar(name, "value1"))
                        .addEnv(literalEnvVar(name, "value2"))
                        .build())
                .build();
        assertThat(StageValidatorImpl.validatePodAgentSpec(spec, false),
                equalTo(ImmutableList.of(String.format("Multiple entries for environment variable %s in workload %s",
                        name, WORKLOAD_ID))));
    }

    @Test
    void failForLogbrokerEnvClash() {
        List<String> envNames = ImmutableList.of(LogbrokerPatcherUtils.DEPLOY_LOGNAME_ENV_NAME,
                LogbrokerPatcherUtils.DEPLOY_LOGS_ENDPOINT_ENV_NAME,
                LogbrokerPatcherUtils.DEPLOY_LOGS_SECRET_ENV_NAME);

        TWorkload.Builder workload = TWorkload.newBuilder();

        workload.setId(WORKLOAD_ID);
        workload.setTransmitLogs(true);
        String varValue = "value";
        envNames.forEach(envName -> workload.addEnv(literalEnvVar(envName, varValue)));

        TPodAgentSpec spec = TPodAgentSpec.newBuilder()
                .addWorkloads(workload.build())
                .build();

        List<String> expectedErrors = envNames.stream()
                .map(envName -> String.format("Env var '%s' is reserved for Deploy, but it is found in workload '%s'"
                        , envName, WORKLOAD_ID))
                .collect(Collectors.toList());

        assertThat(StageValidatorImpl.validateClashWithLogbrokerIds(spec, STAGE_ID), equalTo(expectedErrors));
    }

    @Test
    void failForLogbrokerVolumesClash() {
        TPodAgentSpec spec = TPodAgentSpec.newBuilder().addAllVolumes(ImmutableList.of(
                        TVolume.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_LOGS_VOLUME_ID)
                                .setGeneric(TGenericVolume.newBuilder().build())
                                .build(),
                        TVolume.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_PORTO_LOGS_VOLUME_ID)
                                .setGeneric(TGenericVolume.newBuilder().build())
                                .build(),
                        TVolume.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_STATE_VOLUME_ID)
                                .setGeneric(TGenericVolume.newBuilder().build())
                                .build()))
                .build();

        List<String> expectedErrors = ImmutableList.of(
                String.format("Volume '%s' is reserved for logbroker",
                        LogbrokerPatcherUtils.LOGBROKER_AGENT_LOGS_VOLUME_ID),
                String.format("Volume '%s' is reserved for logbroker",
                        LogbrokerPatcherUtils.LOGBROKER_AGENT_PORTO_LOGS_VOLUME_ID),
                String.format("Volume '%s' is reserved for logbroker",
                        LogbrokerPatcherUtils.LOGBROKER_AGENT_STATE_VOLUME_ID)
        );

        testLogbrokerClash(STAGE_ID, spec, expectedErrors);
    }

    @Test
    void okForLogbrokerVolumesClashWhenStageIsLogbrokerTest() {
        TPodAgentSpec spec = TPodAgentSpec.newBuilder().addAllVolumes(ImmutableList.of(
                        TVolume.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_LOGS_VOLUME_ID)
                                .setGeneric(TGenericVolume.newBuilder().build())
                                .build(),
                        TVolume.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_PORTO_LOGS_VOLUME_ID)
                                .setGeneric(TGenericVolume.newBuilder().build())
                                .build(),
                        TVolume.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_AGENT_STATE_VOLUME_ID)
                                .setGeneric(TGenericVolume.newBuilder().build())
                                .build()))
                .build();

        List<String> expectedErrors = ImmutableList.of();

        testLogbrokerClash(String.format("%s_%s", LogbrokerPatcherUtils.LOGBROKER_TEST_STAGE_PREFIX, "1"), spec,
                expectedErrors);
    }

    @Test
    void failForClashLogbrokerLayer() {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .setResources(TResourceGang.newBuilder()
                        .addLayers(TLayer.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_BASE_LAYER_ID)
                                .build())
                        .build())
                .build();

        List<String> expectedErrors = ImmutableList.of(
                String.format("Layer '%s' is reserved for logbroker", LogbrokerPatcherUtils.LOGBROKER_BASE_LAYER_ID));

        testLogbrokerClash(STAGE_ID, agentSpec, expectedErrors);
    }

    @Test
    void okForClashLogbrokerLayerWhenStageIsLogbrokerTest() {
        TPodAgentSpec agentSpec = TPodAgentSpec.newBuilder()
                .setResources(TResourceGang.newBuilder()
                        .addLayers(TLayer.newBuilder()
                                .setId(LogbrokerPatcherUtils.LOGBROKER_BASE_LAYER_ID)
                                .build())
                        .build())
                .build();

        List<String> expectedErrors = ImmutableList.of();

        testLogbrokerClash(String.format("%s_%s", LogbrokerPatcherUtils.LOGBROKER_TEST_STAGE_PREFIX, "1"), agentSpec,
                expectedErrors);
    }

    @Test
    void failForEmptyDeploymentStrategy() {
        String cluster = "sas-test";
        ReplicaSetUnitSpec unitSpec =
                new ReplicaSetUnitSpec(TestData.REPLICA_SET_SPEC.toBuilder().clearDeploymentStrategy().build(),
                        Map.of(cluster, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), Optional.empty())),
                        POD_AGENT_CONFIG_EXTRACTOR);

        List<String> errors = DEFAULT_VALIDATOR.validate(
                STAGE_SPEC.withDeployUnits(
                        ImmutableMap.of(
                                TestData.DEPLOY_UNIT_ID,
                                DEPLOY_UNIT_SPEC.withDetails(unitSpec)
                        )
                ),
                STAGE_ID,
                Collections.emptySet()
        );
        assertThat(String.format("Deploy unit '%s': Empty deployment strategy for replica set on '%s'",
                TestData.DEPLOY_UNIT_ID, cluster), in(errors));
    }

    @Test
    void failForAbsentLimits() {
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(DataModel.TPodSpec.getDefaultInstance(), false,
                Optional.empty());
        assertThat("Memory limit must not be zero", in(errors));
        assertThat("Cpu limit must not be zero", in(errors));
    }

    @Test
    void failForDifferentMemoryLimitGuarantee() {
        DataModel.TPodSpec spec = DataModel.TPodSpec.newBuilder()
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryLimit(100)
                        .setMemoryGuarantee(200)
                        .build())
                .build();
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(spec, false, Optional.empty());
        assertThat("Memory guarantee 200 and memory limit 100 must be equal", in(errors));
    }

    @Test
    void failForWrongAnonymousMemoryLimit() {
        DataModel.TPodSpec spec = DataModel.TPodSpec.newBuilder()
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryLimit(100)
                        .setAnonymousMemoryLimit(200)
                        .build())
                .build();
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(spec, false, Optional.empty());
        assertThat(StageValidatorImpl.WRONG_ANONYMOUS_MEMORY_ERROR, in(errors));
    }

    @Test
    void okForAbsentAnonymousMemoryLimit() {
        DataModel.TPodSpec spec = DataModel.TPodSpec.newBuilder()
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryLimit(500)
                        .build())
                .build();
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(spec, false, Optional.empty());
        assertThat(StageValidatorImpl.WRONG_ANONYMOUS_MEMORY_ERROR, not(in(errors)));
    }

    @Test
    void okForCorrectAnonymousMemoryLimit() {
        DataModel.TPodSpec spec = DataModel.TPodSpec.newBuilder()
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryLimit(500)
                        .setAnonymousMemoryLimit(400)
                        .build())
                .build();
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(spec, false, Optional.empty());
        assertThat(StageValidatorImpl.WRONG_ANONYMOUS_MEMORY_ERROR, not(in(errors)));
    }

    @Test
    void failForFilledByUserPodAgentBinaryRevisionInDeploymentMeta() {
        DataModel.TPodSpec spec = DataModel.TPodSpec.newBuilder()
                .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setMeta(DataModel.TPodSpec.TPodAgentDeploymentMeta.newBuilder()
                                .setBinaryRevision(1L))
                        .build())
                .build();
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(spec, false, Optional.empty());
        assertThat("Pod agent binary revision in PodAgentDeploymentMeta can't be filled by user", in(errors));
    }

    @Test
    void failWhenBoxesResourceSumGreaterThenResourcesFromRequest() {
        List<String> resourceDescriptions = ImmutableList.of(MEMORY_LIMIT, MEMORY_GUARANTEE, VCPU_LIMIT, VCPU_GUARANTEE);

        long boxResourceValue = 300L;
        long requestResourceValue = 200L;

        TBox.Builder box = TBox.newBuilder().setComputeResources(TComputeResources.newBuilder()
                .setMemoryLimit(boxResourceValue)
                .setMemoryGuarantee(boxResourceValue)
                .setVcpuGuarantee(boxResourceValue)
                .setVcpuLimit(boxResourceValue)
                .build());

        DataModel.TPodSpec spec = DataModel.TPodSpec.newBuilder()
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryLimit(requestResourceValue)
                        .setMemoryGuarantee(requestResourceValue)
                        .setVcpuGuarantee(requestResourceValue)
                        .setVcpuLimit(requestResourceValue)
                        .build())
                .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setSpec(TPodAgentSpec.newBuilder()
                                .addBoxes(box)))
                .build();
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(spec, false, Optional.empty());

        resourceDescriptions.forEach(description -> assertThat(String.format("Boxes %s sum 300 should be less than %s" +
                " 200 pod", description, description), in(errors)));
    }

    @Test
    void failForMultipleBoxesWithoutJugglerPort() {
        Map<String, BoxJugglerConfig> jugglerConfigs = ImmutableMap.of(
                "box1", new BoxJugglerConfig(emptyList(), OptionalInt.empty(), Optional.empty()),
                "box2", new BoxJugglerConfig(emptyList(), OptionalInt.empty(), Optional.empty())
        );

        List<String> errors = DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withJugglerConfig(jugglerConfigs),
                STAGE_ID
        );
        String expectedError = String.format(
                MULTIPLE_BOXES_WITH_SAME_JUGGLER_PORTS_FORMAT,
                Map.of(BoxJugglerConfig.DEFAULT_PORT, List.of("box1", "box2"))
        );
        assertThat(expectedError, in(errors));
    }

    @Test
    void failForMultipleBoxesWithSameJugglerPort() {
        Map<String, BoxJugglerConfig> jugglerConfigs = ImmutableMap.of(
                "box1", new BoxJugglerConfig(emptyList(), OptionalInt.of(BoxJugglerConfig.DEFAULT_PORT), Optional.empty()),
                "box2", new BoxJugglerConfig(emptyList(), OptionalInt.of(BoxJugglerConfig.DEFAULT_PORT), Optional.empty())
        );

        List<String> errors = DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withJugglerConfig(jugglerConfigs),
                STAGE_ID
        );
        String expectedError = String.format(
                MULTIPLE_BOXES_WITH_SAME_JUGGLER_PORTS_FORMAT,
                Map.of(BoxJugglerConfig.DEFAULT_PORT, List.of("box1", "box2"))
        );
        assertThat(expectedError, in(errors));
    }

    @Test
    void failForReservedMonitoringPodLabels() {
        DataModel.TPodSpec.Builder builder = DataModel.TPodSpec.newBuilder();
        builder.getHostInfraBuilder().getMonitoringBuilder()
                .putLabels("stage", "user");
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(builder.build(), false, Optional.empty());
        assertThat("Monitoring label 'stage' is reserved by Deploy", in(errors));
    }

    @Test
    void failForReservedMonitoringEndpointLabels() {
        DataModel.TPodSpec.Builder builder = DataModel.TPodSpec.newBuilder();
        builder.getHostInfraBuilder().getMonitoringBuilder()
                .addUnistats(TMonitoringUnistatEndpoint.newBuilder()
                        .putLabels("stage", "user")
                        .build());
        List<String> errors = DEFAULT_VALIDATOR.validatePodSpec(builder.build(), false, Optional.empty());
        assertThat("Monitoring label 'stage' is reserved by Deploy", in(errors));
    }

    @Test
    void failForTvmSidecarDiskStorageClassAutoWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        List<String> errors = VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC_MULTIPLE_USER_DISKS, true,
                volSettings);

        assertThat("storage class auto is not permitted for tvm sidecar volume settings, when num user disks is 2",
                in(errors));
    }

    @Test
    void failWhenTvmSidecarDiskStorageClassAbsentWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings = Optional.empty();
        List<String> errors = VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC_MULTIPLE_USER_DISKS, true,
                volSettings);

        assertThat("storage class should be defined for tvm sidecar volume settings, when num user disks is 2",
                in(errors));
    }

    @Test
    void okWithTvmSidecarDiskStorageClassAnyValueAndOneUserDiskInSpec() {
        Optional<SidecarVolumeSettings> volSettings1 = Optional.empty();
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        Optional<SidecarVolumeSettings> volSettings3 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings4 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        assertThat(VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC, true, volSettings1), emptyIterable());
        assertThat(VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC, true, volSettings2), emptyIterable());
        assertThat(VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC, true, volSettings3), emptyIterable());
        assertThat(VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC, true, volSettings4), emptyIterable());
    }

    @Test
    void okForTvmSidecarStorageClassNonAutoWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings1 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        assertThat(VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC_MULTIPLE_USER_DISKS, true, volSettings1),
                emptyIterable());
        assertThat(VALIDATOR_WITH_SIDECAR_DISKS.validatePodSpec(TestData.POD_SPEC_MULTIPLE_USER_DISKS, true, volSettings2),
                emptyIterable());
    }

    @Test
    void failForLogbrokerSidecarDiskStorageClassAutoWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        List<String> errors = logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings);

        assertThat("storage class auto is not permitted for logbroker sidecar volume settings, when num user disks is" +
                " 2", in(errors));
    }

    @Test
    void failWhenLogbrokerSidecarDiskStorageClassAbsentWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings = Optional.empty();
        List<String> errors = logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings);

        assertThat("storage class should be defined for logbroker sidecar volume settings, when num user disks is 2",
                in(errors));
    }

    @Test
    void okWithLogbrokerSidecarDiskStorageClassAnyValueAndOneUserDiskInSpec() {
        Optional<SidecarVolumeSettings> volSettings1 = Optional.empty();
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        Optional<SidecarVolumeSettings> volSettings3 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings4 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        assertThat(logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings1), emptyIterable());
        assertThat(logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings2), emptyIterable());
        assertThat(logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings3), emptyIterable());
        assertThat(logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings4), emptyIterable());
    }

    @Test
    void okForLogbrokerSidecarStorageClassNonAutoWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings1 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        assertThat(logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings1),
                emptyIterable());
        assertThat(logbrokerSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings2),
                emptyIterable());
    }

    @Test
    void failForPodAgentSidecarDiskStorageClassAutoWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        List<String> errors = podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings);

        assertThat("storage class auto is not permitted for pod_agent sidecar volume settings, when num user disks is" +
                " 2", in(errors));
    }


    @Test
    void failWhenPodAgentSidecarDiskStorageClassAbsentWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings = Optional.empty();
        List<String> errors = podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings);

        assertThat("storage class should be defined for pod_agent sidecar volume settings, when num user disks is 2",
                in(errors));
    }


    @Test
    void okWithPodAgentSidecarDiskStorageClassAnyValueAndOneUserDiskInSpec() {
        Optional<SidecarVolumeSettings> volSettings1 = Optional.empty();
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        Optional<SidecarVolumeSettings> volSettings3 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings4 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        assertThat(podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings1), emptyIterable());
        assertThat(podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings2), emptyIterable());
        assertThat(podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings3), emptyIterable());
        assertThat(podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC, volSettings4), emptyIterable());
    }

    @Test
    void okForPodAgentSidecarStorageClassNonAutoWithMultipleUserDisks() {
        Optional<SidecarVolumeSettings> volSettings1 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> volSettings2 =
                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));

        assertThat(podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings1),
                emptyIterable());
        assertThat(podAgentSidecarStorageClassScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS, volSettings2),
                emptyIterable());
    }

    @Test
    void failWhenSidecarDiskAllocationIdsClashWithUserDiskIds() {
        DataModel.TPodSpec.TDiskVolumeRequest logbrokerVolumeRequest =
                DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                        .setId(LOGBROKER_DISK_ALLOCATION_ID)
                        .setStorageClass("hdd")
                        .build();

        DataModel.TPodSpec.TDiskVolumeRequest tvmVolumeRequest = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                .setId(TVM_DISK_ALLOCATION_ID)
                .setStorageClass("hdd")
                .build();

        DataModel.TPodSpec.TDiskVolumeRequest podAgentVolumeRequest = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                .setId(POD_AGENT_DISK_ALLOCATION_ID)
                .setStorageClass("hdd")
                .build();

        DataModel.TPodSpec podSpec = DataModel.TPodSpec.newBuilder()
                .addAllDiskVolumeRequests(ImmutableSet.of(logbrokerVolumeRequest, tvmVolumeRequest,
                        podAgentVolumeRequest))
                .build();


        TMultiClusterReplicaSetSpec mcrsSpec = TMultiClusterReplicaSetSpec.newBuilder()
                .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                        .setSpec(podSpec))
                .build();

        List<String> errors = VALIDATOR_WITH_SIDECAR_DISKS.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withDetails(
                        new McrsUnitSpec(mcrsSpec, POD_AGENT_CONFIG_EXTRACTOR)
                ),
                DEFAULT_STAGE_CONTEXT.getStageId()
        );

        assertThat(String.format("Allocation id '%s' is reserved for '%s'", LOGBROKER_DISK_ALLOCATION_ID,
                StageValidatorImpl.LOGBROKER_TOOLS_TYPE), in(errors));
        assertThat(String.format("Allocation id '%s' is reserved for '%s'", TVM_DISK_ALLOCATION_ID,
                StageValidatorImpl.TVM_TOOLS_TYPE), in(errors));
        assertThat(String.format("Allocation id '%s' is reserved for '%s'", POD_AGENT_DISK_ALLOCATION_ID,
                StageValidatorImpl.POD_AGENT_SIDECAR_TYPE), in(errors));
    }

    @Test
    void failWhenPortoLogsVirtualDiskIdRefNotEqualToAnyUserDiskId() {
        String portoLogsVirtualDiskIdRef = "porto_logs_disk_id";
        List<String> errors = portoLogsVirtualDiskIdRefTestScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS,
                Optional.of(portoLogsVirtualDiskIdRef));
        assertThat(String.format("Logbroker logs virtual disk id ref: %s not equal to any user disk id",
                portoLogsVirtualDiskIdRef), in(errors));
    }

    @Test
    void failWhenPortoLogsVirtualDiskIdRefEmptyWithMultipleUserDisks() {
        List<String> errors = portoLogsVirtualDiskIdRefTestScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS,
                Optional.empty());
        assertThat("Logbroker logs virtual disk id ref should be defined, when num user disks is 2", in(errors));
    }

    @Test
    void okWhenPortoLogsVirtualDiskIdRefEqualToAnyUserDiskId() {
        assertThat(portoLogsVirtualDiskIdRefTestScenario(MCRS_UNIT_SPEC_MULTIPLE_USER_DISKS,
                Optional.of(USED_BY_INFRA_VOLUME_REQUEST.getId())), emptyIterable());
    }

    @Test
    void okWhenPortoLogsVirtualDiskIdRefEmptyWithOneUserDisk() {
        assertThat(portoLogsVirtualDiskIdRefTestScenario(MCRS_UNIT_SPEC, Optional.empty()), emptyIterable());
    }

    @Test
    void failWhenCoredumpOutVolumeNotMountedToBox() {
        List<String> errors = coredumpWithOutputVolumeScenario("workload_id", "box_id", "coredump_out_volume_id",
                Optional.empty());
        assertThat("Coredump out volume coredump_out_volume_id is not mounted to box box_id ", in(errors));
    }

    @Test
    void okWhenCoredumpOutVolumeMountedToBox() {
        List<String> errors = coredumpWithOutputVolumeScenario("workload_id", "box_id", "coredump_out_volume_id",
                Optional.of("coredump_volume_mount_path"));
        assertThat(errors, emptyIterable());
    }

    @Test
    void okWhenPatchersRevisionDefault() {
        correctPatchersRevisionTest(RevisionScheme.DEFAULT_REVISION_ID);
    }

    @Test
    void okWhenPatchersRevisionCorrect() {
        correctPatchersRevisionTest(CORRECT_PATCHERS_REVISION);
    }

    private static void correctPatchersRevisionTest(int revisionId) {
        List<String> errors = patchersRevisionScenario(revisionId);
        assertThat(errors, emptyIterable());
    }

    @Test
    void failWhenPatchersRevisionIncorrect() {
        List<String> errors = patchersRevisionScenario(INCORRECT_PATCHERS_REVISION);
        assertThat(String.format("Unknown patchers revision %d", INCORRECT_PATCHERS_REVISION), in(errors));
    }

    private static List<String> patchersRevisionScenario(int revisionId) {
        var validator = DEFAULT_VALIDATOR.withPatchersRevisionsHolder(REVISIONS_HOLDER_WITH_CORRECT_REVISION);
        return validator.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withPatchersRevision(revisionId),
                STAGE_ID
        );
    }

    @Test
    void okWhenLogbrokerDestroyPolicyDefault() {
        correctLogbrokerDestroyPolicyTest(OptionalInt.empty(), OptionalLong.empty());
    }

    @ParameterizedTest
    @ValueSource(ints = {
            LogbrokerPatcherUtils.DEFAULT_UNIFIED_AGENT_POLICY_MAX_TRIES - 1,
            LogbrokerPatcherUtils.DEFAULT_UNIFIED_AGENT_POLICY_MAX_TRIES + 1
    })
    void okWhenLogbrokerDestroyPolicyMaxTriesCorrect(int correctMaxTries) {
        correctLogbrokerDestroyPolicyTest(OptionalInt.of(correctMaxTries), OptionalLong.empty());
    }

    @Test
    void failWhenLogbrokerDestroyPolicyMaxTriesIncorrect() {
        int incorrectMaxTries = -1;
        String expectedErrorMessage = errorWithPrefix(
                LOGBROKER_DESTROY_POLICY_ERROR_PREFIX,
                String.format(LOGBROKER_DESTROY_POLICY_MAX_TRIES_ERROR_FORMAT, incorrectMaxTries)
        );

        incorrectLogbrokerDestroyPolicyTest(
                OptionalInt.of(incorrectMaxTries),
                OptionalLong.empty(),
                expectedErrorMessage
        );
    }

    @Test
    void okWhenLogbrokerDestroyPolicyRestartPeriodMsCorrect() {
        correctLogbrokerDestroyPolicyTest(
                OptionalInt.empty(),
                OptionalLong.of(MINIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS * 10)
        );
    }

    @Test
    void failWhenLogbrokerDestroyPolicyRestartPeriodMsIncorrect() {
        long incorrectRestartPeriodMs = MINIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS - 1;

        String expectedErrorMessage = errorWithPrefix(
                LOGBROKER_DESTROY_POLICY_ERROR_PREFIX,
                String.format(
                        LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_ERROR_FORMAT,
                        MINIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS,
                        incorrectRestartPeriodMs
                )
        );

        incorrectLogbrokerDestroyPolicyTest(
                OptionalInt.empty(),
                OptionalLong.of(incorrectRestartPeriodMs),
                expectedErrorMessage
        );
    }


    private static Stream<Arguments> logbrokerDestroyPolicyTotalRestartPeriodTestParameters() {
        long maxCorrectTotalRestartPeriod = MAXIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS;
        long minIncorrectTotalRestartPeriod = maxCorrectTotalRestartPeriod + 1;

        var maxCorrectTotalRestartPeriodArgument = NamedArgument.of(
                "maximal correct total restart period", maxCorrectTotalRestartPeriod
        );

        var minIncorrectTotalRestartPeriodArgument = NamedArgument.of(
                "minimal incorrect total restart period", minIncorrectTotalRestartPeriod
        );

        var noRestartsMaxTriesArgument = NamedArgument.of("no restarts", 1);

        var oneRestartMaxTriesArgument = NamedArgument.of("one restart", 2);

        int defaultMaxTries = LogbrokerPatcherUtils.DEFAULT_UNIFIED_AGENT_POLICY_MAX_TRIES;

        var defaultMaxTriesArgument = NamedArgument.of("default max tries", defaultMaxTries);

        long defaultMaxTriesMaxCorrectRestartPeriod = maxCorrectTotalRestartPeriod / (defaultMaxTries - 1);

        var defaultMaxTriesMaxCorrectRestartPeriodArgument = NamedArgument.of(
                "maximal correct restart period for default max tries", defaultMaxTriesMaxCorrectRestartPeriod
        );

        var defaultMaxTriesMinIncorrectRestartPeriodArgument = NamedArgument.of(
                "minimal incorrect restart period for default max tries", defaultMaxTriesMaxCorrectRestartPeriod + 1
        );

        long defaultRestartPeriod = LogbrokerPatcherUtils.DEFAULT_UNIFIED_AGENT_POLICY_RESTART_PERIOD_MS;

        var defaultRestartPeriodArgument = NamedArgument.of(
                "default restart period", defaultRestartPeriod
        );

        int defaultRestartPeriodMaxCorrectMaxTries = (int) (maxCorrectTotalRestartPeriod / defaultRestartPeriod) + 1;

        var defaultRestartPeriodMaxCorrectMaxTriesArgument = NamedArgument.of(
                "maximal correct max tries for default restart period", defaultRestartPeriodMaxCorrectMaxTries
        );

        var defaultRestartPeriodMinIncorrectMaxTriesArgument = NamedArgument.of(
                "minimal incorrect max tries for default restart period", defaultRestartPeriodMaxCorrectMaxTries + 1
        );

        return Stream.of(
                Arguments.of(noRestartsMaxTriesArgument, maxCorrectTotalRestartPeriodArgument, ValidationResult.OK),
                Arguments.of(noRestartsMaxTriesArgument, minIncorrectTotalRestartPeriodArgument, ValidationResult.OK),
                Arguments.of(oneRestartMaxTriesArgument, maxCorrectTotalRestartPeriodArgument, ValidationResult.OK),
                Arguments.of(oneRestartMaxTriesArgument, minIncorrectTotalRestartPeriodArgument, ValidationResult.FAIL),
                Arguments.of(defaultMaxTriesArgument, defaultMaxTriesMaxCorrectRestartPeriodArgument, ValidationResult.OK),
                Arguments.of(defaultMaxTriesArgument, defaultMaxTriesMinIncorrectRestartPeriodArgument, ValidationResult.FAIL),
                Arguments.of(defaultRestartPeriodMaxCorrectMaxTriesArgument, defaultRestartPeriodArgument, ValidationResult.OK),
                Arguments.of(defaultRestartPeriodMinIncorrectMaxTriesArgument, defaultRestartPeriodArgument, ValidationResult.FAIL)
        );
    }

    @ParameterizedTest
    @MethodSource("logbrokerDestroyPolicyTotalRestartPeriodTestParameters")
    void logbrokerDestroyPolicyTotalRestartPeriodTest(
            NamedArgument<Integer> maxTriesArgument,
            NamedArgument<Long> restartPeriodArgument,
            ValidationResult expectedValidationResult
    ) {
        var maxTries = OptionalInt.of(maxTriesArgument.getArgument());
        var restartPeriod = OptionalLong.of(restartPeriodArgument.getArgument());

        if (ValidationResult.OK == expectedValidationResult) {
            correctLogbrokerDestroyPolicyTest(maxTries, restartPeriod);
        } else {
            long actualTotalRestartPeriod = (maxTries.getAsInt() - 1) * restartPeriod.getAsLong();

            String expectedErrorMessage = errorWithPrefix(
                    LOGBROKER_DESTROY_POLICY_ERROR_PREFIX,
                    String.format(
                            LOGBROKER_DESTROY_POLICY_TOTAL_RESTART_PERIOD_ERROR_FORMAT,
                            MAXIMAL_CORRECT_LOGBROKER_DESTROY_POLICY_RESTART_PERIOD_MS,
                            actualTotalRestartPeriod
                    )
            );

            incorrectLogbrokerDestroyPolicyTest(maxTries, restartPeriod, expectedErrorMessage);
        }
    }

    private static void correctLogbrokerDestroyPolicyTest(OptionalInt policyMaxTries,
                                                          OptionalLong policyRestartPeriodMs) {
        List<String> errors = logbrokerDestroyPolicyScenario(policyMaxTries, policyRestartPeriodMs);
        noErrors(errors);
    }

    private static void incorrectLogbrokerDestroyPolicyTest(OptionalInt policyMaxTries,
                                                            OptionalLong policyRestartPeriodMs,
                                                            String expectedErrorMessage) {
        List<String> errors = logbrokerDestroyPolicyScenario(policyMaxTries, policyRestartPeriodMs);
        hasError(errors, expectedErrorMessage);
    }

    private static List<String> logbrokerDestroyPolicyScenario(OptionalInt policyMaxTries,
                                                               OptionalLong policyRestartPeriodMs) {
        var logbrokerDestroyPolicy = new LogbrokerDestroyPolicy(policyMaxTries, policyRestartPeriodMs);
        var logbrokerConfig = DEPLOY_UNIT_SPEC.getLogbrokerConfig().toBuilder()
                .withDestroyPolicy(logbrokerDestroyPolicy)
                .build();
        return DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withLogbrokerConfig(logbrokerConfig),
                STAGE_ID
        );
    }

    private static Stream<NamedArgument<LogbrokerTopicRequest>> okWhenLogbrokerTopicRequestCorrectParameters() {
        return Stream.of(
                NamedArgument.of("communal topic request", LogbrokerCommunalTopicRequest.INSTANCE),
                NamedArgument.of("custom topic request", CORRECT_CUSTOM_TOPIC_REQUEST)
        );
    }

    @ParameterizedTest
    @MethodSource("okWhenLogbrokerTopicRequestCorrectParameters")
    void okWhenLogbrokerTopicRequestCorrect(NamedArgument<LogbrokerTopicRequest> topicRequest) {
        var errors = logbrokerTopicRequestScenario(topicRequest.getArgument());
        noErrors(errors);
    }

    @Test
    void okWhenLogbrokerCustomTopicRequestCorrectWithSecret() {
        var topicRequest = CORRECT_CUSTOM_TOPIC_REQUEST;
        var errors = logbrokerTopicRequestScenario(
                topicRequest,
                Map.of(
                        topicRequest.getSecretSelector().getAlias(),
                        Converter.toYpProto(TestData.SECRET)
                ),
                Map.of()
        );
        noErrors(errors);
    }

    private static Stream<NamedArgument<LogbrokerTopicRequest>> failWhenLogbrokerTopicRequestFieldMissingParameters() {
        return Stream.of(
                NamedArgument.of("tvm client id", CORRECT_CUSTOM_TOPIC_REQUEST.withTvmClientId(0)),
                NamedArgument.of("topic name", CORRECT_CUSTOM_TOPIC_REQUEST.withTopicName("")),
                NamedArgument.of("secret alias", CORRECT_CUSTOM_TOPIC_REQUEST.withSecretAlias("")),
                NamedArgument.of("secret id", CORRECT_CUSTOM_TOPIC_REQUEST.withSecretKey(""))
        );
    }

    @ParameterizedTest
    @MethodSource("failWhenLogbrokerTopicRequestFieldMissingParameters")
    void failWhenLogbrokerTopicRequestFieldMissing(NamedArgument<LogbrokerTopicRequest> incorrectTopicRequest) {
        String expectedErrorMessage = errorWithPrefix(
                LOGBROKER_CUSTOM_TOPIC_REQUEST_ERROR_PREFIX,
                missingOrEmptyErrorMessage(incorrectTopicRequest.getName())
        );

        var errors = logbrokerTopicRequestScenario(incorrectTopicRequest.getArgument());
        hasError(errors, expectedErrorMessage);
    }

    @Test
    void failWhenLogbrokerTopicRequestSecretMissing() {
        String expectedErrorMessage = errorWithPrefix(
                LOGBROKER_CUSTOM_TOPIC_REQUEST_ERROR_PREFIX,
                String.format(NO_SECRET_FOR_ALIAS_ERROR_FORMAT, CORRECT_CUSTOM_TOPIC_ALIAS)
        );

        var errors = logbrokerTopicRequestScenario(CORRECT_CUSTOM_TOPIC_REQUEST, Map.of(), Map.of());
        hasError(errors, expectedErrorMessage);
    }

    private static List<String> logbrokerTopicRequestScenario(LogbrokerTopicRequest topicRequest) {
        return logbrokerTopicRequestScenario(
                topicRequest,
                Map.of(),
                Map.of(
                        CORRECT_CUSTOM_TOPIC_REQUEST.getSecretSelector().getAlias(),
                        CORRECT_CUSTOM_TOPIC_SECRET_REF
                )
        );
    }

    private static List<String> logbrokerTopicRequestScenario(LogbrokerTopicRequest topicRequest,
                                                              Map<String, DataModel.TPodSpec.TSecret> secrets,
                                                              Map<String, DataModel.TSecretRef> secretRefs) {
        var podSpecBuilder = DEPLOY_UNIT_SPEC.getDetails()
                .getPodSpec().toBuilder()
                .clearSecrets()
                .putAllSecrets(secrets)
                .clearSecretRefs()
                .putAllSecretRefs(secretRefs);

        var replicaSetUnitSpec = REPLICA_SET_UNIT_SPEC.withPodSpec(
                podSpecBuilder.build()
        );

        var logbrokerConfig = DEPLOY_UNIT_SPEC.getLogbrokerConfig().toBuilder()
                .withTopicRequest(topicRequest)
                .build();

        return DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withLogbrokerConfig(logbrokerConfig)
                        .withDetails(replicaSetUnitSpec)
                        .build(),
                STAGE_ID);
    }

    private static Stream<Optional<AllComputeResources>> okWhenLogbrokerPodResourcesRequestIsCorrectParameters() {
        var streamBuilder = Stream.<Optional<AllComputeResources>>builder();
        streamBuilder.add(Optional.empty());

        long defaultLogbrokerBoxVcpu = 400, reducedLogbrokerBoxVcpu = 200;
        var acceptedVcpu = List.of(
                0L,
                reducedLogbrokerBoxVcpu,
                defaultLogbrokerBoxVcpu,
                MAX_LOGBROKER_BOX_VCPU
        );

        acceptedVcpu.stream()
                .map(vcpu -> EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST.toBuilder()
                        .withVcpuGuarantee(vcpu)
                        .withVcpuLimit(vcpu)
                        .build()
                ).map(Optional::of)
                .forEach(streamBuilder::add);

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("okWhenLogbrokerPodResourcesRequestIsCorrectParameters")
    void okWhenLogbrokerPodAdditionalResourcesRequestIsCorrect(
            Optional<AllComputeResources> podAdditionalResourcesRequest) {
        var errors = logbrokerPodAdditionalResourcesRequestScenario(podAdditionalResourcesRequest);
        noErrors(errors);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, MAX_LOGBROKER_BOX_VCPU + 1})
    void failWhenLogbrokerPodAdditionalResourcesRequestVcpuGuaranteeIsIncorrect(long incorrectVcpuGuarantee) {
        failWhenLogbrokerPodAdditionalResourcesRequestVcpuIsIncorrectScenario(
                AllComputeResources.Builder::withVcpuGuarantee,
                VCPU_GUARANTEE, incorrectVcpuGuarantee
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, MAX_LOGBROKER_BOX_VCPU + 1})
    void failWhenLogbrokerPodAdditionalResourcesRequestVcpuLimitIsIncorrect(long incorrectVcpuLimit) {
        failWhenLogbrokerPodAdditionalResourcesRequestVcpuIsIncorrectScenario(
                AllComputeResources.Builder::withVcpuLimit,
                VCPU_LIMIT, incorrectVcpuLimit
        );
    }

    private static void failWhenLogbrokerPodAdditionalResourcesRequestVcpuIsIncorrectScenario(
            BiFunction<AllComputeResources.Builder, Long, AllComputeResources.Builder> builderSetup,
            String vcpuValueName,
            long incorrectVcpuValue) {
        var incorrectPodAdditionalResourcesRequest = builderSetup.apply(
                EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST.toBuilder(),
                incorrectVcpuValue
        ).build();

        String expectedErrorMessageBody = valueNotInSegmentErrorMessage(
                vcpuValueName,
                incorrectVcpuValue,
                0, MAX_LOGBROKER_BOX_VCPU
        );

        failWhenLogbrokerPodAdditionalResourcesRequestIsIncorrectScenario(
                incorrectPodAdditionalResourcesRequest,
                expectedErrorMessageBody
        );
    }

    private static Stream<AllComputeResources.Builder> failWhenLogbrokerPodAdditionalResourcesRequestVcpuNotEqualParameters() {
        int correctVcpu = 100, otherCorrectVcpu = 200;

        return Stream.of(
                EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST.toBuilder()
                        .withVcpuGuarantee(correctVcpu),
                EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST.toBuilder()
                        .withVcpuLimit(correctVcpu),
                EMPTY_LOGBROKER_POD_ADDITIONAL_RESOURCES_REQUEST.toBuilder()
                        .withVcpuGuarantee(correctVcpu)
                        .withVcpuLimit(otherCorrectVcpu)
        );
    }

    @ParameterizedTest
    @MethodSource("failWhenLogbrokerPodAdditionalResourcesRequestVcpuNotEqualParameters")
    void failWhenLogbrokerPodAdditionalResourcesRequestVcpuNotEqual(
            AllComputeResources.Builder incorrectPodAdditionalResourcesRequestBuilder) {
        var incorrectPodAdditionalResourcesRequest = incorrectPodAdditionalResourcesRequestBuilder
                .build();

        String expectedErrorMessageBody = valuesNotEqualErrorMessage(
                VCPU_GUARANTEE, incorrectPodAdditionalResourcesRequest.getVcpuGuarantee(),
                VCPU_LIMIT, incorrectPodAdditionalResourcesRequest.getVcpuLimit()
        );

        failWhenLogbrokerPodAdditionalResourcesRequestIsIncorrectScenario(
                incorrectPodAdditionalResourcesRequest,
                expectedErrorMessageBody
        );
    }

    private static void failWhenLogbrokerPodAdditionalResourcesRequestIsIncorrectScenario(
            AllComputeResources incorrectPodAdditionalResourcesRequest,
            String expectedErrorMessageBody) {
        String expectedErrorMessage = errorWithPrefix(
                LOGBROKER_BOX_RESOURCES_REQUEST_ERROR_PREFIX,
                expectedErrorMessageBody
        );

        var errors = logbrokerPodAdditionalResourcesRequestScenario(Optional.of(incorrectPodAdditionalResourcesRequest));
        hasError(errors, expectedErrorMessage);
    }

    private static List<String> logbrokerPodAdditionalResourcesRequestScenario(
            Optional<AllComputeResources> podAdditionalResourcesRequest) {
        var logbrokerConfig = DEPLOY_UNIT_SPEC.getLogbrokerConfig().toBuilder()
                .withPodAdditionalResourcesRequest(podAdditionalResourcesRequest)
                .build();

        return DEFAULT_VALIDATOR.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.withLogbrokerConfig(logbrokerConfig),
                STAGE_ID
        );
    }

    private static List<String> coredumpWithOutputVolumeScenario(String workloadId, String boxId,
                                                                 String outputVolumeId,
                                                                 Optional<String> outputVolumeMountPath) {
        TBox.Builder box = createBox(boxId);
        outputVolumeMountPath.ifPresent(mountPath -> TestData.addVolumeToBox(box, outputVolumeId, mountPath));

        TWorkload workload = createWorkload(workloadId, boxId, false);

        CoredumpOutputPolicy coredumpOutPolicy = CoredumpOutputPolicy.outputVolumeId(outputVolumeId);
        Map<String, CoredumpConfig> coredumpConfigs = ImmutableMap.of(
                workloadId,
                COREDUMP_CONFIG_WITHOUT_AGGREGATION.withOutputPolicy(coredumpOutPolicy)
        );

        DataModel.TPodSpec podSpec = DataModel.TPodSpec.newBuilder()
                .addDiskVolumeRequests(USED_BY_INFRA_VOLUME_REQUEST)
                .setResourceRequests(VALID_POD_RESOURCES)
                .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setSpec(TPodAgentSpec.newBuilder().addBoxes(box).addWorkloads(workload))
                        .build())
                .build();

        TMultiClusterReplicaSetSpec mcrsSpec = TMultiClusterReplicaSetSpec.newBuilder()
                .setPodTemplateSpec(TPodTemplateSpec.newBuilder()
                        .setSpec(podSpec)
                        .build())
                .setDeploymentStrategy(TMultiClusterReplicaSetSpec
                        .TDeploymentStrategy
                        .newBuilder()
                        .build())
                .build();

        return VALIDATOR_WITH_SIDECAR_DISKS.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withoutTvmConfig()
                        .withCoredumpConfig(coredumpConfigs)
                        .withDetails(new McrsUnitSpec(mcrsSpec, POD_AGENT_CONFIG_EXTRACTOR))
                        .build(),
                STAGE_ID
        );
    }

    private static List<String> logbrokerSidecarStorageClassScenario(McrsUnitSpec mcrsUnitSpec,
                                                                     Optional<SidecarVolumeSettings> sidecarVolSettings) {
        DeployUnitSpec spec = DEPLOY_UNIT_SPEC.toBuilder()
                .withDetails(mcrsUnitSpec)
                .withoutTvmConfig()
                .withLogbrokerConfig(LOGBROKER_CONFIG.toBuilder()
                        .withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY)
                        .withSidecarVolumeSettings(sidecarVolSettings)
                        .withLogsVirtualDiskIdRef(Optional.of(USED_BY_INFRA_VOLUME_REQUEST.getId()))
                        .build()
                ).build();

        return VALIDATOR_WITH_SIDECAR_DISKS.validateDeployUnitSpec(spec, STAGE_ID);
    }

    private static List<String> podAgentSidecarStorageClassScenario(McrsUnitSpec mcrsUnitSpec,
                                                                    Optional<SidecarVolumeSettings> sidecarVolumeSettings) {
        PodAgentConfig podAgentConfig = POD_AGENT_CONFIG.withSidecarVolumeSettings(sidecarVolumeSettings);

        DeployUnitSpec spec =
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withDetails(
                                mcrsUnitSpec.withPodAgentConfigExtractor(
                                        podAgentDeploymentMeta -> podAgentConfig
                                )
                        )
                        .withoutTvmConfig()
                        .withLogbrokerConfig(LOGBROKER_CONFIG.toBuilder()
                                .withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.DEFAULT)
                                .build()
                        ).build();

        return VALIDATOR_WITH_SIDECAR_DISKS.validateDeployUnitSpec(spec, STAGE_ID);
    }

    private static List<String> portoLogsVirtualDiskIdRefTestScenario(McrsUnitSpec mcrsSpec,
                                                                      Optional<String> portoLogsDiskId) {
        return VALIDATOR_WITH_SIDECAR_DISKS.validateDeployUnitSpec(
                DEPLOY_UNIT_SPEC.toBuilder()
                        .withoutTvmConfig()
                        .withLogbrokerConfig(LOGBROKER_CONFIG.toBuilder()
                                .withLogsVirtualDiskIdRef(portoLogsDiskId)
                                .withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY)
                                .withSidecarVolumeSettings(Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD)))
                                .build()
                        ).withDetails(mcrsSpec)
                        .build(),
                STAGE_ID
        );
    }

    private static void testTvmClash(TPodAgentSpec agentSpec, List<String> expectedErrors) {
        List<String> errors = StageValidatorImpl.validatePodAgentSpec(agentSpec, true);
        assertThat(errors, containsInAnyOrder(expectedErrors.toArray()));
    }

    private static void testLogbrokerClash(String stageId, TPodAgentSpec agentSpec, List<String> expectedErrors) {
        List<String> errors = StageValidatorImpl.validateClashWithLogbrokerIds(agentSpec, stageId);
        assertThat(errors, containsInAnyOrder(expectedErrors.toArray()));
    }

    private static void testThreadLimits(TPodAgentSpec agentSpec, long maxAvailableThreadsPerUserBoxes,
                                         List<String> expectedErrors) {
        List<String> errors = StageValidatorImpl.validateThreadLimits(agentSpec, maxAvailableThreadsPerUserBoxes);
        assertThat(errors, containsInAnyOrder(expectedErrors.toArray()));
    }
}
