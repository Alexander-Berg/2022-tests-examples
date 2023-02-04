package ru.yandex.infra.stage.protobuf;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.docker.DockerResolverTest;
import ru.yandex.infra.stage.docker.DockerState;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.BoxJugglerConfig;
import ru.yandex.infra.stage.dto.CoredumpOutputPolicy;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DeployUnitStatus;
import ru.yandex.infra.stage.dto.DockerImageContents;
import ru.yandex.infra.stage.dto.LogbrokerCommunalTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.LogbrokerCustomTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerDestroyPolicy;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.dto.McrsUnitStatus;
import ru.yandex.infra.stage.dto.PodAgentConfig;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.dto.TvmConfig;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TDeployUnitApproval;
import ru.yandex.yp.client.api.TDeployUnitOverrides;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TDeployUnitStatus;
import ru.yandex.yp.client.api.TLogbrokerConfig;
import ru.yandex.yp.client.api.TLogrotateConfig;
import ru.yandex.yp.client.api.TRuntimeDeployControls;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static ru.yandex.infra.stage.TestData.CONVERTER;
import static ru.yandex.infra.stage.TestData.CONVERTER_WITHOUT_DEPLOY_TIMELINE_STATUSES;
import static ru.yandex.infra.stage.TestData.CONVERTER_WITHOUT_REPLICA_SET_STATUSES;
import static ru.yandex.infra.stage.TestData.DEFAULT_IMAGE_HASH;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_TIMELINE;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_DESCRIPTION;
import static ru.yandex.infra.stage.TestData.REPLICA_SET_UNIT_STATUS_DETAILS_WITHOUT_RAW_STATUS;
import static ru.yandex.infra.stage.TestData.SECRET;
import static ru.yandex.infra.stage.TestData.SECRET_REF;
import static ru.yandex.infra.stage.TestData.SECRET_SELECTOR;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

class ConverterTest {
    private static final DockerImageContents ALL_FILLED_DOCKER_CONTENTS = new DockerImageContents(DOCKER_IMAGE_DESCRIPTION,
            ImmutableList.of(TestData.DOWNLOADABLE_RESOURCE), ImmutableList.of("some", "command"),
            ImmutableList.of("entry", "point"), Optional.of("user"), Optional.of("group"),
            Optional.of("workdir"), ImmutableMap.of("key", "value"), Optional.of(DEFAULT_IMAGE_HASH)
    );

    private static final DeployUnitStatus DEPLOY_UNIT_STATUS_WITH_LATEST_DEPLOYED_REVISION =
            TestData.DEPLOY_UNIT_STATUS.withDeployUnitTimeline(
                    DEPLOY_UNIT_TIMELINE.toBuilder().withLatestDeployedRevision(1).build()
            );

    @Test
    void antiaffinityConstraint() {
        testTemplate(TestData.CONSTRAINT);
    }

    private static Iterable<Arguments> provideParametersForDeployUnitSpec() {
        var argumentsCollectionBuilder = ImmutableList.<Arguments>builder();

        for (boolean soxService : ImmutableList.of(false, true)) {
            for (boolean enablePortoWorkloadMetrics : ImmutableList.of(false, true)) {
                for (SandboxResourceInfo resourceInfo : ImmutableList.of(
                        // EMPTY_RESOURCE SandboxResourceInfo{revision=0, override={}} is not valid anymore
                        new SandboxResourceInfo(1, emptyMap()),
                        new SandboxResourceInfo(2, ImmutableMap.of("key", "value"))
                )) {
                    argumentsCollectionBuilder.add(Arguments.of(soxService, enablePortoWorkloadMetrics, resourceInfo));

                }
            }
        }

        return argumentsCollectionBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForDeployUnitSpec")
    void deployUnitSpec(boolean soxService, boolean enablePortoWorkloadMetrics, SandboxResourceInfo sandboxRelease) {
        testTemplate(new DeployUnitSpec(1, 0, TestData.NETWORK_DEFAULTS, Optional.of(TestData.TVM_CONFIG),
                TestData.DEPLOY_UNIT_SPEC.getDetails(), ImmutableMap.of("box", TestData.DOCKER_IMAGE_DESCRIPTION),
                ImmutableMap.of("", DataModel.TEndpointSetSpec.newBuilder().setPort(1).setProtocol("1").setLivenessLimitRatio(0.5).build()),
                ImmutableMap.of("box",
                        new BoxJugglerConfig(Collections.emptyList(), OptionalInt.of(1),
                                Optional.of(TestData.DOWNLOADABLE_RESOURCE))),
                emptyMap(),
                TestData.LOGBROKER_CONFIG, Optional.of(TestData.SECURITY_SETTINGS), soxService, enablePortoWorkloadMetrics, ImmutableMap.of("workload", TestData.COREDUMP_CONFIG_WITH_AGGREGATION),
                Optional.of(sandboxRelease), Optional.of(sandboxRelease),
                Optional.of(sandboxRelease), Optional.of(sandboxRelease),
                Optional.of(sandboxRelease), Optional.of(sandboxRelease),
                Optional.of(sandboxRelease), Optional.empty()));
    }

    @Test
    void deployUnitStatus() {
        testTemplate(TestData.DEPLOY_UNIT_STATUS);
    }

    @Test
    void deployUnitStatusEnabledLatestDeployedRevision() {
        testTemplate(DEPLOY_UNIT_STATUS_WITH_LATEST_DEPLOYED_REVISION, CONVERTER);
    }

    @Test
    void deployUnitExtendedDeployUnitTimelineStatus() {
        testTemplate(TestData.DEPLOY_UNIT_STATUS, CONVERTER_WITHOUT_DEPLOY_TIMELINE_STATUSES);
    }

    @Test
    void deployUnitExtendedReplicaSetStatus() {
        DeployUnitStatus OLD_WAY_STATUS = TestData.DEPLOY_UNIT_STATUS.withDetails(
                REPLICA_SET_UNIT_STATUS_DETAILS_WITHOUT_RAW_STATUS
        );
        testTemplate(TestData.DEPLOY_UNIT_STATUS, CONVERTER);

        testTemplate(OLD_WAY_STATUS, CONVERTER_WITHOUT_REPLICA_SET_STATUSES);

        TDeployUnitStatus proto = CONVERTER.toProto(TestData.DEPLOY_UNIT_STATUS);
        DeployUnitStatus restored = CONVERTER_WITHOUT_REPLICA_SET_STATUSES.fromProto(proto);
        assertThatEquals(restored, OLD_WAY_STATUS);
    }

    @org.junit.Test
    public void logRotateConfigConverter() {
        Config converterConfig = mock(Config.class);
        OneofDerivedConverter oneofDerivedConverter = mock(OneofDerivedConverter.class);
        Converter converter = new Converter(
                converterConfig,
                oneofDerivedConverter,
                oneofDerivedConverter,
                oneofDerivedConverter
        );
        String config = "Config";
        String box_id = "Box_id";
        TDeployUnitSpec unitSpec = TDeployUnitSpec.newBuilder().putLogrotateConfigs(box_id,
                TLogrotateConfig.newBuilder().setRawConfig(config).build()).build();
        DeployUnitSpec deployUnitSpec = converter.fromProto(unitSpec);
        Assertions.assertNotNull(deployUnitSpec.getLogrotateConfig().get(box_id));
        Assertions.assertEquals(config, deployUnitSpec.getLogrotateConfig().get(box_id).getRawConfig());
    }

    @Test
    void condition() {
        testTemplate(TestData.CONDITION1);
    }

    @Test
    void deployProgress() {
        testTemplate(TestData.DEPLOY_PROGRESS);
    }

    @Test
    void tvmApp() {
        testTemplate(TestData.TVM_SRC);
    }

    @Test
    void tvmClient() {
        testTemplate(TestData.TVM_CLIENT);
    }

    @Test
    void tvmConfig() {
        testTemplate(TestData.TVM_CONFIG);
        testTemplate(new TvmConfig(TestData.TVM_CONFIG.getMode(), TestData.TVM_CONFIG.getBlackboxEnvironment(),
                TestData.TVM_CONFIG.getClients(), OptionalInt.of(1), OptionalInt.empty(), OptionalInt.empty(),
                TestData.TVM_CONFIG.getTvmtoolLayer(), OptionalInt.of(2), OptionalInt.of(3),
                OptionalInt.of(4), Optional.of(TestData.SIDECAR_VOLUME_SETTINGS), false));
    }

    @Test
    void sidecarVolume() {
        testTemplate(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        testTemplate(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        testTemplate(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));
    }

    @Test
    void approveStatus() {
        TRuntimeDeployControls empty = TRuntimeDeployControls.newBuilder()
                .build();
        TRuntimeDeployControls emptyPayload = TRuntimeDeployControls.newBuilder()
                .putDeployUnitApprovals("Cluster", TDeployUnitApproval.newBuilder()
                        .setUser(TDeployUnitApproval.TUserApproval.newBuilder()
                                .build())
                        .build())
                .build();
        TRuntimeDeployControls emptyUser = TRuntimeDeployControls.newBuilder()
                .putDeployUnitApprovals("Cluster", TDeployUnitApproval.newBuilder()
                        .setPayload(TDeployUnitApproval.TApprovalPayload.newBuilder()
                                .build())
                        .build())
                .build();
        TRuntimeDeployControls emptyStatus = TRuntimeDeployControls.newBuilder()
                .putDeployUnitApprovals("Cluster", TDeployUnitApproval.newBuilder()
                        .setUser(TDeployUnitApproval.TUserApproval.newBuilder()
                                .setStatus(TDeployUnitApproval.TUserApproval.EUserApprovalStatus.DISAPPROVED)
                                .build())
                        .setPayload(TDeployUnitApproval.TApprovalPayload.newBuilder()
                                .build())
                        .build())
                .build();
        assertThat(TestData.CONVERTER.fromProto(ImmutableMap.of("test", empty)).getApprovedClusterList(),
                Matchers.anEmptyMap());
        assertThat(TestData.CONVERTER.fromProto(ImmutableMap.of("test", emptyPayload)).getApprovedClusterList(),
                Matchers.anEmptyMap());
        assertThat(TestData.CONVERTER.fromProto(ImmutableMap.of("test", emptyUser)).getApprovedClusterList(),
                Matchers.anEmptyMap());
        assertThat(TestData.CONVERTER.fromProto(ImmutableMap.of("test", emptyStatus)).getApprovedClusterList(),
                Matchers.anEmptyMap());

        TRuntimeDeployControls.Builder withDeploySettings =
                TRuntimeDeployControls.newBuilder().setDeployUnitOverrides(TDeployUnitOverrides.newBuilder()
                        .setDeploySettingsOverride(
                                TDeployUnitOverrides.TDeploySettingsOverride.newBuilder().setDeploySettings(
                                        TDeployUnitSpec.TDeploySettings.newBuilder()
                                                .setDeployStrategy(TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL)
                                                .addClusterSequence(TDeployUnitSpec.TClusterSettings.newBuilder()
                                                        .setNeedApproval(true)
                                                        .setYpCluster("Yp-cluster")
                                                        .build())
                                )
                        )
                        .build());

        Map<String, DeployUnitSpec.DeploySettings> deploySettings = TestData.CONVERTER.fromProto(ImmutableMap.of("test",
                withDeploySettings.build())).getDeploySettings();

        assertThat(deploySettings.isEmpty(), equalTo(false));
        assertThat(deploySettings.get("test").getClustersSettings().get(0).getName(), equalTo("Yp-cluster"));
        assertThat(deploySettings.get("test").getClustersSettings().get(0).isNeedApprove(), equalTo(true));
        assertThat(deploySettings.get("test").getDeployStrategy(),
                equalTo(TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL));

        TRuntimeDeployControls nonEmpty = TRuntimeDeployControls.newBuilder()
                .putDeployUnitApprovals("Cluster", TDeployUnitApproval.newBuilder()
                        .setUser(TDeployUnitApproval.TUserApproval.newBuilder()
                                .setStatus(TDeployUnitApproval.TUserApproval.EUserApprovalStatus.APPROVED)
                                .build())
                        .setPayload(TDeployUnitApproval.TApprovalPayload.newBuilder()
                                .setCluster("Cluster")
                                .setRevision(1)
                                .build())
                        .build())
                .build();
        Map<String, Set<String>> test =
                TestData.CONVERTER.fromProto(ImmutableMap.of("test", nonEmpty)).getApprovedClusterList();
        assertThat(test.get("test"), Matchers.contains("Cluster"));
    }

    @Test
    void logbrokerConfig() {
        testTemplate(TestData.LOGBROKER_CONFIG);

        String logsVirtualDiskId = "logs_virtual_disk_id";
        SidecarVolumeSettings sidecarVolumeSettings = new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO);

        var podAdditionalResourcesRequest = Optional.of(new AllComputeResources(
                789, 987, AllComputeResources.UNKNOWN_DISK_CAPACITY, 34
        ));

        var logbrokerConfig = TestData.LOGBROKER_CONFIG.toBuilder()
                .withLogbrokerAgentLayer(Optional.of(TestData.DOWNLOADABLE_RESOURCE))
                .withLogsVirtualDiskIdRef(Optional.of(logsVirtualDiskId))
                .withSidecarVolumeSettings(Optional.of(sidecarVolumeSettings))
                .withDestroyPolicy(new LogbrokerDestroyPolicy(OptionalInt.of(10), OptionalLong.of(42)))
                .withTopicRequest(new LogbrokerCustomTopicRequest(TestData.LOGBROKER_TOPIC_DESCRIPTION, SECRET_SELECTOR))
                .withPodAdditionalResourcesRequest(podAdditionalResourcesRequest);

        testTemplate(logbrokerConfig.withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.UNKNOWN).build());
        testTemplate(logbrokerConfig.withSidecarBringupMode(LogbrokerConfig.SidecarBringupMode.MANDATORY).build());
    }

    @Test
    void logbrokerConfigWithMissingTopicRequestFromYp() {
        TLogbrokerConfig proto = TLogbrokerConfig.newBuilder().build();
        var logbrokerConfig = CONVERTER.fromProto(proto);

        var actualTopicRequest = logbrokerConfig.getTopicRequest();
        assertThatEquals(actualTopicRequest, LogbrokerCommunalTopicRequest.INSTANCE);
    }

    @Test
    void logbrokerDestroyPolicy() {
        var defaultDestroyPolicy = new LogbrokerDestroyPolicy();

        testTemplate(defaultDestroyPolicy);
        testTemplate(defaultDestroyPolicy.withMaxTries(OptionalInt.of(10)));
        testTemplate(defaultDestroyPolicy.withRestartPeriodMs(OptionalLong.of(42)));
    }

    @Test
    void securitySettings() {
        testTemplate(TestData.SECURITY_SETTINGS);

        testTemplate(TestData.SECURITY_SETTINGS.toBuilder()
                .withDisableDefaultlyEnabledChildOnlyIsolation(true)
                .build()
        );

        testTemplate(TestData.SECURITY_SETTINGS.toBuilder()
                .withDisableDefaultlyEnabledSecretEnv(true)
                .build()
        );

        testTemplate(TestData.SECURITY_SETTINGS.toBuilder()
                .withDisableDefaultlyEnabledChildOnlyIsolation(true)
                .withDisableDefaultlyEnabledSecretEnv(true)
                .build()
        );
    }

    private static Stream<LogbrokerTopicRequest> provideParametersForLogbrokerTopicRequest() {
        return Stream.of(
                LogbrokerCommunalTopicRequest.INSTANCE,
                new LogbrokerCustomTopicRequest(TestData.LOGBROKER_TOPIC_DESCRIPTION, SECRET_SELECTOR)
        );
    }

    @ParameterizedTest
    @MethodSource("provideParametersForLogbrokerTopicRequest")
    void logbrokerTopicRequest(LogbrokerTopicRequest logbrokerTopicRequest) {
        testTemplate(logbrokerTopicRequest);
    }

    @Test
    void secret() {
        testTemplate(SECRET);
    }

    @Test
    void secretYp() {
        testTemplate(SECRET, CONVERTER, "toYpProto", "fromYpProto");
    }

    @Test
    void secretRef() {
        testTemplate(SECRET_REF);
    }

    @Test
    void secretToRef() {
        var expectedSecretRefProto = Converter.toProto(SECRET_REF);

        var actualSecretProto = Converter.toYpProto(SECRET);
        var actualSecretRefProto = Converter.secretToRef(actualSecretProto);

        assertThatEquals(actualSecretRefProto, expectedSecretRefProto);
    }


    @Test
    void secretSelector() { testTemplate(SECRET_SELECTOR);}

    @Test
    void stageSpec() {
        testTemplate(TestData.STAGE_SPEC);
    }

    @Test
    void stageSpecWithDynamicResource() {
        testTemplate(TestData.STAGE_SPEC_WITH_DR);
    }

    @Test
    void stageStatus() {
        testTemplate(TestData.STAGE_STATUS);
    }

    @Test
    void mcrsUnitSpec() {
        testTemplate(TestData.MCRS_UNIT_SPEC);
    }

    @Test
    void mcrsUnitStatus() {
        testTemplate(new McrsUnitStatus(TestData.REPLICA_SET_ID, ImmutableMap.of(
                TestData.CLUSTER, new McrsUnitStatus.PerClusterStatus(List.of(TestData.ENDPOINT_SET_ID))
        )));
    }

    @Test
    void rscUnitSpec() {
        testTemplate(TestData.REPLICA_SET_UNIT_SPEC);
    }

    @Test
    void rscUnitStatus() {
        testTemplate(TestData.REPLICA_SET_UNIT_STATUS_DETAILS);
    }

    @Test
    void checksum() {
        testTemplate(DockerResolverTest.CHECKSUM);
    }

    @Test
    void resourceWithMeta() {
        testTemplate(new ResourceWithMeta(TestData.DOWNLOADABLE_RESOURCE, Optional.of(TestData.RESOURCE_META)));
    }

    @Test
    void downloadableResource() {
        testTemplate(DockerResolverTest.DOWNLOADABLE_RESOURCE);
    }

    @Test
    void dockerImageDescription() {
        testTemplate(TestData.DOCKER_IMAGE_DESCRIPTION);
    }

    @Test
    void dockerImageContents() {
        testTemplate(ALL_FILLED_DOCKER_CONTENTS);
    }

    @Test
    void podAgentConfig() {
        DataModel.TPodSpec.TPodAgentDeploymentMeta.Builder builder = DataModel.TPodSpec.TPodAgentDeploymentMeta.newBuilder();
        PodAgentConfig dto = new PodAgentConfig(Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO)));
        DataModel.TPodSpec.TPodAgentDeploymentMeta protoOnce = TestData.CONVERTER.toProtoPartial(dto, builder).build();
        PodAgentConfig restoredDto = TestData.CONVERTER.fromProto(protoOnce);
        assertThat(dto, equalTo(restoredDto));
    }

    @Test
    void dockerState() {
        testTemplate(new DockerState(ImmutableMap.of(
                TestData.DOCKER_IMAGE_DESCRIPTION, ALL_FILLED_DOCKER_CONTENTS
        )));
    }

    @Test
    void coredumpConfig() {
        testTemplate(TestData.COREDUMP_CONFIG_WITH_AGGREGATION);
        testTemplate(
                TestData.COREDUMP_CONFIG_WITH_AGGREGATION.withOutputPolicy(
                        CoredumpOutputPolicy.outputPath(("out_path"))
                )
        );
        testTemplate(
                TestData.COREDUMP_CONFIG_WITH_AGGREGATION.withOutputPolicy(
                        CoredumpOutputPolicy.outputVolumeId(("out_volume_id"))
                )
        );
    }

    @Test
    void replicaSetDeploymentStrategy() {
        testTemplate(TestData.REPLICA_SET_DEPLOYMENT_STRATEGY);
    }

    @Test
    void dynamicResourceSpec() {
        testTemplate(TestData.DYNAMIC_RESOURCE_SPEC);
    }

    @Test
    void networkDefaults() {
        testTemplate(TestData.NETWORK_DEFAULTS);
        testTemplate(TestData.EMPTY_NETWORK_DEFAULTS);
        testTemplate(TestData.NETWORK_DEFAULTS_WITH_ADDRESS_OVERRIDE);
        testTemplate(TestData.NETWORK_DEFAULTS_WITH_SUBNET_OVERRIDE);
        testTemplate(TestData.NETWORK_DEFAULTS.withIp4AddressPoolId("poolId"));
        testTemplate(TestData.NETWORK_DEFAULTS.withVirtualServiceIds(ImmutableList.of("s1", "s2")));
    }

    @Test
    void schemaMeta() {
        testTemplate(TestData.SCHEMA_META);
    }

    @Test
    void relationMeta() {
        testTemplate(TestData.RELATION_META);
    }

    @Test
    void projectMeta() {
        testTemplate(TestData.PROJECT_META);
    }

    @Test
    void stageMeta() {
        testTemplate(TestData.STAGE_META);
    }

    @Test
    void dynamicResourceMeta() {
        testTemplate(TestData.DYNAMIC_RESOURCE_META);
    }

    @Test
    void horizontalPodAutoscalerMeta() {
        testTemplate(TestData.HORIZONTAL_POD_AUTOSCALER_META);
    }

    private <T> void testTemplate(T java) {
        testTemplate(java, CONVERTER);
    }

    // TODO: copypasted from GrpcProtoConverter in iss repo
    private <T> void testTemplate(T java, Converter converter) {
        testTemplate(java, converter, "toProto", "fromProto");
    }

    private <T> void testTemplate(T java, Converter converter,
                                  String toProtoMethodName, String fromProtoMethodName) {
        try {
            Method toProto = converter.getClass().getDeclaredMethod(toProtoMethodName, java.getClass());
            Method fromProto = converter.getClass().getDeclaredMethod(fromProtoMethodName, toProto.getReturnType());
            Object protoOnce = toProto.invoke(converter, java);
            T restored = (T) fromProto.invoke(converter, protoOnce);
            assertThat(restored, equalTo(java));
            Object protoTwice = toProto.invoke(converter, restored);
            // .equals() for dto may lack some checks, so use an auto-generated proto one to double-check
            assertThat(protoOnce, equalTo(protoTwice));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
